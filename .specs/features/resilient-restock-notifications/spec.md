# Notificações resilientes de reposição Specification

## Problem Statement

A fila `inventory.restock-needed` desacopla o consumo de estoque do PDF e do e-mail, mas ainda trata falhas transitórias como definitivas: o consumidor registra erro e confirma a mensagem. Redelivery acidental pode enviar e-mails duplicados ao gestor. Não há histórico persistido do processamento.

Esta etapa adiciona idempotência por `eventId`, estados `PENDING` / `SENT` / `FAILED`, retry limitado e DLQ, conforme `docs/architecture/overview.md`, sem alterar a publicação pós-commit nem o job agendado.

## Goals

- [x] Redelivery da mesma mensagem não envia mais de um e-mail equivalente para o mesmo `eventId`.
- [x] Falha transitória de PDF ou e-mail permite novas tentativas até um limite configurável; depois disso a mensagem vai para DLQ e o registro fica `FAILED`.
- [x] Cada `eventId` processado pela fila deixa rastro consultável no PostgreSQL.

## Out of Scope

| Feature | Reason |
| --- | --- |
| Transactional Outbox | Decisão arquitetural adiada; falha de publicação após commit continua só com log |
| Retry ou DLQ na publicação (`RestockEventPublisher`) | Escopo é o consumidor de notificação |
| Idempotência ou fila para o job `@Scheduled` | O job permanece in-process, como na fila de reposição |
| Métricas Prometheus / Actuator | Fase de observabilidade |
| API HTTP de consulta ao histórico | Persistência interna; sem endpoint público nesta etapa |
| Reprocessamento manual a partir da DLQ | Operação futura; a DLQ só recebe mensagens após esgotar retry |
| Mudança de PDF, fórmula de reposição, `RESEND_TO` ou contrato de consumo HTTP | Comportamento de negócio do alerta permanece |
| Nova fila além de `inventory.restock-needed` e sua DLQ | RabbitMQ só neste caminho |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Chave de idempotência | `eventId` (UUID) da mensagem | Já único por consumo confirmado; alinhado ao overview | y |
| Estados persistidos | `PENDING`, `SENT`, `FAILED` | Estados conceituais do overview | y |
| Sucesso sem e-mail | Releitura sem estoque baixo marca `SENT` e não envia e-mail | Mesma regra da fila anterior; redelivery não reprocessa | y |
| Retry | `spring.rabbitmq.listener.simple.retry` com `max-attempts` configurável (padrão 3), `default-requeue-rejected: false` | Padrão Spring Boot 3; evita retry infinito | y |
| DLQ | Fila durável `inventory.restock-needed.dlq` via `RepublishMessageRecoverer` após esgotar retry | Overview exige DLQ; recoverer oficial do Spring AMQP | y |
| Falha transitória | Exceção não tratada no listener após registrar `PENDING`; Spring faz retry | Separa falha recuperável de poison message | y |
| Mensagem incompleta | Log, confirmação imediata, sem insert, sem retry, sem DLQ | Poison permanente; mesma intenção da etapa anterior | y |
| Proprietário inexistente | Log, registro `FAILED` com motivo, confirmação sem retry | Reprocessar não cria o usuário | y |
| Concorrência no mesmo `eventId` | Unique constraint em `event_id`; segunda entrega concurrente trata registro existente como idempotente | Impede dois e-mails pelo mesmo evento | y |
| Limite de retry | Propriedade `RESTOCK_NOTIFICATION_MAX_ATTEMPTS` (padrão `3`) | Configurável sem redeploy de código | y |
| Local da config AMQP | Estender `RestockQueueConfig` e `application.yaml` | Mesmo ponto da fila principal | y |

**Open questions:** none — defaults acima seguem `context.md` (Guided) até confirmação explícita da spec.

---

## User Stories

### P1: Idempotência na fila ⭐ MVP

**User Story**: As a gestor, I want the same restock queue message to be processed at most once for email purposes, so that accidental redelivery does not spam my inbox.

**Why P1**: Requisito central do overview e gap explícito da fila de reposição.

**Acceptance Criteria**:

1. WHEN the consumer receives a complete message with a new `eventId` THEN the system SHALL persist a notification row in status `PENDING` before calling PDF or email.
2. WHEN processing completes successfully for that `eventId` THEN the system SHALL set status to `SENT` and SHALL send at most one email for that `eventId`.
3. WHEN the consumer receives a complete message whose `eventId` already has status `SENT` THEN the system SHALL NOT send email and SHALL acknowledge the message.
4. WHEN the releitura finds no low-stock products for the owner THEN the system SHALL NOT send email and SHALL set status to `SENT` for that `eventId`.
5. The system SHALL enforce uniqueness of `eventId` in the notification history table.

**Independent Test**: Publicar consumo, processar uma vez com e-mail mockado; republicar manualmente a mesma mensagem; verificar um único envio e status `SENT`.

---

### P1: Retry e DLQ ⭐ MVP

**User Story**: As a operador, I want transient PDF or email failures to retry automatically and permanently failed messages to land in a DLQ, so that stock stays committed while notifications get another chance.

**Why P1**: Substitui o comportamento “log + ACK” da fila anterior para falhas recuperáveis.

**Acceptance Criteria**:

1. IF PDF generation or email sending fails with a recoverable error during processing THEN the system SHALL leave inventory unchanged and SHALL NOT acknowledge until retries are exhausted or processing succeeds.
2. WHEN processing succeeds after one or more retries THEN the system SHALL set status to `SENT` and SHALL acknowledge the message.
3. WHEN retry attempts reach the configured maximum without success THEN the system SHALL set status to `FAILED`, SHALL republish the message to `inventory.restock-needed.dlq`, and SHALL acknowledge the original delivery.
4. The system SHALL NOT perform unbounded retries on the main queue.

**Independent Test**: Mock de e-mail falhando nas primeiras tentativas e passando na última; depois mock falhando sempre até a mensagem aparecer na DLQ com status `FAILED`.

---

### P1: Poison messages e integridade ⭐ MVP

**User Story**: As a operador, I want invalid or permanently impossible messages to stop quickly without retry storms, so that the queue stays healthy.

**Why P1**: Protege o broker e o consumidor junto com idempotência.

**Acceptance Criteria**:

1. IF the message lacks any of `eventId`, `productId`, `occurredAt`, or `ownerId` THEN the system SHALL log an error, SHALL NOT insert a notification row, SHALL NOT retry, and SHALL acknowledge the message.
2. IF `ownerId` does not exist in `tb_users` THEN the system SHALL persist `FAILED` with reason, SHALL NOT retry, and SHALL acknowledge the message.
3. IF processing fails after retries THEN the system SHALL leave committed batches and inventory logs unchanged.

**Independent Test**: Mensagem incompleta some da fila principal sem linha nova; `ownerId` inválido gera `FAILED` e não reenfileira.

---

### P2: Job agendado inalterado

**User Story**: As a gestor, I want the scheduled scan to behave as today, so that only the queue path gains resilience.

**Why P2**: Preserva decisão da fila de reposição.

**Acceptance Criteria**:

1. WHILE the scheduled job runs THEN the system SHALL call `checkInventoryAndNotify()` in-process without writing to the notification history table for that run.
2. WHILE the scheduled job runs THEN the system SHALL NOT publish to RabbitMQ.

**Independent Test**: Job dispara e-mail (mock) sem linhas novas em `tb_restock_notifications` atribuíveis ao job.

---

## Edge Cases

1. WHEN two deliveries of the same `eventId` overlap in time THEN the system SHALL produce at most one email (unique constraint + idempotent read of `SENT`).
2. WHEN retry is in progress for an `eventId` THEN a duplicate delivery SHALL NOT send a second email before `SENT`.
3. IF the broker restarts after a message was published but before consume THEN the system SHALL still process with idempotency once the consumer returns.
4. The inventory module SHALL NOT depend on the notification entity; only the notification consumer writes history.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| RESNOT-01 | P1: Idempotência na fila | - | Verified |
| RESNOT-02 | P1: Idempotência na fila | - | Verified |
| RESNOT-03 | P1: Idempotência na fila | - | Verified |
| RESNOT-04 | P1: Idempotência na fila | - | Verified |
| RESNOT-05 | P1: Idempotência na fila | - | Verified |
| RESNOT-06 | P1: Retry e DLQ | - | Verified |
| RESNOT-07 | P1: Retry e DLQ | - | Verified |
| RESNOT-08 | P1: Retry e DLQ | - | Verified |
| RESNOT-09 | P1: Retry e DLQ | - | Verified |
| RESNOT-10 | P1: Poison messages | - | Verified |
| RESNOT-11 | P1: Poison messages | - | Verified |
| RESNOT-12 | P1: Poison messages | - | Verified |
| RESNOT-13 | P2: Job agendado | - | Verified |
| RESNOT-14 | P2: Job agendado | - | Verified |
| RESNOT-15 | Edge: concorrência | - | Verified |
| RESNOT-16 | Edge: limites de módulo | - | Verified |

**Coverage:** 16 total, 16 mapped to tasks, 0 unmapped

---

## Success Criteria

- [x] Redelivery manual ou acidental do mesmo `eventId` não duplica e-mail.
- [x] Falha simulada de e-mail recupera dentro do limite de retry; falha persistente vai para `inventory.restock-needed.dlq` com status `FAILED`.
- [x] Mensagem incompleta e owner inválido não geram retry storm.
- [x] `POST /api/v1/batches/consume` e publicação pós-commit permanecem com o comportamento validado na fila de reposição.
- [x] Job agendado continua sem fila e sem histórico por execução.
