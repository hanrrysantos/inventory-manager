# Notificações resilientes de reposição Context

**Spec**: `.specs/features/resilient-restock-notifications/spec.md`
**Captured**: 2026-09-30
**Mode**: Guided (defaults propostos pelo agente; usuário confirma na aprovação da spec)

---

## Feature boundary

Persistência de estado, idempotência, retry limitado e DLQ **somente** no consumidor da fila `inventory.restock-needed`. Publicação pós-commit, job agendado, PDF, fórmula e HTTP de consumo permanecem como na fila de reposição.

---

## Decisions

### 1. Mecanismo de retry

**Decision**: Retry do listener Spring (`spring.rabbitmq.listener.simple.retry`) + `RepublishMessageRecoverer` para `inventory.restock-needed.dlq`.

**Alternatives considered**: NACK manual com requeue; fila de delay com TTL. Descartadas por complexidade operacional desnecessária neste monólito.

**Locked for design**: yes

### 2. Sem e-mail ainda é sucesso

**Decision**: Releitura sem produtos em estoque baixo grava `SENT` e não envia e-mail; redelivery futura ignora.

**Rationale**: Alinha com RABBIT-07; idempotência cobre “processamento concluído”, não “e-mail enviado”.

**Locked for design**: yes

### 3. Poison messages

**Decision**: Mensagem incompleta — ACK imediato, sem linha, sem retry. Owner ausente — linha `FAILED`, ACK imediato, sem retry.

**Locked for design**: yes

### 4. Falha após esgotar retry

**Decision**: Status `FAILED` no PostgreSQL; mensagem republicada na DLQ; estoque inalterado.

**Locked for design**: yes

### 5. Tentativas máximas

**Decision**: Padrão 3, override via `RESTOCK_NOTIFICATION_MAX_ATTEMPTS`.

**Locked for design**: yes

---

## Agent discretion

- Nomes de colunas e índices Flyway seguem o padrão `tb_*` existente.
- Serviço de orquestração fica em `notification/service` (ex.: `RestockNotificationProcessor`); entidade em `notification/entity`.
