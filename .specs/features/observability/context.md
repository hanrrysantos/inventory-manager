# Observabilidade Context

**Gathered:** 2026-10-01
**Spec:** `.specs/features/observability/spec.md`
**Status:** Ready for design
**Mode:** Guided

---

## Feature Boundary

Expor saúde e métricas da API (Actuator + Micrometer), raspar com Prometheus, mostrar em Grafana local, contar quatro eventos do fluxo de reposição, e correlacionar logs HTTP com `requestId` e logs da fila com `eventId`.

Não altera consumo FEFO, `204`, retry, DLQ, PDF, e-mail nem JWT. Tracing distribuído e deploy público ficam fora.

---

## Implementation Decisions

### Métricas de domínio

- Os quatro counters do overview entram no P1: `stock_consumption_total`, `restock_events_total`, `notifications_sent_total`, `notifications_failed_total`.
- Cada um incrementa em 1 no evento correspondente, não pela quantidade de itens consumidos.
- O job `@Scheduled` não incrementa esses quatro.

### Exposição e segurança do scrape

- Prometheus raspa `/actuator/prometheus` sem credencial, só na rede interna do Compose.
- Actuator fica em porta de management separada da API de negócio. Essa porta não é publicada na internet; no host, se existir mapeamento, é só `127.0.0.1`.
- `/actuator/health` e `/actuator/prometheus` entram na exposição. `env` e `heapdump` não.
- JWT das rotas `/api/v1/**` permanece.

**Local vs depois:** Grafana e Prometheus desta etapa são o Compose local (`127.0.0.1`). As métricas existem em qualquer execução da API. No deploy futuro o mesmo padrão vale (Prometheus na rede privada, Actuator fora da internet). Esse wiring de produção não entra aqui.

### Correlação de logs

- Todo request HTTP gera um `requestId` (UUID) no servidor, devolve `X-Request-Id` e inclui o id nos logs daquele request.
- Publicação na fila loga `requestId` e `eventId` juntos.
- Consumidor da fila continua logando `eventId`. Sem OpenTelemetry.

### Grafana local

- Compose sobe Prometheus e Grafana.
- Grafana em `127.0.0.1:3000` com um dashboard provisionado (JVM/HTTP + os quatro counters).
- Sem regras de alerta e sem Grafana Cloud.

### Agent's Discretion

- Nome Micrometer interno (ponto vs underscore) desde que a série Prometheus bata com os quatro nomes da spec.
- Layout interno do dashboard, desde que os quatro counters e séries JVM/HTTP apareçam.
- Porta de management concreta (ex.: 8081), desde que fique fora da porta HTTP de negócio.

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma área recusada. As quatro foram decididas nesta sessão.

---

## Specific References

- `docs/architecture/overview.md` seção Observabilidade.
- Decisão do usuário em 2026-10-01: quatro counters; scrape recomendado; correlação recomendada; Grafana com dashboard provisionado.

---

## Deferred Ideas

- Tracing OpenTelemetry / Jaeger.
- Alertas Grafana.
- Wiring Prometheus/Grafana no ambiente público de demonstração (etapa Deploy).
- Métricas de catálogo, auth ou paginação.
