# Fila de reposição Specification

## Problem Statement

O alerta de reposição já espera o commit, mas ainda roda no mesmo processo que o consumo. PDF e e-mail acontecem antes da resposta HTTP terminar, e não há fila entre `inventory` e `notification`.

Esta etapa coloca `RestockNeededEvent` no RabbitMQ depois do commit. O consumidor processa o alerta fora da transação e fora do tempo da resposta HTTP.

## Goals

- [ ] Um `consumeStock` confirmado publica uma mensagem e responde `204` sem esperar PDF ou e-mail.
- [ ] Rollback de consumo não publica mensagem.
- [ ] Falha de publicação, PDF ou e-mail não desfaz lote nem log e não muda o `204`.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Retry, redelivery e DLQ | Etapa seguinte: notificações resilientes |
| Idempotência contra redelivery | Mesma etapa seguinte. Um redelivery pode enviar outro e-mail |
| Histórico `PENDING` / `SENT` / `FAILED` | Mesma etapa seguinte |
| Transactional Outbox | Arquitetura adia o outbox. Falha de publicação pode perder o alerta daquela requisição |
| Isolamento do alerta por `owner` e destinatário por conta | Fora desde a spec de eventos após commit |
| Nova regra de limiar ou PDF por produto | O evento continua um sinal. O consumidor relê o estoque baixo |
| RabbitMQ em `auth`, `user`, `product` ou no consumo FEFO | A fila existe só neste caminho |
| Actuator, Micrometer, Prometheus e Grafana | Fase de observabilidade |
| Mudança do contrato HTTP de consumo, criação ou entrada de estoque | O `204` de `POST /api/v1/batches/consume` permanece |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Limite do incremento | Só transporte. Consumidor processa uma vez, registra falha de PDF ou e-mail e confirma a mensagem | Decisão de 2026-09-29. Retry e DLQ são a etapa seguinte | y |
| Broker fora depois do commit | HTTP `204`, log com `eventId`, alerta daquela requisição pode não sair | Decisão de 2026-09-29. Sem outbox | y |
| Job agendado | `checkInventoryAndNotify` direto, sem publicar na fila | Decisão de 2026-09-29. Só `consumeStock` confirmado publica | y |
| Durabilidade | Fila durável e mensagem persistente | Restart do broker não descarta evento já publicado. A perda aceita é a falha de publicação | y |
| Mensagem incompleta | Log, confirmação da mensagem e nenhum e-mail | Sem DLQ, mensagem inválida não pode voltar para sempre | y |
| Redelivery acidental | Pode gerar outro e-mail | Idempotência está fora. Cada mensagem relê o estoque | y |
| Credencial do broker | Variável de ambiente, sem secret versionado | Padrão do repositório | y |
| Métrica de fila | Só log com `eventId` | Prometheus fica na fase de observabilidade | y |
| `ownerId` na mensagem | O publicador lê o proprietário autenticado no thread do request e grava o id na mensagem. O consumidor relê o estoque baixo desse proprietário | `OwnerContext` lança exceção sem `SecurityContext`. O consumidor da fila não tem o contexto da requisição. Sem o id, o alerta do consumo não sai | y |

**Open questions:** none

---

## User Stories

### P1: Consumo confirmado publica na fila ⭐ MVP

**User Story**: As a gestor de estoque, I want the restock signal to leave the stock transaction only after commit, so that a rolled-back consumption never becomes a queue message.

**Why P1**: Sem a publicação pós-commit, a fila reintroduz o problema que a etapa anterior fechou.

**Acceptance Criteria**:

1. WHEN `consumeStock` confirma a transação THEN o sistema SHALL publicar exatamente uma mensagem no RabbitMQ com `eventId`, `productId`, `occurredAt` e `ownerId`.
2. WHEN `consumeStock` lança `InsufficientStockException` THEN o sistema SHALL NOT publicar mensagem no RabbitMQ.
3. WHEN `createBatch` ou `addStock` conclui THEN o sistema SHALL NOT publicar mensagem no RabbitMQ.
4. The sistema SHALL publicar a mensagem de reposição somente depois do commit da transação de `consumeStock`.

**Independent Test**: Consumo com estoque suficiente deixa uma mensagem na fila e o lote persistido. Consumo com estoque insuficiente não deixa mensagem. Criação e entrada de lote não deixam mensagem.

---

### P1: Alerta sai fora da resposta HTTP ⭐ MVP

**User Story**: As a operador, I want `POST /api/v1/batches/consume` to finish with `204` before the email, so that the stock movement does not wait on PDF or Resend.

**Why P1**: É o comportamento assíncrono desta etapa.

**Acceptance Criteria**:

1. WHEN `consumeStock` confirma a transação THEN a API SHALL responder `204 No Content` sem aguardar a geração do PDF nem o envio do e-mail.
2. WHEN o consumidor recebe uma mensagem completa THEN o sistema SHALL reler os produtos com estoque baixo, usar a condição `totalQuantity <= minStock` e a fórmula `minStock - totalQuantity`, gerar o PDF atual e enviar o e-mail atual.
3. WHEN essa releitura não encontra produto com estoque baixo THEN o sistema SHALL NOT enviar e-mail.
4. The módulo `inventory` SHALL NOT depender do módulo `notification`.

**Independent Test**: Com o consumidor parado, o consumo ainda responde `204` e o estoque permanece. Ao processar a mensagem, o e-mail sai só se existir produto em estoque baixo.

---

### P1: Falha externa não desfaz o consumo ⭐ MVP

**User Story**: As a operador, I want a broker, PDF, or email failure to leave the committed consumption in place, so that stock and the HTTP result stay aligned.

**Why P1**: A fila não pode devolver o acoplamento que o evento pós-commit removeu.

**Acceptance Criteria**:

1. IF a publicação no RabbitMQ falhar depois do commit THEN o sistema SHALL manter lotes e logs persistidos, responder `204 No Content` e registrar a falha em log com o `eventId`.
2. IF a geração do PDF ou o envio pelo Resend falhar no consumidor THEN o sistema SHALL manter lotes e logs persistidos, registrar a falha em log com o `eventId` e confirmar a mensagem.
3. IF a mensagem não tiver `eventId`, `productId`, `occurredAt` ou `ownerId` THEN o sistema SHALL registrar a falha em log, confirmar a mensagem e SHALL NOT enviar e-mail.

**Independent Test**: Broker indisponível no publish: `204`, estoque gravado, nenhuma mensagem. Falha do Resend no consumidor: estoque intacto, mensagem confirmada, sem nova tentativa nesta etapa.

---

### P2: Job agendado permanece direto

**User Story**: As a gestor, I want the scheduled low-stock scan to keep calling the alert in process, so that the queue is only the path of a committed consumption.

**Why P2**: O job já cobre a varredura posterior. Colocá-lo na fila mudaria um fluxo que esta etapa não altera.

**Acceptance Criteria**:

1. WHILE o job agendado de `StockAlertService` executa, o sistema SHALL chamar `checkInventoryAndNotify` no mesmo processo.
2. WHILE o job agendado de `StockAlertService` executa, o sistema SHALL NOT publicar `RestockNeededEvent` no RabbitMQ.

**Independent Test**: Disparar o job não cria mensagem na fila e ainda executa a verificação de estoque baixo.

---

## Edge Cases

1. WHEN `consumeStock` confirma com quantidade pedida zero ou negativa e não lança `InsufficientStockException` THEN o sistema SHALL publicar a mensagem. <!-- RABBIT-14 -->
2. IF o consumidor estiver parado WHEN a publicação sucede THEN o sistema SHALL deixar a mensagem na fila até um consumidor recebê-la, e a API SHALL já ter respondido `204`. <!-- RABBIT-15 -->
3. The sistema SHALL usar o RabbitMQ somente entre a publicação de `RestockNeededEvent` e o consumidor de notificação. <!-- RABBIT-16 -->

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| RABBIT-01 | P1: Consumo confirmado publica na fila | - | In Tasks |
| RABBIT-02 | P1: Consumo confirmado publica na fila | - | Done |
| RABBIT-03 | P1: Consumo confirmado publica na fila | - | Done |
| RABBIT-04 | P1: Consumo confirmado publica na fila | - | Done |
| RABBIT-05 | P1: Alerta sai fora da resposta HTTP | - | Done |
| RABBIT-06 | P1: Alerta sai fora da resposta HTTP | - | Done |
| RABBIT-07 | P1: Alerta sai fora da resposta HTTP | - | Done |
| RABBIT-08 | P1: Alerta sai fora da resposta HTTP | - | Done |
| RABBIT-09 | P1: Falha externa não desfaz o consumo | - | Done |
| RABBIT-10 | P1: Falha externa não desfaz o consumo | - | Done |
| RABBIT-11 | P1: Falha externa não desfaz o consumo | - | Done |
| RABBIT-12 | P2: Job agendado permanece direto | - | Done |
| RABBIT-13 | P2: Job agendado permanece direto | - | Done |
| RABBIT-14 | P1: Consumo confirmado publica na fila | - | Done |
| RABBIT-15 | P1: Alerta sai fora da resposta HTTP | - | Done |
| RABBIT-16 | P1: Alerta sai fora da resposta HTTP | - | Done |

**Coverage:** 16 total, 16 mapped to tasks, 0 unmapped

---

## Success Criteria

- [ ] `POST /api/v1/batches/consume` confirmado responde `204` com uma mensagem durável na fila e sem esperar o e-mail.
- [ ] Consumo revertido, `createBatch` e `addStock` não publicam.
- [ ] Falha de broker na publicação, falha de PDF e falha de Resend deixam lote e log persistidos.
- [ ] O job agendado não publica na fila.
- [ ] Nenhum outro módulo publica ou consome no RabbitMQ.
