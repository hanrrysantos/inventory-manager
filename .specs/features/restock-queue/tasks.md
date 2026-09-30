# Fila de reposição Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/restock-queue/design.md`
**Status**: Draft

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `AGENTS.md` (JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo), `.github/workflows/backend-ci.yml` (`./mvnw clean test`).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Publicador e consumidor | unit | Todos os ramos de publicação, ack e falha. 1:1 com RABBIT-01, RABBIT-04, RABBIT-06, RABBIT-07, RABBIT-09, RABBIT-10, RABBIT-11, RABBIT-14 | `backend/src/test/java/**/*Test.java` | `cd backend && bash ./mvnw -Dtest=RestockEventPublisherTest,RestockQueueListenerTest test` |
| Alerta e consulta por proprietário | unit | Método com `User` e método do job. Lista vazia não envia e-mail | `backend/src/test/java/**/serviceTest/*Test.java` | `cd backend && bash ./mvnw -Dtest=StockAlertServiceTest,ProductServiceTest test` |
| Consumo, fila e HTTP | integration | `204`, rollback, broker fora, consumidor parado, job sem mensagem, falha de e-mail. RABBIT-02, RABBIT-03, RABBIT-05, RABBIT-12, RABBIT-13, RABBIT-15 | `backend/src/test/java/**/integration/*Test.java` | `cd backend && bash ./mvnw -Dtest=RestockNeededEventIntegrationTest,RestockPublishFailureIntegrationTest test` |
| Config, fila, compose, docs | none | Gate de build | - | `cd backend && bash ./mvnw -DskipTests package` |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tarefa com teste unitário | `cd backend && bash ./mvnw -Dtest=RestockEventPublisherTest,RestockQueueListenerTest,StockAlertServiceTest,ProductServiceTest,BatchServiceTest test` |
| Full | Tarefa com teste de integração | `cd backend && bash ./mvnw -Dtest=RestockNeededEventIntegrationTest,RestockPublishFailureIntegrationTest,RestockEventPublisherTest,RestockQueueListenerTest,StockAlertServiceTest,BatchServiceTest test` |
| Build | Config, compose, docs ou fim de fase | `cd backend && bash ./mvnw -DskipTests package` |

---

## Execution Plan

Fases em ordem. Tarefas dentro da fase em ordem.

### Phase 1: Fundação

```
T1 -> T3
```

### Phase 2: Publicação e consumo

```
T6 -> T7 -> T8
T5 -> T8
```

### Phase 3: Integração

T9 e T10 dependem da fase 2. Não há dependência entre elas.

### Phase 4: Ambiente local

```
T11 -> T13
T12 -> T13
```

---

## Task Breakdown

### Phase 1: Fundação

### T1: Adicionar dependências AMQP e Testcontainers

**What**: Inclui `spring-boot-starter-amqp` e `org.testcontainers:rabbitmq` na versão `1.21.4` já usada no PostgreSQL.
**Where**: `backend/pom.xml`
**Depends on**: None
**Reuses**: o parent Spring Boot `3.3.5` e a propriedade `testcontainers.version`
**Requirement**: RABBIT-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `spring-boot-starter-amqp` está no classpath principal, sem versão solta
- [x] `org.testcontainers:rabbitmq` está em `test` na versão `1.21.4`
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `build(rabbitmq): adiciona cliente amqp e testcontainers`

---

### T2: Configurar a conexão do broker

**What**: Expõe host, porta, usuário e senha do RabbitMQ por variável de ambiente, com padrão local.
**Where**: `backend/src/main/resources/application.yaml`
**Depends on**: None
**Reuses**: o padrão já usado por `DB_URL` e `RESEND_API_KEY`
**Requirement**: RABBIT-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `spring.rabbitmq.host` lê `RABBITMQ_HOST` com padrão `localhost`
- [x] `spring.rabbitmq.port` lê `RABBITMQ_PORT` com padrão `5672`
- [x] `spring.rabbitmq.username` lê `RABBITMQ_USERNAME` com padrão `guest`
- [x] `spring.rabbitmq.password` lê `RABBITMQ_PASSWORD` com padrão `guest`
- [x] Nenhum secret real entra no arquivo
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `chore(rabbitmq): configura conexao do broker`

---

### T3: Declarar a fila durável e o conversor JSON

**What**: Cria a fila durável `inventory.restock-needed`, o `Jackson2JsonMessageConverter` e um `RabbitTemplate` com mensagem persistente.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/inventory/config/RestockQueueConfig.java`
**Depends on**: T1
**Reuses**: Jackson do `spring-boot-starter-web`
**Requirement**: RABBIT-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] A fila é durável e o nome é `inventory.restock-needed`
- [x] O template usa JSON e `MessageDeliveryMode.PERSISTENT`
- [x] Não há exchange própria, DLQ nem retry
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `feat(inventory): declara fila duravel de reposicao`

---

### T4: Criar a mensagem da fila

**What**: Adiciona o record com `eventId`, `productId`, `occurredAt` e `ownerId`.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/inventory/event/RestockQueueMessage.java`
**Depends on**: None
**Reuses**: os campos de `RestockNeededEvent`
**Requirement**: RABBIT-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] O record expõe os quatro campos
- [x] `RestockNeededEvent` continua com três campos
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `feat(inventory): adiciona mensagem de reposicao`

---

### Phase 2: Publicação e consumo

### T5: Publicar a mensagem depois do commit

**What**: O publicador escuta `RestockNeededEvent` em `AFTER_COMMIT`, grava o `ownerId` autenticado e engole falha de broker ou de proprietário.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/inventory/event/RestockEventPublisher.java`
**Depends on**: T1, T3, T4
**Reuses**: `RestockNeededEvent`, `OwnerContext`, `RabbitTemplate`
**Requirement**: RABBIT-01, RABBIT-04, RABBIT-08, RABBIT-09, RABBIT-14

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] O método está em `AFTER_COMMIT` e envia uma `RestockQueueMessage` para `inventory.restock-needed`
- [x] A classe não importa `notification`
- [x] Falha de `RabbitTemplate` ou de `OwnerContext` é logada com `eventId` e não é relançada
- [x] Sem proprietário autenticado, nenhuma mensagem é enviada
- [x] Teste unitário cobre envio, falha de broker e ausência de proprietário
- [x] `BatchService` continua publicando `RestockNeededEvent` no commit, inclusive com quantidade zero ou negativa, e não publica em `InsufficientStockException`, `createBatch` ou `addStock`
- [x] Gate rápido passa: `cd backend && bash ./mvnw -Dtest=RestockEventPublisherTest,BatchServiceTest test`
- [x] Nenhum teste existente é apagado ou enfraquecido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(inventory): publica reposicao no rabbitmq apos commit`

---

### T6: Consultar estoque baixo por proprietário explícito

**What**: Adiciona a releitura que recebe `User` e não chama `OwnerContext`.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/product/service/ProductService.java`
**Depends on**: None
**Reuses**: `ProductRepository.findLowStockProducts`
**Requirement**: RABBIT-06

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `findLowStockProducts(User, Pageable)` usa o `User` recebido na consulta já existente
- [x] O método com só `Pageable` continua chamando `OwnerContext`
- [x] Teste unitário mostra que o método novo não chama `currentUser()`
- [x] Gate rápido passa: `cd backend && bash ./mvnw -Dtest=ProductServiceTest test`
- [x] Nenhum teste existente é apagado ou enfraquecido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(product): consulta estoque baixo por proprietario explicito`

---

### T7: Alertar um proprietário sem publicar na fila

**What**: Adiciona `checkInventoryAndNotify(User)` com o PDF e o e-mail atuais. O método sem argumento permanece o job.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/notification/service/StockAlertService.java`
**Depends on**: T6
**Reuses**: `PdfService`, `EmailSender`, a fórmula já gerada pelo PDF
**Requirement**: RABBIT-06, RABBIT-07, RABBIT-12, RABBIT-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] O método com `User` relê por esse proprietário, não envia e-mail se a lista vier vazia e envia o PDF atual quando houver item
- [x] O método sem argumento não recebe `RabbitTemplate` e não publica mensagem
- [x] Testes unitários cobrem envio, lista vazia e a ausência de publicação no método do job
- [x] Gate rápido passa: `cd backend && bash ./mvnw -Dtest=StockAlertServiceTest test`
- [x] Nenhum teste existente é apagado ou enfraquecido

**Tests**: unit
**Gate**: quick

**Commit**: `feat(notification): alerta de reposicao para um proprietario`

---

### T8: Consumir a fila e remover o listener in-process

**What**: O consumidor confirma a mensagem em sucesso e em falha. O listener `AFTER_COMMIT` que chama o alerta direto sai.
**Where**: `backend/src/main/java/br/com/hanrry/inventory/notification/event/RestockQueueListener.java`
**Depends on**: T4, T5, T7
**Reuses**: `StockAlertService.checkInventoryAndNotify(User)`, `UserRepository`
**Requirement**: RABBIT-06, RABBIT-07, RABBIT-10, RABBIT-11, RABBIT-16

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `@RabbitListener` escuta só `inventory.restock-needed`
- [x] Mensagem completa carrega o usuário e chama `checkInventoryAndNotify(User)`
- [x] Campo ausente, usuário inexistente, falha de PDF ou falha de Resend geram log com `eventId` quando ele existir, não enviam e-mail no caso inválido e não lançam exceção
- [x] `RestockNeededEventListener` e o teste dele não existem mais
- [x] Nenhum outro módulo declara `@RabbitListener` ou `RabbitTemplate.convertAndSend`
- [x] Gate rápido passa: `cd backend && bash ./mvnw -Dtest=RestockQueueListenerTest,StockAlertServiceTest,BatchServiceTest test`
- [x] Nenhum teste existente é apagado ou enfraquecido, fora a remoção do teste do listener antigo

**Tests**: unit
**Gate**: quick

**Commit**: `feat(notification): consome reposicao da fila`

---

### Phase 3: Integração

### T9: Provar commit, 204 e job contra RabbitMQ real

**What**: O teste de integração sobe PostgreSQL e RabbitMQ e prova o caminho feliz, o rollback, o `204` antes do e-mail e o job sem mensagem.
**Where**: `backend/src/test/java/br/com/hanrry/inventory/notification/integration/RestockNeededEventIntegrationTest.java`
**Depends on**: T5, T8
**Reuses**: o teste atual de commit, rollback e falha de e-mail
**Requirement**: RABBIT-01, RABBIT-02, RABBIT-03, RABBIT-05, RABBIT-12, RABBIT-13, RABBIT-14, RABBIT-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `RabbitMQContainer` sobe junto com o PostgreSQL já usado
- [x] Consumo confirmado responde `204` enquanto o `EmailSender` está bloqueado e deixa uma mensagem durável
- [x] Depois que o consumidor corre, o e-mail sai só se houver estoque baixo
- [x] `InsufficientStockException`, `createBatch` e `addStock` não deixam mensagem
- [x] Quantidade zero ou negativa que confirma ainda publica
- [x] Com o consumidor parado, a mensagem permanece na fila e o `204` já foi respondido
- [x] O job chama a verificação direta e a fila não ganha mensagem
- [x] Falha de Resend depois do consumo da mensagem mantém lote e log e não reenvia
- [x] Gate completo passa: `cd backend && bash ./mvnw -Dtest=RestockNeededEventIntegrationTest,BatchServiceTest test`
- [x] Nenhum teste existente é apagado ou enfraquecido

**Tests**: integration
**Gate**: full

**Commit**: `test(notification): cobre fila de reposicao com rabbitmq`

---

### T10: Provar falha de publicação com o broker fora

**What**: Com o broker inacessível, o consumo confirmado responde `204`, grava lote e log e não publica.
**Where**: `backend/src/test/java/br/com/hanrry/inventory/notification/integration/RestockPublishFailureIntegrationTest.java`
**Depends on**: T5
**Reuses**: PostgreSQL Testcontainers e o `MockMvc` do teste de reposição
**Requirement**: RABBIT-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] O contexto aponta `spring.rabbitmq.port` para uma porta fechada e sobe sem o container do RabbitMQ
- [x] `POST /api/v1/batches/consume` confirmado responde `204`
- [x] Lote e log permanecem gravados
- [x] Nenhuma mensagem é entregue a um consumidor
- [x] Gate completo passa: `cd backend && bash ./mvnw -Dtest=RestockPublishFailureIntegrationTest test`
- [x] Nenhum teste existente é apagado ou enfraquecido

**Tests**: integration
**Gate**: full

**Commit**: `test(notification): cobre falha de publicacao com broker fora`

---

### Phase 4: Ambiente local

### T11: Subir o RabbitMQ no Compose

**What**: Adiciona o serviço do broker na rede da API e aponta a API para ele.
**Where**: `backend/docker-compose.yml`
**Depends on**: T2
**Reuses**: a rede `inventory-network` e o `depends_on` da API
**Requirement**: RABBIT-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] O serviço usa a imagem oficial do RabbitMQ e não publica a porta de gerenciamento na internet além do bind local necessário para desenvolvimento
- [x] A API recebe `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` e `RABBITMQ_PASSWORD` apontando para esse serviço
- [x] A API só sobe depois do broker saudável
- [x] `docker compose -f backend/docker-compose.yml config --quiet` termina com código 0
- [x] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `build(compose): adiciona rabbitmq local`

---

### T12: Registrar as variáveis no exemplo de ambiente

**What**: Documenta as quatro variáveis do broker sem secret real.
**Where**: `backend/.env.example`
**Depends on**: T2
**Reuses**: o bloco já existente de `RESEND_API_KEY`
**Requirement**: RABBIT-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` e `RABBITMQ_PASSWORD` estão no exemplo
- [ ] Os valores são locais (`localhost`, `5672`, `guest`) e não são credenciais de produção
- [ ] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `docs(rabbitmq): exemplifica variaveis do broker`

---

### T13: Documentar o broker no README do backend

**What**: Inclui as variáveis do RabbitMQ na tabela de ambiente do README.
**Where**: `backend/README.md`
**Depends on**: T11, T12
**Reuses**: a tabela que já descreve `RESEND_API_KEY`
**Requirement**: RABBIT-01

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] A tabela explica as quatro variáveis e diz que o Compose preenche o host do serviço
- [ ] O texto não instrui a versionar `.env`
- [ ] Gate de build passa: `cd backend && bash ./mvnw -DskipTests package`

**Tests**: none
**Gate**: build

**Commit**: `docs(rabbitmq): documenta variaveis do broker`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 -> T3
Phase 2:  T6 -> T7 -> T8
          T5 -> T8
Phase 3:  T9
          T10
Phase 4:  T11 -> T13
          T12 -> T13
```

Execução sequencial. Um lote só começa depois do anterior. 13 tarefas passam de um lote: na Execute, oferecer subagentes antes de implementar.

---

## Task Granularity Check

| Task | Scope | Status |
| --- | --- | --- |
| T1: Dependências | 1 arquivo | ✅ Granular |
| T2: Conexão | 1 arquivo | ✅ Granular |
| T3: Fila e conversor | 1 classe | ✅ Granular |
| T4: Mensagem | 1 record | ✅ Granular |
| T5: Publicador | 1 classe e o teste dela | ✅ Granular |
| T6: Consulta por proprietário | 1 método | ✅ Granular |
| T7: Alerta por proprietário | 1 método | ✅ Granular |
| T8: Consumidor | 1 classe. A remoção do listener antigo é o corte necessário para não alertar duas vezes | ✅ Granular |
| T9: Integração feliz | 1 classe de teste | ✅ Granular |
| T10: Broker fora | 1 classe de teste | ✅ Granular |
| T11: Compose | 1 arquivo | ✅ Granular |
| T12: Exemplo de ambiente | 1 arquivo | ✅ Granular |
| T13: README | 1 arquivo | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| --- | --- | --- | --- |
| T1 | None | sem seta de entrada | ✅ Match |
| T2 | None | sem seta de entrada | ✅ Match |
| T3 | T1 | T1 -> T3 | ✅ Match |
| T4 | None | sem seta de entrada | ✅ Match |
| T5 | T1, T3, T4 | sem seta intrafase. As três dependências são da fase 1 | ✅ Match |
| T6 | None | sem seta de entrada | ✅ Match |
| T7 | T6 | T6 -> T7 | ✅ Match |
| T8 | T4, T5, T7 | T5 -> T8 e T7 -> T8. T4 é da fase 1 | ✅ Match |
| T9 | T5, T8 | sem seta intrafase. As duas dependências são da fase 2 | ✅ Match |
| T10 | T5 | sem seta intrafase. T5 é da fase 2 | ✅ Match |
| T11 | T2 | sem seta intrafase. T2 é da fase 1 | ✅ Match |
| T12 | T2 | sem seta intrafase. T2 é da fase 1 | ✅ Match |
| T13 | T11, T12 | T11 -> T13 e T12 -> T13 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| --- | --- | --- | --- | --- |
| T1 | Config | none | none | ✅ OK |
| T2 | Config | none | none | ✅ OK |
| T3 | Config | none | none | ✅ OK |
| T4 | Config | none | none | ✅ OK |
| T5 | Publicador | unit | unit | ✅ OK |
| T6 | Consulta por proprietário | unit | unit | ✅ OK |
| T7 | Alerta | unit | unit | ✅ OK |
| T8 | Consumidor | unit | unit | ✅ OK |
| T9 | Integração | integration | integration | ✅ OK |
| T10 | Integração | integration | integration | ✅ OK |
| T11 | Config | none | none | ✅ OK |
| T12 | Config | none | none | ✅ OK |
| T13 | Config | none | none | ✅ OK |
