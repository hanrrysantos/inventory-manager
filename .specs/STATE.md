# Project state

## Handoff

| Field | Value |
| --- | --- |
| Branch | `main` (inclui merge PR #15 fila de reposição) |
| Feature in progress | `resilient-restock-notifications` — Execute concluído (T1–T12) |
| Next authorized step | Verifier / `validation.md` |
| Blockers | Nenhum técnico; aguardando confirmação dos defaults em `context.md` |

## Decisions

| ID | Status | Summary |
| --- | --- | --- |
| AD-001 | active | Modular monolith por domínio; notification isolada de transações de inventory |
| AD-002 | active | Publicação RabbitMQ após commit; sem outbox na primeira implementação |
| AD-003 | active | Job agendado de estoque baixo permanece in-process, fora da fila |
| AD-004 | active | Idempotência de reposição por `eventId` + histórico PostgreSQL (`PENDING`/`SENT`/`FAILED`) |
| AD-005 | active | Retry Spring listener + DLQ `inventory.restock-needed.dlq` após max attempts (padrão 3) |
