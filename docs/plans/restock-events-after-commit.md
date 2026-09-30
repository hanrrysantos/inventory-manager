# Eventos de reposição após commit — Implementation Plan

> **Para agentes de implementação:** execute este plano tarefa por tarefa,
> conforme `AGENTS.md` e a Skill `code-review` após cada tarefa. Não execute
> a próxima tarefa automaticamente. Em `CHANGES_REQUESTED`, parar e reportar;
> não executar `fix-findings` sem autorização.

**Goal:** desacoplar o alerta de reposição da transação de `consumeStock`,
publicando `RestockNeededEvent` somente após commit, sem mudar a regra atual
de disparo nem o conteúdo do relatório.

**Architecture:** `inventory` publica um evento de aplicação durante
`consumeStock`. O processamento ocorre em
`@TransactionalEventListener(phase = AFTER_COMMIT)` no módulo
`notification`. `BatchService` deixa de depender de `StockAlertService`.
O listener relê o estoque baixo e reutiliza `PdfService` e `EmailSender`.
RabbitMQ e outbox permanecem fora.

**Tech Stack:** Java 21, Spring Boot 3, Spring `ApplicationEventPublisher`,
`@TransactionalEventListener`, JUnit 5, Mockito, MockMvc, Testcontainers
PostgreSQL.

**Spec:** [`../specs/restock-events-after-commit.md`](../specs/restock-events-after-commit.md)

**Architecture ref:** [`../architecture/overview.md`](../architecture/overview.md)
(seções Reposição, Eventos e transações, estratégia “Eventos após commit”).

## Global Constraints

- Execute somente a tarefa autorizada e aguarde autorização antes da próxima.
- Não introduza RabbitMQ, Transactional Outbox, retry, DLQ, Actuator,
  Micrometer, Prometheus, Grafana, Redis ou novos endpoints.
- Não altere FEFO, locks pessimistas, migrations existentes, contratos HTTP
  de lote nem a fórmula `minStock - totalQuantity`.
- Não altere `@Scheduled`, `RESEND_TO`, `PdfService` nem o isolamento por
  `owner` do alerta.
- Não persista histórico de notificação nesta etapa.
- Use `bash ./mvnw` dentro de `backend/`.
- Characterization tests de comportamento já existente podem nascer GREEN.
  RED → GREEN → REFACTOR vale para comportamento novo desta spec.
- Cada tarefa termina com testes e `code-review` limitado ao diff da tarefa.
- Não fazer commit, push ou avançar de tarefa sem autorização.

## Estado atual

- `BatchService.consumeStock` é `@Transactional` e chama
  `stockAlertService.checkInventoryAndNotify()` no fim do método, ainda
  dentro da transação.
- A chamada ocorre em todo caminho que não lança `InsufficientStockException`,
  inclusive quantidade zero ou negativa.
- `createBatch` e `addStock` não chamam o alerta.
- `StockAlertService` relê todos os produtos com estoque baixo, gera PDF e
  envia e-mail. O job agendado usa o mesmo método.
- `inventory` depende de `notification` via injeção direta.
- Testes unitários de `BatchService` verificam
  `verify(stockAlertService).checkInventoryAndNotify()`.
- `InventoryTransactionIntegrationTest` zera `min_stock` no `@BeforeEach`
  para evitar e-mail durante os testes de transação.

## Decisões técnicas deste plano

| Decisão | Escolha |
| --- | --- |
| Publicação | `ApplicationEventPublisher.publishEvent` dentro de `consumeStock` |
| Processamento | `@TransactionalEventListener(phase = AFTER_COMMIT)` |
| Tipo do evento | record `RestockNeededEvent` em `inventory` |
| Payload | `eventId`, `productId` do produto consumido, `occurredAt`; o listener não usa esses campos para montar o relatório |
| Orquestração | listener chama `StockAlertService.checkInventoryAndNotify()` |
| Falha do alerta | listener captura exceções, registra log e não relança |
| Job | permanece chamada direta, sem evento |

Publicar durante a transação e escutar em `AFTER_COMMIT` impede que rollback
dispare o listener. Capturar a exceção no listener impede que a falha do
Resend vire `500` depois do estoque já confirmado.

## Sequência

```text
Tarefa 1  Characterization do acoplamento atual
Tarefa 2  Evento em inventory e publicação no consumo
Tarefa 3  Listener AFTER_COMMIT e isolamento de falha
Tarefa 4  Testes de integração commit / rollback / falha de e-mail
Tarefa 5  Validação final contra a spec
```

---

### Tarefa 1: Characterization do acoplamento atual

**Objetivo:** proteger o comportamento atual antes de mover o alerta para
depois do commit.

**Arquivos envolvidos:**

- `backend/src/test/java/br/com/hanrry/inventory/inventory/serviceTest/BatchServiceTest.java`
- `backend/src/test/java/br/com/hanrry/inventory/notification/serviceTest/StockAlertServiceTest.java`

**Escopo:** não alterar código de produção. Completar ou ajustar somente
testes que registrem:

- alerta chamado após consumo que não lança `InsufficientStockException`;
- alerta não chamado quando o consumo é insuficiente;
- alerta não chamado em `createBatch` e `addStock`;
- `StockAlertService` envia e-mail somente quando há produtos com estoque
  baixo.

Os testes unitários atuais já cobrem a maior parte. Esta tarefa só adiciona
o que faltar para a substituição posterior de `StockAlertService` por evento.

**Validação:**

```bash
cd backend && bash ./mvnw -Dtest=BatchServiceTest,StockAlertServiceTest test
```

**Critério de conclusão:** os testes acima passam; nenhum arquivo de
produção mudou.

---

### Tarefa 2: Evento em inventory e publicação no consumo

**Objetivo:** `inventory` deixa de conhecer `notification` e publica
`RestockNeededEvent` ao concluir um consumo que não é insuficiente.

**Arquivos previstos:**

- Criar: `backend/src/main/java/br/com/hanrry/inventory/inventory/event/RestockNeededEvent.java`
- Modificar: `backend/src/main/java/br/com/hanrry/inventory/inventory/service/BatchService.java`
- Modificar: `backend/src/test/java/br/com/hanrry/inventory/inventory/serviceTest/BatchServiceTest.java`

**Design:**

```text
record RestockNeededEvent(UUID eventId, Long productId, Instant occurredAt)
```

`BatchService` recebe `ApplicationEventPublisher` no lugar de
`StockAlertService`. Após um consumo que não lança
`InsufficientStockException`, publica o evento com o `productId` da
requisição.

**Testes:** os unitários de `BatchService` passam a verificar
`publishEvent(any(RestockNeededEvent.class))` nos mesmos caminhos que hoje
verificam `checkInventoryAndNotify()`, e `never()` no caminho de estoque
insuficiente, `createBatch` e `addStock`.

**Validação:**

```bash
cd backend && bash ./mvnw -Dtest=BatchServiceTest test
```

**Critério de conclusão:** `BatchService` não importa tipos de
`notification`; os testes unitários de consumo passam. O listener ainda não
existe; o alerta após consumo ficará ausente até a Tarefa 3. Não publicar
essa tarefa isoladamente em produção.

---

### Tarefa 3: Listener AFTER_COMMIT e isolamento de falha

**Objetivo:** `notification` processa o evento depois do commit e isola
falhas externas.

**Arquivos previstos:**

- Criar: `backend/src/main/java/br/com/hanrry/inventory/notification/event/RestockNeededEventListener.java`
- Criar: `backend/src/test/java/br/com/hanrry/inventory/notification/event/RestockNeededEventListenerTest.java`
- Verificar: `StockAlertService`, `PdfService`, `EmailSender` sem mudança de
  regra

**Design:**

```text
@TransactionalEventListener(phase = AFTER_COMMIT)
void on(RestockNeededEvent event)
    try
        stockAlertService.checkInventoryAndNotify()
    catch (Exception)
        log error
        não relançar
```

O listener não usa `event.productId()` para filtrar o relatório. O job
agendado não muda.

**Testes unitários do listener:**

- delega para `checkInventoryAndNotify()`;
- captura exceção do serviço e não relança.

**Validação:**

```bash
cd backend && bash ./mvnw -Dtest=RestockNeededEventListenerTest,StockAlertServiceTest,BatchServiceTest test
```

**Critério de conclusão:** o alerta volta a ocorrer após consumo confirmado;
falha simulada no listener não escapa.

---

### Tarefa 4: Testes de integração de commit, rollback e falha de e-mail

**Objetivo:** provar as invariantes da spec contra PostgreSQL real e o
contexto transacional do Spring.

**Arquivos previstos:**

- Criar ou estender teste de integração em
  `backend/src/test/java/br/com/hanrry/inventory/inventory/integration/`
  ou `notification/integration/`
- Reutilizar Testcontainers PostgreSQL já adotado
- Usar `@MockitoBean` / spy de `EmailSender` quando for necessário observar
  ou forçar falha sem chamar o Resend

**Cenários obrigatórios:**

1. Consumo insuficiente: nenhum envio de e-mail; lote e log revertidos.
2. Consumo confirmado com produto em estoque baixo: e-mail disparado depois
   do commit.
3. `EmailSender` lança após consumo válido: lote e log permanecem
   persistidos.
4. Quando houver teste HTTP de consumo, falha do `EmailSender` não muda o
   status de sucesso atual do endpoint.

Não usar `Thread.sleep` como sincronização principal. O listener
`AFTER_COMMIT` é síncrono na thread da requisição.

Preservar o `@BeforeEach` que zera `min_stock` nos testes de transação que
não exercitam alerta, para não gerar e-mail colateral.

**Validação:**

```bash
cd backend && bash ./mvnw -Dtest=InventoryTransactionIntegrationTest,InventoryConcurrencyIntegrationTest,*Restock* test
```

Ajuste os nomes ao arquivo criado. Em seguida execute os testes de
`inventory` e `notification` afetados.

**Critério de conclusão:** os quatro cenários da spec passam; os testes de
concorrência e transação existentes continuam verdes.

---

### Tarefa 5: Validação final contra a spec

**Objetivo:** confirmar o incremento completo e revisar o diff.

**Validação:**

```bash
cd backend && bash ./mvnw clean test
```

**Revisão:** executar a Skill `code-review` no diff desta feature, limitado
ao escopo da spec.

**Critérios da spec a marcar:**

- [x] `BatchService` não chama `StockAlertService`
- [x] evento processado somente após commit
- [x] rollback não notifica
- [x] falha do alerta não reverte estoque nem falha o HTTP do consumo
- [x] PDF, `RESEND_TO` e job inalterados
- [x] sem RabbitMQ, outbox, tabela de notificação ou nova regra de reposição

**Critério de conclusão:** suíte completa verde, code review sem findings
bloqueantes e critérios da spec validados.

---

## Riscos

- Publicar o evento fora da transação faria o listener rodar mesmo em
  rollback. A publicação deve ocorrer dentro de `consumeStock`.
- Relançar exceção no listener confirma o estoque e ainda devolve erro HTTP.
  A captura é obrigatória.
- Entregar a Tarefa 2 sem a Tarefa 3 remove o alerta após consumo. As duas
  precisam coexistir antes de qualquer release.
- Testes de integração que não controlam `min_stock` ou `EmailSender` podem
  tentar envio real.

## Fora deste plano

RabbitMQ, outbox, estados persistidos, ownership do alerta, mudança do job,
frontend e observabilidade.
