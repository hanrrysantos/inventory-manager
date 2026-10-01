# Notificações resilientes de reposição Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/resilient-restock-notifications/design.md`
**Status**: Done

> **Sub-agent offer (Execute):** 12 tarefas em 5 fases → provável divisão em 2 workers (~7 + ~5). Confirmar com o usuário antes de despachar.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `AGENTS.md` (JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo), `.github/workflows/backend-ci.yml` (`./mvnw clean test`).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Processador de notificação | unit | Todos os ramos: idempotência `SENT`, insert `PENDING`, owner inválido, incompleto, sucesso, exceção relançada. 1:1 com RESNOT-01–05, RESNOT-10–12 | `backend/src/test/java/**/serviceTest/*Test.java` | `cd backend && bash ./mvnw -Dtest=RestockNotificationProcessorTest test` |
| Listener AMQP | unit | Delegação, poison ACK, exceção propagada. RESNOT-06, RESNOT-10 | `backend/src/test/java/**/event/*Test.java` | `cd backend && bash ./mvnw -Dtest=RestockQueueListenerTest test` |
| Fila + retry + DLQ | integration | Redelivery sem e-mail duplicado; retry recuperável; DLQ após max; job sem histórico. RESNOT-03, RESNOT-07–09, RESNOT-13–15 | `backend/src/test/java/**/integration/*Test.java` | `cd backend && bash ./mvnw -Dtest=RestockNotificationResilienceIntegrationTest,RestockNeededEventIntegrationTest test` |
| Migration / config YAML | none | Gate de build | - | `cd backend && bash ./mvnw -DskipTests package` |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tarefa só unitária | `cd backend && bash ./mvnw -Dtest=RestockNotificationProcessorTest,RestockQueueListenerTest test` |
| Full | Tarefa de integração ou ajuste ponta a ponta | `cd backend && bash ./mvnw -Dtest=RestockNotificationResilienceIntegrationTest,RestockNeededEventIntegrationTest,RestockPublishFailureIntegrationTest,RestockNotificationProcessorTest,RestockQueueListenerTest test` |
| Build | Migration, config ou docs | `cd backend && bash ./mvnw -DskipTests package` |
| Release | Última tarefa | `cd backend && bash ./mvnw clean test` |

---

## Execution Plan

### Phase 1: Persistência

```
T1 → T2 → T3
```

### Phase 2: Processamento

```
T4 → T5
```

### Phase 3: Broker resilience

```
T6 → T7
```

### Phase 4: Testes unitários

```
T8
```

### Phase 5: Testes de integração

```
T9 → T10 → T11
```

### Phase 6: Documentação

```
T12
```

---

## Task Breakdown

### Phase 1: Persistência

### T1: Migration do histórico de notificações

**What**: Cria `tb_restock_notifications` com `event_id` único, FK para owner e índice de status.
**Where**: `backend/src/main/resources/db/migration/V5__Create_Restock_Notifications.sql`
**Depends on**: None
**Reuses**: Padrão Flyway `V1__Create_Tables.sql`
**Requirement**: RESNOT-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Tabela com colunas definidas no design
- [x] `event_id UUID NOT NULL UNIQUE`
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `feat(notification): adiciona migration de historico de reposicao`

---

### T2: Entidade e enum de status

**What**: Mapeia `RestockNotification` e `RestockNotificationStatus` (`PENDING`, `SENT`, `FAILED`).
**Where**: `backend/src/main/java/br/com/hanrry/inventory/notification/entity/RestockNotification.java`
**Depends on**: T1
**Reuses**: Convenções JPA/Lombok do projeto
**Requirement**: RESNOT-01, RESNOT-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Enum e entidade alinhados à migration
- [x] Gate de build passa

**Tests**: none
**Gate**: build

**Commit**: `feat(notification): adiciona entidade de notificacao de reposicao`

---

### T3: Repositório por eventId

**What**: Expõe lookup por `eventId` para idempotência.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/notification/repository/RestockNotificationRepository.java`
**Depends on**: T2
**Reuses**: Spring Data JPA existente
**Requirement**: RESNOT-03, RESNOT-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `findByEventId(UUID)` disponível
- [x] Gate de build passa

**Tests**: none
**Gate**: build

**Commit**: `feat(notification): adiciona repositorio de notificacao de reposicao`

---

### Phase 2: Processamento

### T4: Processador idempotente

**What**: Implementa `RestockNotificationProcessor` com ramos incompleto, owner ausente, `SENT`, `PENDING`→alerta→`SENT`, e relançamento em falha de PDF/e-mail.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/notification/service/RestockNotificationProcessor.java`
**Depends on**: T3
**Reuses**: `StockAlertService.checkInventoryAndNotify(User)`, `UserRepository`
**Requirement**: RESNOT-01–05, RESNOT-10–12, RESNOT-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Lógica conforme design e context
- [x] Sem dependência nova de `inventory` para entidades de notification
- [x] Gate de build passa

**Tests**: none (cobertos em T8)
**Gate**: build

**Commit**: `feat(notification): processa reposicao com idempotencia`

---

### T5: Listener delegando e propagando falha

**What**: `RestockQueueListener` chama o processador; não captura exceções transitórias; mantém ACK silencioso para poison/owner inválido via processador.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/notification/event/RestockQueueListener.java`
**Depends on**: T4
**Reuses**: `@RabbitListener` existente
**Requirement**: RESNOT-06, RESNOT-10–11

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Listener fino sem lógica duplicada
- [x] Gate de build passa

**Tests**: none (cobertos em T8)
**Gate**: build

**Commit**: `refactor(notification): delega fila ao processador resiliente`

---

### Phase 3: Broker resilience

### T6: Fila DLQ e MessageRecoverer

**What**: Declara `inventory.restock-needed.dlq` e bean `RepublishMessageRecoverer` apontando para ela.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/inventory/config/RestockQueueConfig.java`
**Depends on**: None
**Reuses**: Beans de fila existentes
**Requirement**: RESNOT-08, RESNOT-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Constante DLQ pública para testes
- [x] Recoverer configurado no container factory ou via `@RabbitListener` container custom
- [x] Gate de build passa

**Tests**: none
**Gate**: build

**Commit**: `feat(rabbitmq): adiciona dlq de reposicao`

---

### T7: Retry configurável do listener

**What**: Habilita retry simples do Spring AMQP com `RESTOCK_NOTIFICATION_MAX_ATTEMPTS` (padrão 3) e `default-requeue-rejected: false`.
**Where**: `backend/src/main/resources/application.yaml`, `backend/.env.example`
**Depends on**: T6
**Reuses**: Padrão de env do broker
**Requirement**: RESNOT-06–09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Retry enabled com max attempts configurável
- [x] `.env.example` documenta a variável
- [x] Gate de build passa

**Tests**: none
**Gate**: build

**Commit**: `chore(rabbitmq): configura retry de consumo de reposicao`

---

### Phase 4: Testes unitários

### T8: Testes do processador e listener

**What**: Adiciona `RestockNotificationProcessorTest` e atualiza `RestockQueueListenerTest` para o novo contrato (propagação vs ACK).
**Where**: `backend/src/test/java/br/com/hanrry/inventory/notification/serviceTest/RestockNotificationProcessorTest.java`, `backend/src/test/java/br/com/hanrry/inventory/notification/event/RestockQueueListenerTest.java`
**Depends on**: T5, T7
**Reuses**: Padrão Mockito do projeto
**Requirement**: RESNOT-01–05, RESNOT-06, RESNOT-10–12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Cobertura 1:1 dos ramos do processador listados na matrix
- [x] Listener testado para delegação e propagação
- [x] Quick gate passa

**Tests**: unit (incluídos nesta tarefa)
**Gate**: quick

**Commit**: `test(notification): cobre idempotencia e listener resiliente`

---

### Phase 5: Testes de integração

### T9: Integração de idempotência

**What**: Teste Testcontainers (PostgreSQL + RabbitMQ) republicando a mesma mensagem e assertando um único e-mail e status `SENT`.
**Where**: `backend/src/test/java/br/com/hanrry/inventory/notification/integration/RestockNotificationResilienceIntegrationTest.java`
**Depends on**: T8
**Reuses**: `RestockNeededEventIntegrationTest` helpers
**Requirement**: RESNOT-03, RESNOT-04, RESNOT-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Cenário de redelivery passa
- [x] Full gate parcial passa (classe nova)

**Tests**: integration (incluídos nesta tarefa)
**Gate**: full

**Commit**: `test(notification): cobre idempotencia na fila de reposicao`

---

### T10: Integração de retry e DLQ

**What**: Cenários de e-mail falhando até sucesso e falhando até DLQ + status `FAILED`.
**Where**: `backend/src/test/java/br/com/hanrry/inventory/notification/integration/RestockNotificationResilienceIntegrationTest.java`
**Depends on**: T9
**Reuses**: Mesma classe de integração
**Requirement**: RESNOT-07–09, RESNOT-12

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Mensagem na DLQ após max attempts
- [x] Estoque inalterado
- [x] Full gate passa

**Tests**: integration (incluídos nesta tarefa)
**Gate**: full

**Commit**: `test(notification): cobre retry e dlq de reposicao`

---

### T11: Regressão da fila anterior e job

**What**: Ajusta `RestockNeededEventIntegrationTest` / `RestockQueueListenerTest` onde RABBIT-10 mudou; adiciona assert de job sem linha em `tb_restock_notifications`.
**Where**: `backend/src/test/java/br/com/hanrry/inventory/notification/integration/RestockNeededEventIntegrationTest.java`, `backend/src/test/java/br/com/hanrry/inventory/notification/serviceTest/StockAlertServiceTest.java`
**Depends on**: T10
**Reuses**: Testes existentes da fila de reposição
**Requirement**: RESNOT-13, RESNOT-14, RESNOT-16

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Suíte de notification relacionada verde
- [x] Release gate passa: `cd backend && bash ./mvnw clean test`

**Tests**: integration + unit updates
**Gate**: release

**Commit**: `test(notification): alinha regressao da fila de reposicao`

---

### Phase 6: Documentação

### T12: Documentar comportamento resiliente

**What**: Atualiza `backend/README.md` (próximos passos), menciona DLQ/retry no compose se necessário, referencia variável de max attempts.
**Where**: `backend/README.md`, `backend/docker-compose.yml` (somente se comentário ou doc inline for necessário)
**Depends on**: T11
**Reuses**: Seção “Próximos passos” existente
**Requirement**: RESNOT-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] README reflete idempotência, retry e DLQ implementados
- [x] Observabilidade continua listada como futura
- [x] Build gate passa

**Tests**: none
**Gate**: build

**Commit**: `docs(notification): documenta notificacoes resilientes de reposicao`
