# Observabilidade Design

**Spec**: `.specs/features/observability/spec.md`
**Context**: `.specs/features/observability/context.md`
**Status**: Draft

---

## Architecture Overview

A API ganha Actuator na porta de management `8081`. Micrometer registra JVM/HTTP e quatro counters de domínio. Prometheus raspa `api:8081/actuator/prometheus` na rede do Compose. Grafana em `127.0.0.1:3000` carrega um dashboard provisionado. Um filtro HTTP gera `requestId`, devolve `X-Request-Id` e preenche o MDC. `inventory` incrementa consumo e publicação só depois do commit. `notification` incrementa `SENT` e `FAILED` nas transições de estado. O job agendado não toca os counters.

```mermaid
graph TD
    HTTP[HTTP 8080] --> F[RequestIdFilter]
    F --> API[Controllers JWT]
    F --> MDC[MDC requestId]
    Consume[consumeStock commit] --> Pub[RestockEventPublisher AFTER_COMMIT]
    Pub --> C1[stock_consumption_total]
    Pub -->|convertAndSend ok| C2[restock_events_total]
    Pub --> Q[inventory.restock-needed]
    Q --> Proc[RestockNotificationProcessor]
    Proc -->|markSent| C3[notifications_sent_total]
    Proc -->|markFailed / recoverer| C4[notifications_failed_total]
    Prom[Prometheus] -->|scrape 8081| Act[/actuator/prometheus]
    Graf[Grafana 127.0.0.1:3000] --> Prom
    Job[checkInventoryAndNotify] -.->|sem counters| Proc
```

**Abordagens consideradas**

| Abordagem | Prós | Contras |
| --- | --- | --- |
| **A — Actuator + `micrometer-registry-prometheus` + Compose** (recomendada) | Stack do overview, BOM do Boot 3.3.5, scrape oficial `/actuator/prometheus` | Segunda porta no processo |
| B — Endpoint `/metrics` próprio na `:8080` | Uma porta só | Inventa contrato; JWT ou métricas públicas na API de negócio |
| C — OpenTelemetry Collector | Tracing futuro | Fora da spec; muda o modelo de correlação |

**Escolha**: A. A spec já trava Actuator, Prometheus, Grafana e porta de management separada. Documentado em [Spring Boot 3.3 Actuator](https://docs.spring.io/spring-boot/3.3/reference/actuator/monitoring.html) e [Metrics](https://docs.spring.io/spring-boot/3.3/reference/actuator/metrics.html).

Conformidade com decisões ativas: AD-001 (inventory não importa notification; métricas de cada lado usam `shared`), AD-002 (incremento de publicação só após `convertAndSend` bem-sucedido no `AFTER_COMMIT`), AD-003 (job sem counters), AD-004/AD-005 (incremento `SENT`/`FAILED` nas transições já existentes).

---

## Code Reuse Analysis

### Existing Components to Leverage

| Component | Location | How to Use |
| --- | --- | --- |
| `RestockEventPublisher` | `inventory/event/RestockEventPublisher.java` | Depois do commit: `incrementConsumption`; depois de `convertAndSend`: `incrementPublished`; log já tem `eventId`, passa a incluir `requestId` do MDC |
| `RestockNotificationStateService` | `notification/service/RestockNotificationStateService.java` | `incrementSent` em `markSent` se o status anterior não era `SENT`; `incrementFailed` em `markFailed` se não era `FAILED` |
| `RestockNotificationMessageRecoverer` | `notification/event/RestockNotificationMessageRecoverer.java` | `incrementFailed` só na transição para `FAILED` |
| `RestockNotificationProcessor` | `notification/service/RestockNotificationProcessor.java` | Mensagem incompleta continua sem claim e sem counter; logs de `eventId` permanecem |
| `StockAlertService` | `notification/service/StockAlertService.java` | Job intacto; não injeta métricas |
| `SecurityConfig` | `auth/config/SecurityConfig.java` | Segunda `SecurityFilterChain` com `EndpointRequest.toAnyEndpoint()` e `permitAll`; cadeia `/api/v1/**` inalterada |
| `docker-compose.yml` | `backend/docker-compose.yml` | Serviços `prometheus` e `grafana`; healthcheck da API troca `/v3/api-docs` por `http://localhost:8081/actuator/health` |
| Testcontainers + MockMvc | `RestockNeededEventIntegrationTest`, `SecurityIntegrationTest` | Novo teste de integração com Postgres/Rabbit e `TestRestTemplate` na porta de management |

### Integration Points

| System | Integration Method |
| --- | --- |
| Micrometer | `MeterRegistry` autoconfigurado; `io.micrometer:micrometer-registry-prometheus` sem versão (BOM) |
| Actuator | `spring-boot-starter-actuator`; `management.server.port=8081`; `exposure.include=health,prometheus` |
| Prometheus | Scrape `api:8081` path `/actuator/prometheus`, sem auth |
| Grafana | Provisionamento de datasource Prometheus + um dashboard JSON |
| Logs | MDC `requestId`; `logging.pattern.console` com `%X{requestId}` |

---

## Components

### Actuator / management

- **Purpose**: Health e scrape fora da porta HTTP de negócio.
- **Location**: `backend/src/main/resources/application.yaml`
- **Interfaces**: `GET :8081/actuator/health`, `GET :8081/actuator/prometheus`
- **Dependencies**: `spring-boot-starter-actuator`, `micrometer-registry-prometheus`
- **Reuses**: Porta da API `server.port` / `PORT` inalterada
- **Behavior**: `env` e `heapdump` não entram no include. Compose publica `8081` só em `127.0.0.1` se mapear. Dentro da rede Docker a API escuta `0.0.0.0:8081` para o Prometheus.

### `ActuatorSecurityConfig`

- **Purpose**: Libera só endpoints Actuator; JWT da API permanece.
- **Location**: `backend/src/main/java/br/com/hanrry/inventory/auth/config/SecurityConfig.java` (segunda chain `@Order`)
- **Interfaces**: `SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity)`
- **Dependencies**: `EndpointRequest.toAnyEndpoint()`
- **Reuses**: Cadeia atual de `/api/v1/**`

O Boot 3.3 recua a auto-config de segurança do Actuator quando já existe `SecurityFilterChain`. Sem esta chain, `anyRequest().authenticated()` bloquearia o scrape.

### `RequestIdFilter`

- **Purpose**: Gera UUID de request, header e MDC.
- **Location**: `backend/src/main/java/br/com/hanrry/inventory/shared/web/RequestIdFilter.java`
- **Interfaces**: `void doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)`
- **Dependencies**: SLF4J MDC
- **Reuses**: `OncePerRequestFilter`
- **Behavior**: Ignora `X-Request-Id` de entrada. Sempre gera UUID. Seta header de resposta. `MDC.put("requestId", id)` e `MDC.remove` no `finally`.

### `RestockMetrics`

- **Purpose**: Quatro counters com try/catch para não quebrar estoque nem consumidor.
- **Location**: `backend/src/main/java/br/com/hanrry/inventory/shared/observability/RestockMetrics.java`
- **Interfaces**:
  - `void incrementConsumption()`
  - `void incrementPublished()`
  - `void incrementSent()`
  - `void incrementFailed()`
- **Dependencies**: `MeterRegistry`
- **Reuses**: Nada de `notification` em `inventory`
- **Behavior**: Nomes Micrometer `stock.consumption`, `restock.events`, `notifications.sent`, `notifications.failed` (Prometheus acrescenta `_total`). Exceção no registry é logada e engolida.

### Hooks de domínio

- **`RestockEventPublisher.on`**: `AFTER_COMMIT` → `incrementConsumption()`; `convertAndSend` ok → `incrementPublished()`; log `requestId` (MDC) + `eventId`. Falha de broker: consumo incrementado, publicação não.
- **`RestockNotificationStateService.markSent`**: incrementa sent se o status persistido não era `SENT`.
- **`RestockNotificationStateService.markFailed`**: incrementa failed se não era `FAILED`.
- **`RestockNotificationMessageRecoverer`**: incrementa failed só se mudou para `FAILED`.
- **`StockAlertService`**: sem `RestockMetrics`.

### Compose Prometheus / Grafana

- **Purpose**: Scrape e dashboard locais.
- **Location**: `backend/docker-compose.yml`, `backend/observability/prometheus/prometheus.yml`, `backend/observability/grafana/`
- **Dependencies**: imagens oficiais `prom/prometheus` e `grafana/grafana`
- **Behavior**: Prometheus `127.0.0.1:9090`, Grafana `127.0.0.1:3000`. API não depende desses serviços. Credencial Grafana por env com padrão local.

---

## Data Models

Sem tabela nova. Estado de métricas é in-process (Micrometer) + TSDB do Prometheus local.

Nomes Prometheus obrigatórios:

```text
stock_consumption_total
restock_events_total
notifications_sent_total
notifications_failed_total
```

Sem tags no P1.

---

## Error Handling Strategy

| Error Scenario | Handling | User Impact |
| --- | --- | --- |
| Rollback de `consumeStock` | `AFTER_COMMIT` não dispara | Counters iguais; erro de estoque atual |
| Broker fora no publish | Log; `incrementConsumption` já ocorreu; sem `incrementPublished` | `204`; alerta da request pode faltar |
| `MeterRegistry` lança | `RestockMetrics` captura e loga | `204` e consumidor seguem |
| Prometheus/Grafana parados | API não chama os dois | `204`; dashboard vazio |
| Mensagem incompleta | Sem claim, sem increment | Log de erro com `eventId` |
| Owner ausente | `markFailed` + `incrementFailed` | Sem e-mail, sem retry |
| Redelivery `SENT` | `SKIP_ALREADY_DONE`; sem increment | Sem segundo e-mail |
| Retry depois sucesso | Um `markSent` → um `incrementSent` | Um e-mail |

---

## Risks & Concerns

| Concern | Location (file:line) | Impact | Mitigation |
| --- | --- | --- | --- |
| Counter Micrometer não participa da transação JPA | `BatchService.java:93-134` | Incrementar dentro de `consumeStock` subiria o counter mesmo em rollback posterior | Incrementar só no `AFTER_COMMIT` do publicador; evento só existe se o consumo não lançou |
| `SecurityFilterChain` atual autentica `anyRequest` | `SecurityConfig.java:75` | Scrape 401 se Actuator cair na cadeia da API | Chain `@Order` com `EndpointRequest`; teste 200 na `:8081` e 401/404 na `:8080` |
| Healthcheck Compose usa `/v3/api-docs` | `docker-compose.yml:68` | Probe não reflete saúde da app | Trocar para `:8081/actuator/health` |
| Imagem da API precisa de `wget` no healthcheck | `docker-compose.yml:68` | Probe quebra se o binário sumir | Manter `wget`; só muda a URL |
| `OutputCapture` de testes unitários não aplica `logging.pattern` | `RestockEventPublisherTest.java:76-88` | `requestId` no pattern não aparece no unitário | Logar `requestId=` no texto da mensagem, lido do MDC |
| Teste de compose Grafana não roda na suíte Maven | `backend/docker-compose.yml` | OBS-18–20 sem gate de processo | Tarefa de compose valida `docker compose config` e arquivos provisionados; T8 prova 204 sem Prometheus |

---

## Tech Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Porta de management | `8081` (`MANAGEMENT_PORT`) | Discretion do context; não colide com `8080` |
| Segurança do scrape | `EndpointRequest` + `permitAll` na chain do Actuator | Boot 3.3 recua a auto-config quando já há `SecurityFilterChain` |
| Onde incrementar consumo | `RestockEventPublisher` `AFTER_COMMIT` | Counter não dá rollback; evento só após consumo válido |
| Classe de métricas | `shared/observability/RestockMetrics` | AD-001: inventory não depende de notification |
| Nomes Micrometer | `stock.consumption` etc. | Convenção Micrometer; Prometheus emite `*_total` |
| Tags | Nenhuma no P1 | Spec não pede dimensão extra |
| `requestId` de entrada | Ignorado | Spec: servidor gera UUID |
| Prometheus na suíte | Não sobe container Prometheus nos testes | Pull model; T8 cobre 204 sem scraper (OBS-21) |
| Grafana | Um dashboard JSON provisionado | Decisão de context |

**Project-level**: AD-006 — Actuator + Micrometer + Prometheus; endpoints de management fora da porta HTTP de negócio.
