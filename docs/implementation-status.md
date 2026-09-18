# Status de implementação dos Planos 1 e 2

Data da verificação: 2026-09-17

Este documento compara os critérios dos planos com o estado atual do código.
Ele não substitui os planos originais; registra o que já foi implementado e o
que ainda precisa ser validado ou executado.

## Resumo

Os dois planos estão funcionalmente implementados em sua maior parte.
Não há, neste momento, uma tarefa de implementação de produção claramente
pendente dentro dos Planos 1 e 2. O trabalho restante é principalmente:

- executar e registrar a validação final dos dois planos;
- realizar o code review final do escopo de cada plano;
- confirmar o build da imagem Docker;
- validar o deploy e o banco remoto do Supabase;
- registrar as limitações preservadas e a alteração excepcional das migrations.

## Plano 1 — Fundação e modularização

### Itens implementados

- Os módulos de domínio `auth`, `user`, `product`, `inventory`,
  `notification` e `shared` existem na estrutura principal.
- `Batch` e `InventoryLog` estão no módulo `inventory`.
- Usuários e autenticação estão separados nos módulos `user` e `auth`.
- Catálogo e categorias estão no módulo `product`.
- PDF, e-mail e alerta estão no módulo `notification`.
- Exceções e configurações compartilhadas estão em `shared`.
- Existem testes de integração para PostgreSQL e Spring Security.
- Os contratos atuais dos controllers e o fluxo síncrono de notificações foram
  preservados.
- Não foram introduzidos RabbitMQ, retry, DLQ, Redis, Transactional Outbox ou
  microserviços.

### Pendências de fechamento

1. Executar a validação final definida na Tarefa 10:

   ```bash
   bash ./mvnw clean test
   bash ./mvnw package -DskipTests
   docker build -t inventory-manager .
   git diff --check
   git status --short
   ```

2. Executar o `code-review` final do Plano 1, verificando especialmente:
   - ausência de packages técnicos antigos;
   - ausência de classes duplicadas;
   - preservação dos endpoints e payloads;
   - ausência de funcionalidades reservadas para planos posteriores.

3. Registrar formalmente as limitações preservadas, incluindo o comportamento
   atual de cálculo de estoque e o fluxo síncrono de alerta.

## Plano 2 — Consistência e concorrência de estoque

### Itens implementados

- A consulta FEFO ordena por `expiryDate ASC, id ASC`.
- Lotes expirados não participam do consumo.
- A decisão de consumo usa `PESSIMISTIC_WRITE` dentro da transação.
- `addStock` utiliza locking específico do lote.
- Existem testes de concorrência com PostgreSQL real e transações
  independentes.
- O cenário de overselling é coberto: dois consumos concorrentes não podem
  consumir o mesmo saldo.
- Entradas concorrentes não perdem atualizações.
- Saldo e `InventoryLog` permanecem na mesma transação.
- O rollback por estoque insuficiente é validado contra o banco.
- `StockAlertService.checkInventoryAndNotify()` continua sendo chamado pelo
  fluxo de consumo.
- A implementação não usa `Thread.sleep` como mecanismo principal de
  sincronização.

### Ressalva registrada

O Plano 2 determinava que `V1__Create_Tables.sql` e `V2__Populate_Tables.sql`
não fossem editadas. Posteriormente, foi autorizada explicitamente a alteração
direta dessas migrations porque o banco seria reinicializado. As alterações
estão no commit `972b571` e exigem recriação/reset do banco remoto antes do
deploy.

### Pendências de fechamento

1. Executar novamente a validação final da Tarefa 8:

   ```bash
   bash ./mvnw clean test
   bash ./mvnw package -DskipTests
   docker build -t inventory-manager .
   git diff --check
   ```

2. Repetir o teste de concorrência para confirmar estabilidade:

   ```bash
   bash ./mvnw -Dtest=InventoryConcurrencyIntegrationTest test
   bash ./mvnw -Dtest=InventoryConcurrencyIntegrationTest test
   ```

3. Executar o `code-review` final do Plano 2, limitado a:
   - repository e locking;
   - `BatchService`;
   - testes de transação e concorrência;
   - aderência ao fluxo FEFO;
   - ausência de RabbitMQ, eventos assíncronos, retry, DLQ e observabilidade.

4. Após o reset do Supabase, fazer o deploy da API e confirmar nos logs do
   Render que o Flyway aplicou novamente as migrations V1 e V2.

## Ordem recomendada para concluir

1. Resetar o banco do Supabase, caso isso ainda não tenha sido feito.
2. Executar a validação final do Plano 1.
3. Executar a validação final e a repetição do teste concorrente do Plano 2.
4. Executar os dois code reviews finais.
5. Fazer o deploy e validar a aplicação contra o banco remoto.
6. Somente depois iniciar uma nova etapa de desenvolvimento.

## Estado dos commits

As últimas correções estão separadas nos commits:

- `24ba47f corrige seguranca e agendamento`
- `972b571 alinha entidades e migrations`

O worktree estava limpo no momento desta verificação.
