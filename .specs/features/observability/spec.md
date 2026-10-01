# Observabilidade Specification

## Problem Statement

A API já desacopla estoque, fila e notificação, mas um operador não consegue ver saúde, volume de consumo, publicação, envio ou falha sem ler log. Não há Actuator, Prometheus nem Grafana. Logs HTTP não carregam um id de request, então o `204` não se liga sozinho ao `eventId` da fila.

Esta etapa adiciona Actuator + Micrometer, scrape Prometheus, Grafana local com dashboard, quatro counters de domínio e correlação `requestId` / `eventId`, conforme `docs/architecture/overview.md`.

## Goals

- [x] `/actuator/health` e `/actuator/prometheus` respondem; o healthcheck do Compose usa health.
- [x] Os quatro counters de reposição incrementam nos eventos certos e não duplicam em redelivery `SENT`.
- [x] Prometheus e Grafana sobem no Compose local; Grafana em `127.0.0.1:3000` mostra o dashboard provisionado.
- [x] Cada request HTTP devolve `X-Request-Id`; logs HTTP e de publicação permitem juntar `requestId` e `eventId`.

## Out of Scope

| Feature | Reason |
| --- | --- |
| OpenTelemetry, Jaeger, Micrometer Tracing | Correlação desta etapa é log + header, não tracing distribuído |
| Grafana Cloud, alertas, PagerDuty | Operação local; alertas não estão no overview desta fase |
| Prometheus/Grafana no deploy público | Etapa Deploy; aqui só Compose local |
| Transactional Outbox | Decisão já adiada na fila de reposição |
| Mudança de FEFO, `204`, retry, DLQ, PDF, e-mail ou JWT | Observabilidade observa o fluxo; não altera regra |
| Métricas de catálogo, auth, paginação ou job agendado | Cortes fora do fluxo consume → fila → notificação |
| Endpoints `env`, `heapdump`, `logfile`, `beans` | Vazam configuração e memória |
| API HTTP de consulta às métricas além do scrape Prometheus | O pull do Prometheus é o contrato |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Counters de domínio | Os quatro nomes Prometheus `stock_consumption_total`, `restock_events_total`, `notifications_sent_total`, `notifications_failed_total` | Decisão 2026-10-01; exemplos do overview | y |
| Grão do counter | Incremento +1 por evento, não pela quantidade consumida | Mesmo grão da mensagem de reposição; quantidade já está no estoque | y |
| `notifications_sent_total` | Incrementa quando o status persistido vira `SENT`, inclusive releitura sem e-mail | `SENT` já significa processamento concluído na spec resiliente | y |
| Job agendado | Não incrementa os quatro counters | Job continua in-process, fora da fila | y |
| Scrape | Sem credencial na rede interna do Compose; Actuator em porta de management separada, não na porta HTTP de negócio | Decisão 2026-10-01. API `:8080` já é publicada; juntar Prometheus nela abriria métricas em todo cliente da API | y |
| Alcance local | Prometheus e Grafana desta etapa só no Compose, bind `127.0.0.1`. Métricas da API existem em qualquer run. Wiring de produção fica na etapa Deploy | Resposta à dúvida de 2026-10-01: o padrão (rede privada) vale depois; o Compose é só agora | y |
| Endpoints Actuator | Só `health` e `prometheus` | Suficiente para probe e scrape | y |
| Correlação HTTP | Servidor gera UUID `requestId`; header de resposta `X-Request-Id`; MDC nos logs do request. Não reutiliza header de entrada | Evita id forjado no log; cliente ainda recebe o valor | y |
| Correlação da fila | `eventId` permanece nos logs do consumidor; log de publicação inclui `requestId` e `eventId` | Junta o `204` ao bilhete da fila sem tracing | y |
| Grafana | `127.0.0.1:3000`, um dashboard provisionado com JVM/HTTP e os quatro counters | Decisão 2026-10-01 opção dashboard provisionado | y |
| Credencial Grafana | Variáveis de ambiente com padrão local, sem secret real versionado | Mesmo padrão do RabbitMQ/Postgres no Compose | y |
| Falha de stack de métricas | Prometheus ou Grafana parado não muda `204`, estoque, fila nem e-mail | Pull model; a API não depende do scraper | y |
| Incremento vs transação | Falha ao registrar métrica não falha o `204` nem o consumidor | Observabilidade não pode reverter estoque | y |
| Redelivery | `SENT` reentregue não incrementa `notifications_sent_total` de novo. `FAILED` incrementa uma vez no recoverer | Idempotência já existe no histórico | y |
| Publicação falha após commit | `stock_consumption_total` incrementa; `restock_events_total` não | Consumo confirmou; mensagem não saiu | y |

**Open questions:** none

---

## User Stories

### P1: Saúde e scrape ⭐ MVP

**User Story**: As a operador, I want health and Prometheus metrics on a management port, so that I can probe the app and scrape without opening secrets or the business API.

**Why P1**: Base do overview (Actuator, Micrometer, Prometheus). Sem isso o dashboard não tem fonte.

**Acceptance Criteria**:

1. WHEN the application process is up THEN `GET /actuator/health` on the management port SHALL return HTTP 200 with body status `UP`.
2. WHEN a client scrapes `GET /actuator/prometheus` on the management port THEN the system SHALL return HTTP 200 with Prometheus text exposition including a JVM memory series.
3. The system SHALL NOT serve `/actuator/prometheus` on the business HTTP port used by `/api/v1/**`.
4. The system SHALL NOT expose `/actuator/env` or `/actuator/heapdump`.
5. The system SHALL keep JWT on protected `/api/v1/**` routes unchanged.
6. WHEN Docker Compose starts the API THEN the API healthcheck SHALL call `/actuator/health` and SHALL succeed while the process is up.

**Independent Test**: Subir a app, `GET` health e prometheus na porta de management, confirmar 401/404 de prometheus na porta 8080, e `docker compose` healthcheck verde.

---

### P1: Counters de reposição ⭐ MVP

**User Story**: As a operador, I want four domain counters on the restock path, so that I can see consumption, publish, send and failure without reading logs.

**Why P1**: Exemplos do overview e único valor de negócio desta etapa.

**Acceptance Criteria**:

1. WHEN `POST /api/v1/batches/consume` confirms with 204 THEN `stock_consumption_total` SHALL increase by 1.
2. WHEN a restock message is published after that commit THEN `restock_events_total` SHALL increase by 1.
3. WHEN notification status becomes `SENT` THEN `notifications_sent_total` SHALL increase by 1.
4. WHEN notification status becomes `FAILED` THEN `notifications_failed_total` SHALL increase by 1.
5. WHEN consume rolls back THEN the four domain counters SHALL NOT increase.
6. WHEN a message whose `eventId` is already `SENT` is redelivered THEN `notifications_sent_total` SHALL NOT increase.
7. WHILE the scheduled job `checkInventoryAndNotify()` runs THEN the four domain counters SHALL NOT increase for that run.

**Independent Test**: Consumir até 204, ler `/actuator/prometheus` antes e depois; repetir rollback, redelivery `SENT` e disparo do job.

---

### P1: Correlação de logs ⭐ MVP

**User Story**: As a operador, I want a request id on HTTP logs and the existing event id on queue logs, so that I can follow one consume from 204 to notification.

**Why P1**: Overview exige identificadores de correlação no mesmo processamento.

**Acceptance Criteria**:

1. WHEN the API handles an HTTP request THEN the response SHALL include header `X-Request-Id` with a UUID.
2. WHEN the API handles an HTTP request THEN log lines of that request SHALL include the same `requestId`.
3. WHEN a restock message is published after consume THEN the publish log SHALL include that `requestId` and the message `eventId`.
4. WHEN the queue consumer logs completion or failure THEN the log SHALL include `eventId`.

**Independent Test**: Consumir uma vez, copiar `X-Request-Id`, achar o mesmo valor no log de publicação ao lado do `eventId`, e achar o mesmo `eventId` no log do consumidor.

---

### P1: Prometheus e Grafana locais ⭐ MVP

**User Story**: As a desenvolvedor, I want Prometheus and Grafana on loopback with a provisioned dashboard, so that `docker compose up` shows JVM, HTTP and the four counters.

**Why P1**: Overview lista Prometheus e Grafana; decisão de dashboard provisionado.

**Acceptance Criteria**:

1. WHEN Docker Compose is up THEN Prometheus SHALL scrape the API management endpoint on the Compose network without a credential.
2. WHEN Docker Compose is up THEN Grafana SHALL listen on `127.0.0.1:3000`.
3. The Grafana container SHALL load a provisioned dashboard that includes the four domain counters and JVM or HTTP series.
4. IF Prometheus or Grafana is stopped THEN `POST /api/v1/batches/consume` SHALL still return 204 and SHALL leave committed stock unchanged.

**Independent Test**: `docker compose up`, abrir Grafana em `127.0.0.1:3000`, consumir estoque, ver os counters subirem; derrubar Prometheus e repetir o 204.

---

## Edge Cases

1. WHEN `createBatch` or `addStock` succeeds THEN the four domain counters SHALL NOT increase.
2. IF publish to RabbitMQ fails after commit THEN `stock_consumption_total` SHALL have increased by 1 and `restock_events_total` SHALL NOT have increased for that request.
3. IF the message is incomplete THEN the four domain counters SHALL NOT increase.
4. IF `ownerId` does not exist THEN `notifications_failed_total` SHALL increase by 1 and the other three domain counters SHALL follow the consume/publish rules only.
5. WHEN retry succeeds after a transient PDF or email failure THEN `notifications_sent_total` SHALL increase by 1 and `notifications_failed_total` SHALL NOT increase for that `eventId`.
6. IF recording a metric throws THEN the system SHALL still return 204 for a confirmed consume and SHALL still process the queue message.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| OBS-01 | P1: Saúde e scrape | Execute | Implemented |
| OBS-02 | P1: Saúde e scrape | Execute | Implemented |
| OBS-03 | P1: Saúde e scrape | Execute | Implemented |
| OBS-04 | P1: Saúde e scrape | Execute | Implemented |
| OBS-05 | P1: Saúde e scrape | Execute | Implemented |
| OBS-06 | P1: Saúde e scrape | Execute | Implemented |
| OBS-07 | P1: Counters de reposição | Execute | Implemented |
| OBS-08 | P1: Counters de reposição | Execute | Implemented |
| OBS-09 | P1: Counters de reposição | Execute | Implemented |
| OBS-10 | P1: Counters de reposição | Execute | Implemented |
| OBS-11 | P1: Counters de reposição | Execute | Implemented |
| OBS-12 | P1: Counters de reposição | Execute | Implemented |
| OBS-13 | P1: Counters de reposição | Execute | Implemented |
| OBS-14 | P1: Correlação de logs | Execute | Implemented |
| OBS-15 | P1: Correlação de logs | Execute | Implemented |
| OBS-16 | P1: Correlação de logs | Execute | Implemented |
| OBS-17 | P1: Correlação de logs | Execute | Implemented |
| OBS-18 | P1: Prometheus e Grafana locais | Execute | Implemented |
| OBS-19 | P1: Prometheus e Grafana locais | Execute | Implemented |
| OBS-20 | P1: Prometheus e Grafana locais | Execute | Implemented |
| OBS-21 | P1: Prometheus e Grafana locais | Execute | Implemented |
| OBS-22 | Edge: createBatch/addStock | Execute | Implemented |
| OBS-23 | Edge: falha de publicação | Execute | Implemented |
| OBS-24 | Edge: mensagem incompleta | Execute | Implemented |
| OBS-25 | Edge: owner ausente | Execute | Implemented |
| OBS-26 | Edge: retry depois sucesso | Execute | Implemented |
| OBS-27 | Edge: falha ao gravar métrica | Execute | Implemented |

**Coverage:** 27 total, 27 mapped to tasks, 0 unmapped

---

## Success Criteria

- [x] Health e Prometheus respondem na porta de management; prometheus não responde na porta da API de negócio.
- [x] Consumo 204 sobe `stock_consumption_total` e `restock_events_total`; `SENT` sobe `notifications_sent_total`; `FAILED` sobe `notifications_failed_total`.
- [x] Redelivery `SENT` e o job agendado não distorcem os quatro counters.
- [x] `X-Request-Id` no response; log de publicação junta `requestId` e `eventId`.
- [ ] `docker compose up` deixa Grafana em `127.0.0.1:3000` com dashboard; API continua 204 se Prometheus estiver parado.
