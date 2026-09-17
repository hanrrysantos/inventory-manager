****# Inventory Consistency & Concurrency Implementation Plan

> **For agentic workers:** execute uma tarefa por vez, usando a Skill `code-review` após cada tarefa. Não execute a próxima tarefa automaticamente.

**Goal:** tornar entradas e consumos de estoque atomicamente consistentes sob concorrência real no PostgreSQL, preservando FEFO, excluindo lotes expirados e impedindo overselling, lost updates e divergência entre saldo e movimentações.

**Architecture:** o fluxo permanece no módulo `inventory`, com `BatchService` como fronteira transacional. O consumo selecionará e bloqueará, na mesma transação, os lotes que participam da decisão FEFO; `addStock` usará locking específico para o lote. `InventoryLog` continuará na mesma transação da alteração do lote, e a chamada atual de `StockAlertService` será preservada.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, Flyway, JUnit 5, Spring Test, Mockito, Testcontainers PostgreSQL e `TransactionTemplate`.

**Spec:** requisitos aprovados em `docs/plans/02-inventory-consistency.md` antes da substituição deste documento; decisões arquiteturais em `docs/architecture.md`.

## Global Constraints

- Use `bash ./mvnw ...`; `mvnw` permanece com permissão 644.
- Execute somente a tarefa autorizada e aguarde autorização antes da próxima.
- Não edite `V1__Create_Tables.sql` nem `V2__Populate_Tables.sql`.
- Se uma constraint for necessária, crie nova migration versionada e justifique a invariante protegida.
- Não introduza RabbitMQ, eventos, notificações assíncronas, retry/DLQ, observabilidade, Redis ou microserviços.
- Não altere o cálculo global do `ProductMapper` nem corrija `INPUT` versus `ENTRY`, salvo necessidade técnica direta.
- Preserve `StockAlertService.checkInventoryAndNotify()`; seu desacoplamento pertence a outro plano.
- Não introduza optimistic locking ou locking fora do escopo de inventory.
- Testes concorrentes devem usar duas transações independentes, sincronização por latch/barreira ou equivalente e timeout explícito. `Thread.sleep` não pode ser o mecanismo principal.
- Cada tarefa termina com testes e `code-review` limitado ao diff da tarefa.
- Em `CHANGES_REQUESTED`, parar e reportar; não executar `fix-findings` automaticamente.
- Não fazer push, não criar commits automaticamente e não avançar entre tarefas.

## Estado atual preservado

- `BatchService.createBatch`, `addStock` e `consumeStock` possuem `@Transactional`.
- Criação e entrada geram `INPUT`; consumo gera `OUTPUT`.
- Insuficiência lança `InsufficientStockException` depois de percorrer os lotes, permitindo rollback da transação.
- A consulta atual seleciona quantidade positiva e ordena somente por `expiryDate ASC`, incluindo lotes expirados.
- Não existem `@Lock`, `PESSIMISTIC_WRITE` ou `@Version`.

---

### Tarefa 1: Characterization dos comportamentos atuais

**Objetivo:** registrar o comportamento de inventory antes de mudar validade, FEFO ou concorrência.

**Arquivos:**

- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/serviceTest/BatchServiceTest.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/integration/InventoryTransactionIntegrationTest.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/product/mapperTest/ProductMapperTest.java` somente se necessário para explicitar cobertura existente.
- Não modificar produção ou migrations.

**Testes:** consumo parcial; consumo em múltiplos lotes; insuficiência com rollback consultado no banco; criação e `INPUT`; `addStock` e `INPUT`; consulta de expirados; consumo atual de lote expirado; duas validades iguais; consumo zero e negativo, registrando o comportamento observado sem corrigi-lo.

**RED/GREEN:** testes que apenas documentam comportamento existente nascem GREEN. O teste de lote expirado deve afirmar que hoje ele é consumido, para ser convertido em mudança RED → GREEN na Tarefa 3.

**Implementação mínima:** somente testes e fixtures.

**Validação:**

```bash
bash ./mvnw -Dtest=BatchServiceTest,InventoryTransactionIntegrationTest,ProductMapperTest test
```

**Conclusão:** os comportamentos atuais e o rollback persistido estão documentados, sem alteração de produção.

**Review:** executar `code-review` somente sobre o diff da Tarefa 1.

---

### Tarefa 2: FEFO determinístico

**Objetivo:** preservar `expiryDate ASC` e adicionar desempate determinístico baseado no modelo existente.

**Arquivos:**

- Modificar: `src/main/java/br/com/hanrry/inventory/inventory/repository/BatchRepository.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/serviceTest/BatchServiceTest.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/integration/InventoryTransactionIntegrationTest.java`

**Interface esperada:**

```java
List<Batch> findByProductIdAndQuantityGreaterThanOrderByExpiryDateAscIdAsc(
        Long productId, Long quantity);
```

Nesta tarefa a seleção ainda caracteriza validade; o filtro de lotes expirados será aplicado na Tarefa 3. Se o nome derivado ficar ilegível, usar `@Query` com `quantity > 0`, `ORDER BY expiryDate ASC, id ASC`.

**Testes:** validade diferente consome primeiro o vencimento mais próximo; validade igual consome menor ID; consumo em múltiplos lotes mantém a ordem; integração confirma a ordem no PostgreSQL.

**RED/GREEN:** o teste deve proteger a ordenação explícita `expiryDate ASC, id ASC` e não pode depender de PostgreSQL retornar uma ordem diferente antes da mudança. A implementação mínima altera a consulta e os stubs; o teste pode nascer GREEN se a fixture atual já coincidir acidentalmente com a ordem esperada.

**Validação:**

```bash
bash ./mvnw -Dtest=BatchServiceTest,InventoryTransactionIntegrationTest test
```

**Conclusão:** FEFO continua por validade e torna-se determinístico por ID.

**Review:** executar `code-review` sobre repository, serviço afetado e testes.

---

### Tarefa 3: Exclusão de lotes expirados

**Objetivo:** implementar a decisão aprovada: lote expirado nunca participa de `consumeStock`.

**Arquivos:**

- Modificar: `src/main/java/br/com/hanrry/inventory/inventory/repository/BatchRepository.java`
- Modificar: `src/main/java/br/com/hanrry/inventory/inventory/service/BatchService.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/serviceTest/BatchServiceTest.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/integration/InventoryTransactionIntegrationTest.java`

**Comportamento:** selecionar `expiryDate >= LocalDate.now()`. Validade igual à data atual permanece disponível; lote anterior fica intacto. O endpoint de listagem de expirados não muda.

**RED/GREEN:** converter o characterization da Tarefa 1 para esperar lote expirado intacto e `InsufficientStockException` quando não houver estoque válido suficiente. Deve falhar antes da implementação.

**Implementação mínima:** adicionar o predicado de validade à consulta FEFO. Não alterar `ProductMapper`, cálculo global ou migrations.

**Validação:**

```bash
bash ./mvnw -Dtest=BatchServiceTest,InventoryTransactionIntegrationTest test
```

**Conclusão:** nenhum expirado é reduzido ou recebe `OUTPUT`.

**Review:** executar `code-review` somente sobre o diff da Tarefa 3.

---

### Tarefa 4: Locking pessimista da decisão FEFO

**Objetivo:** bloquear os registros que participam da decisão completa do consumo até commit ou rollback.

**Arquivos:**

- Modificar: `src/main/java/br/com/hanrry/inventory/inventory/repository/BatchRepository.java`
- Modificar: `src/main/java/br/com/hanrry/inventory/inventory/service/BatchService.java` somente se necessário.
- Criar: `src/test/java/br/com/hanrry/inventory/inventory/integration/InventoryConcurrencyIntegrationTest.java`

**Interface:** a consulta de lotes elegíveis deve usar `@Lock(LockModeType.PESSIMISTIC_WRITE)`, filtrar produto, quantidade positiva e validade não expirada, e ordenar por `expiryDate ASC, id ASC`. O locking de `addStock` será específico na Tarefa 6.

**Implementação mínima:** manter `consumeStock` como fronteira `@Transactional`; a consulta bloqueada deve ser a leitura usada para toda a decisão FEFO. Não adicionar apenas `@Lock` sem garantir que a consulta seja usada dentro da transação correta.

**Testes:** PostgreSQL real, dois contextos de persistência, duas transações independentes, latch/barreira, timeout explícito e ausência de sincronização principal por `Thread.sleep`. Validar a consulta com `PESSIMISTIC_WRITE`, a participação na fronteira `@Transactional` e que o lock permanece até commit ou rollback. O cenário de negócio completo com estoque 10 e dois consumos de 8 pertence exclusivamente à Tarefa 5.

**RED/GREEN:** adicionar o teste da consulta bloqueada e da fronteira transacional sem exigir que ele reproduza uma inconsistência acidental. A implementação deve fazer a consulta com `PESSIMISTIC_WRITE` ser usada dentro de `consumeStock`; a prova de overselling e insuficiência concorrente fica para a Tarefa 5.

**Validação:**

```bash
bash ./mvnw -Dtest=InventoryConcurrencyIntegrationTest test
```

**Conclusão:** todos os lotes usados pela decisão FEFO ficam protegidos até a finalização da transação.

**Review:** executar `code-review` sobre repository, serviço e teste de locking.

---

### Tarefa 5: Concorrência real de `consumeStock`

**Objetivo:** comprovar ausência de overselling e divergência entre saldo e logs.

**Arquivos:**

- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/integration/InventoryConcurrencyIntegrationTest.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/integration/InventoryTransactionIntegrationTest.java` somente para fixtures compartilhadas.
- Não modificar migrations.

**Harness:** usar `PostgreSQLContainer<>("postgres:16-alpine")` e Flyway; criar lote isolado; executar cada chamada em `TransactionTemplate` com transação independente; usar executor e latch/barreira para liberar as chamadas; usar `Future#get` com timeout; capturar sucesso e `InsufficientStockException`; encerrar o executor e falhar explicitamente em timeout.

**Cenário obrigatório:** estoque inicial 10 e dois `consumeStock(productId, 8)` concorrentes:

- exatamente um sucesso;
- exatamente uma `InsufficientStockException`;
- quantidade final = 2;
- total persistido de `OUTPUT` = 8;
- nenhum saldo negativo;
- nenhum overselling lógico.

Adicionar cenário de múltiplos lotes, verificando que lock e FEFO protegem a decisão completa.

**Validação:**

```bash
bash ./mvnw -Dtest=InventoryConcurrencyIntegrationTest test
```

Repetir a classe para verificar estabilidade, sem alterar o código entre execuções.

**Conclusão:** as asserções consultam o banco após commits/rollbacks e o cenário obrigatório passa dentro do timeout.

**Review:** executar `code-review` somente sobre o teste concorrente e suporte necessário.

---

### Tarefa 6: Locking e concorrência de `addStock`

**Objetivo:** impedir lost update quando duas entradas alteram o mesmo lote.

**Arquivos:**

- Modificar: `src/main/java/br/com/hanrry/inventory/inventory/repository/BatchRepository.java`
- Modificar: `src/main/java/br/com/hanrry/inventory/inventory/service/BatchService.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/serviceTest/BatchServiceTest.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/integration/InventoryConcurrencyIntegrationTest.java`

**Interface:** criar método específico para localizar lote por ID com `PESSIMISTIC_WRITE`, sem reutilizar a consulta FEFO. `addStock` deve usar esse método antes de calcular o novo saldo.

**Cenário:** lote inicial 10 e dois `addStock` concorrentes de 8. O resultado esperado é quantidade final 26, soma das entradas 16 e exatamente dois registros `InventoryLog` do tipo `INPUT`, cada um com quantidade 8 e associado ao lote correto. Usar transações independentes, sincronização determinística e timeout.

**RED/GREEN:** adicionar o teste contra a implementação atual, que pode expor lost update; implementar o método bloqueado e atualizar stubs unitários.

**Validação:**

```bash
bash ./mvnw -Dtest=BatchServiceTest,InventoryConcurrencyIntegrationTest test
```

**Conclusão:** nenhuma entrada concorrente é perdida e todos os logs são persistidos.

**Review:** executar `code-review` sobre `BatchService`, `BatchRepository` e testes.

---

### Tarefa 7: Atomicidade entre lote, `InventoryLog` e rollback

**Objetivo:** comprovar que saldo e movimentação formam uma única unidade transacional.

**Arquivos:**

- Modificar: `src/main/java/br/com/hanrry/inventory/inventory/service/InventoryLogService.java` somente se necessário.
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/integration/InventoryTransactionIntegrationTest.java`
- Modificar: `src/test/java/br/com/hanrry/inventory/inventory/serviceTest/BatchServiceTest.java`
- Não alterar `StockAlertService`.

**Testes:** criação e `INPUT` juntos; `addStock` e `INPUT` juntos; consumo de múltiplos lotes e `OUTPUT` juntos; insuficiência após lotes anteriores revertendo todos os saldos e logs; nenhuma asserção baseada apenas em memória.

**Implementação mínima:** preservar `@Transactional` em `BatchService`. `InventoryLogService.createLog` participa da transação externa por propagação padrão; não criar transação independente.

**Validação:**

```bash
bash ./mvnw -Dtest=InventoryTransactionIntegrationTest,BatchServiceTest test
```

**Conclusão:** saldo e movimentações são commitados ou revertidos juntos.

**Review:** executar `code-review` somente sobre o diff da Tarefa 7.

---

### Tarefa 8: Validação final do Plano 2

**Objetivo:** validar suíte, empacotamento, imagem e aderência arquitetural.

**Arquivos:** nenhum arquivo deve ser alterado nesta tarefa, salvo correção de teste estritamente causada pelas tarefas anteriores.

**Validações obrigatórias:**

```bash
bash ./mvnw clean test
bash ./mvnw package -DskipTests
docker build -t inventory-manager .
git diff --check
```

Confirmar que testes concorrentes usam PostgreSQL real e transações independentes; V1/V2 não foram editadas; não há RabbitMQ, eventos, notificações assíncronas, retry/DLQ, Redis ou observabilidade; `ProductMapper` não mudou; `StockAlertService` continua sendo chamado; não há `Thread.sleep` como sincronização principal; e JaCoCo foi gerado.

**Conclusão:** todas as validações passam, o cenário obrigatório continua passando e a revisão contra `docs/architecture.md` confirma FEFO, locking pessimista transacional e ausência de escopo posterior.

**Review:** executar `code-review` sobre o diff final, reportando findings, severidades, evidências, testes, status e bloqueadores. Se `CHANGES_REQUESTED`, parar sem `fix-findings`.

---

## Auto-revisão contra `docs/architecture.md`

- `Batch` permanece no domínio `inventory`.
- FEFO prioriza menor validade e possui desempate por ID.
- Lotes expirados não são disponibilidade para consumo.
- Consumo, movimentação e commit permanecem na mesma unidade transacional.
- O fluxo crítico usa locking pessimista no PostgreSQL.
- Concorrência é validada por integração real.
- Quantidade negativa e double spending são estados inválidos.
- Não são introduzidos RabbitMQ, eventos, consumidores, retry, DLQ ou desacoplamento de notificações.
- O cálculo global do `ProductMapper` permanece fora do escopo.
- Migrations aplicadas não são editadas; novas migrations só serão criadas para constraints justificadas.

Este documento é o plano executável. Nenhuma tarefa deve ser implementada sem autorização explícita para a tarefa correspondente.****
