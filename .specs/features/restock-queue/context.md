# Fila de reposição — Context

**Gathered:** 2026-09-29
**Spec:** `.specs/features/restock-queue/spec.md`
**Status:** Ready for design

---

## Feature Boundary

Substituir o processamento in-process de `RestockNeededEvent` por uma fila RabbitMQ entre `inventory` e `notification`. Depois do commit de `consumeStock`, `inventory` publica o evento. `notification` consome a mensagem, relê o estoque baixo, gera o PDF e envia o e-mail. Gatilho, fórmula e contrato HTTP do consumo permanecem.

---

## Implementation Decisions

### Limite deste incremento

- A etapa entrega só o transporte.
- O consumidor processa a mensagem uma vez.
- Falha de PDF ou de e-mail é registrada em log e a mensagem é confirmada.
- Retry, redelivery, DLQ, idempotência e histórico `PENDING` / `SENT` / `FAILED` ficam na etapa seguinte, notificações resilientes.

### Fila indisponível depois do commit

- O consumo HTTP continua com sucesso.
- A falha de publicação entra no log com o `eventId`.
- O alerta daquela requisição pode não sair.
- Não há Transactional Outbox nesta etapa.
- O job agendado continua sendo a varredura posterior.

### Job agendado

- `StockAlertService` continua chamando `checkInventoryAndNotify` no mesmo processo.
- O job não publica `RestockNeededEvent` na fila.
- Só `consumeStock` que confirma a transação publica na fila.

### Proprietário na mensagem

- A mensagem leva `ownerId` além de `eventId`, `productId` e `occurredAt`.
- O publicador lê esse id no thread da requisição, depois do commit, via `OwnerContext`.
- O consumidor relê o estoque baixo desse proprietário.
- Sem `ownerId`, a mensagem é confirmada, a falha entra no log e nenhum e-mail sai.

### Agent's Discretion

Nenhuma área ficou em "você decide".

### Declined / Undiscussed Gray Areas → Assumptions

- Fila e mensagem duráveis, para um restart do broker não descartar um evento já publicado.
- Mensagem sem `eventId`, `productId` ou `occurredAt`: log, confirmação da mensagem e nenhum e-mail.
- Redelivery acidental pode gerar outro e-mail. Deduplicação fica fora.
- Credenciais do broker só por variável de ambiente.
- Sem métricas Prometheus nesta etapa. O log leva o `eventId`.

---

## Specific References

Arquitetura em `docs/architecture/overview.md`: publicação só após commit, fila apenas entre `inventory` e `notification`, sem outbox na primeira entrada do RabbitMQ.

Spec anterior `docs/specs/restock-events-after-commit.md`: gatilho atual, evento como sinal, fórmula `minStock - totalQuantity`, `inventory` sem depender de `notification`.

---

## Deferred Ideas

- Retry, redelivery e DLQ.
- Idempotência contra redelivery.
- Histórico persistido `PENDING` / `SENT` / `FAILED`.
- Transactional Outbox.
- Isolamento do alerta por `owner` e destinatário por conta.
- Observabilidade com Actuator, Micrometer, Prometheus e Grafana.
