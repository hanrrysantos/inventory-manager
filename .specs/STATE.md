# Project state

## Handoff

- **Feature**: observability / `.specs/features/observability/`
- **Phase / Task**: Execute T1–T11 implementados; sem commits (pedido do usuário)
- **Completed**: Specify, Design, Tasks, implementação local e `mvnw clean test` (207 testes, 0 falhas)
- **In-progress**: revisão humana do diff; commits atômicos por tarefa e Verifier ainda não rodaram
- **Next step**: revisar o diff na `feat/observability`; se ok, autorizar commits (T1–T11) e o Verifier
- **Blockers**: nenhum
- **Uncommitted files**: `.specs/**`, `backend/**` (Actuator, métricas, Compose Prometheus/Grafana, testes)
- **Branch**: `feat/observability`

## Decisions

| ID | Status | Summary |
| --- | --- | --- |
| AD-001 | active | Modular monolith por domínio; notification isolada de transações de inventory |
| AD-002 | active | Publicação RabbitMQ após commit; sem outbox na primeira implementação |
| AD-003 | active | Job agendado de estoque baixo permanece in-process, fora da fila |
| AD-004 | active | Idempotência de reposição por `eventId` + histórico PostgreSQL (`PENDING`/`SENT`/`FAILED`) |
| AD-005 | active | Retry Spring listener + DLQ `inventory.restock-needed.dlq` após max attempts (padrão 3) |
| AD-006 | active | Actuator + Micrometer + Prometheus; endpoints de management fora da porta HTTP de negócio |
