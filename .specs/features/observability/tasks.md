# Observabilidade Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/observability/design.md`
**Status**: Implemented (uncommitted)

> **Sub-agent offer (Execute):** 11 tarefas em 5 fases → 2 batches (P1+P2+P3 = 7, P4+P5 = 4). Confirmar com o usuário antes de despachar.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `AGENTS.md` (JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo), `.github/workflows/backend-ci.yml` (`./mvnw clean test`).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Actuator e segurança | integration | Health 200 na management; Prometheus text + série JVM; prometheus ausente na porta da API; `env`/`heapdump` não expostos; JWT das rotas `/api/v1/**` inalterado. OBS-01–05 | `backend/src/test/java/**/integration/*Test.java` | `cd backend && bash ./mvnw -Dtest=ActuatorEndpointsTest,SecurityIntegrationTest test` |
| Filtro HTTP e métricas | unit | `X-Request-Id` UUID gerado no servidor; MDC; counters +1; falha do registry não relança; publicação só após send; `SENT`/`FAILED` sem duplicar. OBS-07–17, OBS-22–27 | `backend/src/test/java/**/*Test.java` | `cd backend && bash ./mvnw -Dtest=RequestIdFilterTest,RestockMetricsTest,RestockEventPublisherTest,RestockNotificationStateServiceTest,RestockNotificationProcessorTest,StockAlertServiceTest test` |
| Consumo + scrape ponta a ponta | integration | 204 sobe consumo e publicação; rollback e job não sobem; redelivery `SENT` não sobe sent; header `X-Request-Id`; 204 sem Prometheus. OBS-07–16, OBS-21–26 | `backend/src/test/java/**/integration/*Test.java` | `cd backend && bash ./mvnw -Dtest=ObservabilityIntegrationTest,RestockNeededEventIntegrationTest,RestockPublishFailureIntegrationTest test` |
| Compose, YAML, dashboard, docs | none | Gate de build | - | `cd backend && bash ./mvnw -DskipTests package` |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tarefa só unitária | `cd backend && bash ./mvnw -Dtest=RequestIdFilterTest,RestockMetricsTest,RestockEventPublisherTest,RestockNotificationStateServiceTest,RestockNotificationProcessorTest,StockAlertServiceTest test` |
| Full | Tarefa de integração ou Actuator | `cd backend && bash ./mvnw -Dtest=ActuatorEndpointsTest,ObservabilityIntegrationTest,SecurityIntegrationTest,RestockNeededEventIntegrationTest,RestockPublishFailureIntegrationTest,RequestIdFilterTest,RestockMetricsTest,RestockEventPublisherTest,RestockNotificationProcessorTest,StockAlertServiceTest test` |
| Build | Dependência, YAML, compose ou docs | `cd backend && bash ./mvnw -DskipTests package` |
| Release | Última tarefa | `cd backend && bash ./mvnw clean test` |

---

## Execution Plan

Fases em ordem. Tarefas dentro da fase em ordem.

### Phase 1: Fundação

```
T1 -> T2 -> T3
```

### Phase 2: Correlação

```
T4
```

### Phase 3: Counters

```
T5 -> T6
T5 -> T7
```

### Phase 4: Integração

```
T8
```

### Phase 5: Compose e docs

```
T9 -> T10 -> T11
```

---

## Task Breakdown

### Phase 1: Fundação

### T1: Adicionar Actuator e registry Prometheus

**What**: Inclui `spring-boot-starter-actuator` e `io.micrometer:micrometer-registry-prometheus` sem versão fixa, pelo BOM do parent 3.3.5.
**Where**: `backend/pom.xml`
**Depends on**: None
**Reuses**: parent Spring Boot `3.3.5`
**Requirement**: OBS-01, OBS-02

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] As duas dependências estão no `pom.xml` sem versão explícita
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `build(observability): adiciona actuator e registry prometheus`

---

### T2: Configurar porta de management e exposição

**What**: Define `management.server.port` 8081, `exposure.include` só `health,prometheus` e o pattern de log com `%X{requestId}`.
**Where**: `backend/src/main/resources/application.yaml`
**Depends on**: T1
**Reuses**: `server.port` / `PORT` atuais
**Requirement**: OBS-01, OBS-02, OBS-03, OBS-04, OBS-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `management.server.port` lê `MANAGEMENT_PORT` com padrão `8081`
- [x] `management.endpoints.web.exposure.include` é `health,prometheus`
- [x] `logging.pattern.console` inclui `%X{requestId}`
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `chore(observability): configura porta de management do actuator`

---

### T3: Liberar scrape do Actuator sem abrir a API

**What**: Adiciona `SecurityFilterChain` com `EndpointRequest.toAnyEndpoint()` e `permitAll`; prova health/prometheus na management e ausência na porta da API.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/auth/config/SecurityConfig.java`
**Depends on**: T2
**Reuses**: cadeia JWT de `/api/v1/**` em `SecurityConfig.java`
**Requirement**: OBS-01, OBS-02, OBS-03, OBS-04, OBS-05

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Chain do Actuator não exige JWT
- [x] Cadeia `/api/v1/**` permanece autenticada
- [x] `ActuatorEndpointsTest` cobre OBS-01–05
- [x] Gate completo passa: `cd backend && bash ./mvnw -Dtest=ActuatorEndpointsTest,SecurityIntegrationTest test`
- [x] Test count: sem deleção silenciosa em `SecurityIntegrationTest`

**Tests**: integration
**Gate**: full

**Commit**: `feat(auth): libera health e prometheus na porta de management`

---

### Phase 2: Correlação

### T4: Filtro de `requestId`

**What**: `OncePerRequestFilter` gera UUID, seta `X-Request-Id`, preenche MDC `requestId` e limpa no `finally`.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/shared/web/RequestIdFilter.java`
**Depends on**: T3
**Reuses**: filtros servlet do Spring já usados em `auth`
**Requirement**: OBS-14, OBS-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Header de entrada `X-Request-Id` é ignorado
- [x] Resposta tem UUID em `X-Request-Id`
- [x] MDC contém o mesmo valor durante a cadeia e some depois
- [x] Gate rápido passa: `cd backend && bash ./mvnw -Dtest=RequestIdFilterTest test`
- [x] Test count: `RequestIdFilterTest` ≥ 3

**Tests**: unit
**Gate**: quick

**Commit**: `feat(shared): adiciona request id nos logs http`

---

### Phase 3: Counters

### T5: Fachada `RestockMetrics`

**What**: Quatro counters Micrometer com try/catch e log em falha do registry.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/shared/observability/RestockMetrics.java`
**Depends on**: T1
**Reuses**: `MeterRegistry` autoconfigurado
**Requirement**: OBS-07, OBS-08, OBS-09, OBS-10, OBS-27

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Nomes Micrometer `stock.consumption`, `restock.events`, `notifications.sent`, `notifications.failed`
- [x] Cada incremento +1; exceção do registry não relança
- [x] Gate rápido passa: `cd backend && bash ./mvnw -Dtest=RestockMetricsTest test`
- [x] Test count: `RestockMetricsTest` ≥ 5

**Tests**: unit
**Gate**: quick

**Commit**: `feat(shared): adiciona counters de reposicao`

---

### T6: Incrementar consumo e publicação após commit

**What**: No `AFTER_COMMIT`, incrementa consumo; incrementa publicação só após `convertAndSend`; loga `requestId` e `eventId`.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/inventory/event/RestockEventPublisher.java`
**Depends on**: T5
**Reuses**: `RestockEventPublisher` e `RestockEventPublisherTest`
**Requirement**: OBS-07, OBS-08, OBS-11, OBS-16, OBS-22, OBS-23

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `incrementConsumption` ocorre no listener `AFTER_COMMIT`
- [x] `incrementPublished` só depois de send bem-sucedido
- [x] Falha de broker incrementa consumo e não incrementa publicação
- [x] Log contém `requestId=` (MDC) e `eventId=`
- [x] Gate rápido passa: `cd backend && bash ./mvnw -Dtest=RestockEventPublisherTest test`
- [x] Test count: `RestockEventPublisherTest` ≥ 4 (os atuais) mais os de métrica

**Tests**: unit
**Gate**: quick

**Commit**: `feat(inventory): incrementa metricas apos commit da reposicao`

---

### T7: Incrementar sent e failed nas transições

**What**: Incrementa sent/failed só na primeira transição para `SENT`/`FAILED`; incompleto e job não incrementam.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/notification/service/RestockNotificationStateService.java`
**Depends on**: T5
**Reuses**: `RestockNotificationProcessor`, `RestockNotificationMessageRecoverer`, `StockAlertService`
**Requirement**: OBS-09, OBS-10, OBS-12, OBS-13, OBS-17, OBS-24, OBS-25, OBS-26

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `markSent` incrementa uma vez; redelivery `SENT` não incrementa
- [x] `markFailed` e recoverer incrementam failed só na transição
- [x] Mensagem incompleta e job não chamam `RestockMetrics`
- [x] Logs de conclusão/falha do consumidor ainda têm `eventId=`
- [x] Gate rápido passa: `cd backend && bash ./mvnw -Dtest=RestockNotificationStateServiceTest,RestockNotificationProcessorTest,StockAlertServiceTest test`
- [x] Test count: sem deleção nos testes atuais do processador e do job

**Tests**: unit
**Gate**: quick

**Commit**: `feat(notification): incrementa metricas de envio e falha`

---

### Phase 4: Integração

### T8: Teste ponta a ponta de métricas e `requestId`

**What**: Integração Postgres + Rabbit prova 204, counters, header, rollback, job, redelivery `SENT` e 204 sem Prometheus.
**Where**: `backend/src/test/java/br/com/hanrry/inventory/notification/integration/ObservabilityIntegrationTest.java`
**Depends on**: T3, T4, T6, T7
**Reuses**: `RestockNeededEventIntegrationTest`, `RestockPublishFailureIntegrationTest`
**Requirement**: OBS-07, OBS-08, OBS-09, OBS-10, OBS-11, OBS-12, OBS-13, OBS-14, OBS-16, OBS-21, OBS-22, OBS-23, OBS-25, OBS-26

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Consume 204 sobe `stock_consumption_total` e `restock_events_total`
- [x] `SENT` sobe `notifications_sent_total`; redelivery não sobe de novo
- [x] Rollback e job não sobem os quatro
- [x] Resposta tem `X-Request-Id`; scrape Prometheus contém JVM e os counters
- [x] Gate completo passa: `cd backend && bash ./mvnw -Dtest=ObservabilityIntegrationTest,RestockNeededEventIntegrationTest,RestockPublishFailureIntegrationTest test`
- [x] Test count: testes de regressão da fila e da resiliência intactos

**Tests**: integration
**Gate**: full

**Commit**: `test(observability): cobre counters e request id no consumo`

---

### Phase 5: Compose e docs

### T9: Prometheus e healthcheck no Compose

**What**: Sobe Prometheus na rede do Compose, raspa `api:8081`, publica `127.0.0.1:9090` e troca o healthcheck da API para `/actuator/health` na 8081.
**Where**: `backend/docker-compose.yml`
**Depends on**: T2
**Reuses**: rede `inventory-network` e healthcheck com `wget`
**Requirement**: OBS-06, OBS-18

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Serviço `prometheus` usa imagem oficial e scrape sem credencial
- [x] API não `depends_on` Prometheus
- [x] Healthcheck da API chama `http://localhost:8081/actuator/health`
- [x] `docker compose -f backend/docker-compose.yml config --quiet` retorna 0
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `build(compose): adiciona prometheus e healthcheck do actuator`

---

### T10: Dashboard Grafana provisionado

**What**: Serviço Grafana em `127.0.0.1:3000` com datasource Prometheus e dashboard dos quatro counters mais JVM ou HTTP.
**Where**: `backend/observability/grafana/dashboards/inventory-restock.json`
**Depends on**: T9
**Reuses**: volume/provisioning padrão do Grafana
**Requirement**: OBS-19, OBS-20

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Grafana escuta `127.0.0.1:3000`
- [x] Dashboard provisionado referencia os quatro nomes Prometheus da spec
- [x] Credencial via env com padrão local, sem secret real
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `build(compose): provisiona dashboard grafana de reposicao`

---

### T11: Documentar scrape, Grafana e `requestId`

**What**: Atualiza README e `.env.example` com porta 8081, Prometheus, Grafana e `X-Request-Id`.
**Where**: `backend/README.md`
**Depends on**: T10
**Reuses**: tabela de variáveis do README
**Requirement**: OBS-14, OBS-18, OBS-19

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] README cita health `:8081/actuator/health`, Prometheus `:9090`, Grafana `:3000`
- [x] `.env.example` tem `MANAGEMENT_PORT` e credenciais Grafana locais
- [x] Gate de release passa: `cd backend && bash ./mvnw clean test`

**Tests**: none
**Gate**: build

**Commit**: `docs(observability): documenta actuator prometheus e grafana`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5

Phase 1:  T1 -> T2 -> T3
Phase 2:  T4
Phase 3:  T5 -> T6
          T5 -> T7
Phase 4:  T8
Phase 5:  T9 -> T10 -> T11
```

T3, T4, T6 e T7 bloqueiam T8 (dependência entre fases; sem seta no diagrama da fase 4). T2 bloqueia T9.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Dependências Actuator | 1 arquivo | ✅ Granular |
| T2: YAML management | 1 arquivo | ✅ Granular |
| T3: Security + teste Actuator | 1 classe de produção + testes no mesmo task | ✅ Granular |
| T4: RequestIdFilter | 1 componente | ✅ Granular |
| T5: RestockMetrics | 1 classe | ✅ Granular |
| T6: Publisher | 1 classe | ✅ Granular |
| T7: StateService (hooks; recoverer no mesmo fluxo de status) | 1 classe âncora | ⚠️ Coeso |
| T8: ObservabilityIntegrationTest | 1 classe de teste | ✅ Granular |
| T9: docker-compose Prometheus | 1 arquivo | ✅ Granular |
| T10: Dashboard Grafana | 1 arquivo âncora | ✅ Granular |
| T11: README | 1 arquivo âncora | ⚠️ Docs + `.env.example` |

**Granularity check**: nenhuma tarefa mistura componentes de domínio distintos. T7 e T11 são 2–3 arquivos do mesmo corte.

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | — | ✅ Match |
| T2 | T1 | T1 -> T2 | ✅ Match |
| T3 | T2 | T2 -> T3 | ✅ Match |
| T4 | T3 | cross-phase | ✅ Match |
| T5 | T1 | cross-phase | ✅ Match |
| T6 | T5 | T5 -> T6 | ✅ Match |
| T7 | T5 | T5 -> T7 | ✅ Match |
| T8 | T3, T4, T6, T7 | cross-phase | ✅ Match |
| T9 | T2 | cross-phase | ✅ Match |
| T10 | T9 | T9 -> T10 | ✅ Match |
| T11 | T10 | T10 -> T11 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | pom / config | none | none | ✅ OK |
| T2 | YAML | none | none | ✅ OK |
| T3 | Actuator e segurança | integration | integration | ✅ OK |
| T4 | Filtro HTTP | unit | unit | ✅ OK |
| T5 | Métricas | unit | unit | ✅ OK |
| T6 | Publicador | unit | unit | ✅ OK |
| T7 | Estado de notificação | unit | unit | ✅ OK |
| T8 | Integração consumo + scrape | integration | integration | ✅ OK |
| T9 | Compose | none | none | ✅ OK |
| T10 | Dashboard | none | none | ✅ OK |
| T11 | Docs | none | none | ✅ OK |
