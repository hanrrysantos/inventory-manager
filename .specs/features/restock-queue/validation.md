# Fila de reposição Validation

**Date**: 2026-09-29
**Spec**: `.specs/features/restock-queue/spec.md`
**Diff range**: `406d7da..145bc95` (`git diff main...HEAD` em `feat/restock-queue`)
**Verifier**: independent sub-agent (author ≠ verifier)

Veredito: PASS. Os 16 requisitos têm evidência que corresponde à spec, o Full gate passou com 43 testes e os 12 mutantes foram mortos. Falha de publicação, PDF ou e-mail não desfaz estoque nem muda o `204`.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | `spring-boot-starter-amqp` sem versão, `org.testcontainers:rabbitmq` em `test` via `${testcontainers.version}` |
| T2 | ✅ Done | `backend/src/main/resources/application.yaml:37-41`, padrões locais, sem secret |
| T3 | ✅ Done | `RestockQueueConfig.java:19` fila durável, `:32` `PERSISTENT`, sem exchange, DLQ ou retry |
| T4 | ✅ Done | Record com 4 campos. `RestockNeededEvent` continua com 3 |
| T5 | ✅ Done | `RestockEventPublisher.java:21` `AFTER_COMMIT`, `:31-37` captura e loga `eventId` |
| T6 | ✅ Done | `ProductService.findLowStockProducts(User, Pageable)` |
| T7 | ✅ Done | `StockAlertService.java:30-32` |
| T8 | ✅ Done | `RestockQueueListener`. `RestockNeededEventListener` e o teste dele foram removidos |
| T9 | ✅ Done | `RestockNeededEventIntegrationTest` com `RabbitMQContainer` |
| T10 | ✅ Done | `RestockPublishFailureIntegrationTest` com porta fechada |
| T11 | ✅ Done | Tem extras de escopo, ver Code Quality |
| T12 | ✅ Done | `backend/.env.example` com valores locais |
| T13 | ✅ Done | `backend/README.md` |

Todas as caixas Done-when de T1 a T13 estão marcadas `[x]` em `tasks.md`. Nenhuma tarefa está bloqueada ou parcial.

---

## Spec-Anchored Acceptance Criteria

Caminhos de teste abreviados. Prefixo: `backend/src/test/java/br/com/hanrry/inventory/`.

- `RNEIT` = `notification/integration/RestockNeededEventIntegrationTest.java`
- `RPFIT` = `notification/integration/RestockPublishFailureIntegrationTest.java`
- `REPT` = `inventory/event/RestockEventPublisherTest.java`
- `RQLT` = `notification/event/RestockQueueListenerTest.java`
- `SAST` = `notification/serviceTest/StockAlertServiceTest.java`

| ID | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| -- | ------------------------- | -------------------- | ----------------------- | ------ |
| RABBIT-01 | WHEN `consumeStock` confirma THEN publica exatamente uma mensagem com os 4 campos | 1 mensagem com `eventId`, `productId`, `occurredAt`, `ownerId` | `backend/src/test/java/br/com/hanrry/inventory/notification/integration/RestockNeededEventIntegrationTest.java:198` - `assertEquals(1, messages.size())`. `RNEIT:204-208` - `assertNotNull(body.eventId())`, `assertEquals(PRODUCT_ID, body.productId())`, `occurredAt` entre `before` e `after`, `assertEquals(ownerId(), body.ownerId())`. `backend/src/test/java/br/com/hanrry/inventory/inventory/event/RestockEventPublisherTest.java:73` - `assertEquals(new RestockQueueMessage(eventId, 13L, occurredAt, 7L), message.getValue())` | ✅ PASS |
| RABBIT-02 | WHEN `InsufficientStockException` THEN não publica | Nenhuma mensagem, rollback | `RNEIT:251-254` - `assertThrows(InsufficientStockException.class, ...)`. `RNEIT:257` - `assertEquals(5L, persisted.getQuantity())`. `RNEIT:259` - `assertNull(rabbitTemplate.receive(RESTOCK_NEEDED_QUEUE, NO_MESSAGE_WAIT_MS))` | ✅ PASS |
| RABBIT-03 | WHEN `createBatch` ou `addStock` THEN não publica | Nenhuma mensagem | `RNEIT:272` - `assertEquals(15L, ...getQuantity())`. `RNEIT:273` - `assertNull(rabbitTemplate.receive(...))` | ✅ PASS |
| RABBIT-04 | Publicar somente depois do commit | Fase `AFTER_COMMIT` | `REPT:56` - `assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase())`. Rollback ponta a ponta em `RNEIT:259` (mata M6) | ✅ PASS |
| RABBIT-05 | WHEN commit THEN `204` sem aguardar PDF e e-mail | `204 No Content` com e-mail ainda bloqueado | `RNEIT:168` - `status().isNoContent()`. `RNEIT:170` - `assertFalse(emailFinished.get())`. `RNEIT:172` - `assertEquals(9L, persisted.getQuantity())` | ✅ PASS |
| RABBIT-06 | WHEN mensagem completa THEN relê estoque baixo, condição `<=`, fórmula, PDF e e-mail atuais | Releitura por dono, PDF e e-mail atuais | `RQLT:70` - `verify(stockAlertService).checkInventoryAndNotify(owner)`. `SAST:117-121` - `verify(pdfService).generateLowStockReport(lowStockProducts)` e `verify(emailSender).sendLowStockAlert(List.of("Notebook"), pdfReport)`. `RNEIT:182-183` - `productNames.getValue().contains(productName())`, `pdfReport.getValue().length > 0`. `backend/src/test/java/br/com/hanrry/inventory/product/serviceTest/ProductServiceTest.java:406` - `verify(ownerContext, never()).currentUser()`. Condição e fórmula continuam no código existente: `backend/src/main/java/br/com/hanrry/inventory/product/repository/ProductRepository.java:39` (`<= p.minStock`), `backend/src/main/java/br/com/hanrry/inventory/notification/document/PdfService.java:45` (`minStock() - totalQuantity()`), asserida em `backend/src/test/java/br/com/hanrry/inventory/notification/documentTest/PdfServiceTest.java:58` - `assertTrue(normalizedText.contains("7"))` | ✅ PASS (ver Observação 4) |
| RABBIT-07 | WHEN releitura sem estoque baixo THEN não envia e-mail | Nenhum e-mail | `RNEIT:241` - `verify(emailSender, never()).sendLowStockAlert(anyList(), any())`. `SAST:135-136` - `verifyNoInteractions(pdfService)`, `verifyNoInteractions(emailSender)` | ✅ PASS |
| RABBIT-08 | `inventory` não depende de `notification` | Nenhum import de `notification` em `inventory` | Estático: `backend/src/main/java/br/com/hanrry/inventory/inventory/event/RestockEventPublisher.java:3-11` importa só `shared.security`, Lombok, AMQP e Spring. `rg "import br.com.hanrry.inventory.notification" backend/src/main/java/br/com/hanrry/inventory/inventory` retorna zero linhas | ✅ PASS (estático, ver Observação 3) |
| RABBIT-09 | IF falha de publicação THEN lote e log mantidos, `204`, log com `eventId` | `204`, estoque 9, 1 log `OUTPUT`, log com UUID | `backend/src/test/java/br/com/hanrry/inventory/notification/integration/RestockPublishFailureIntegrationTest.java:132` - `status().isNoContent()`. `RPFIT:135` - `assertEquals(9L, persisted.getQuantity())`. `RPFIT:136-140` - 1 log `OUTPUT`. `RPFIT:141` - `PUBLISH_FAILURE_LOG.matcher(output.getAll()).find()`. `REPT:85` - `assertDoesNotThrow(() -> publisher.on(event))`. `REPT:87` - `contains("eventId=" + eventId)` | ✅ PASS |
| RABBIT-10 | IF PDF ou Resend falha no consumidor THEN estoque mantido, log com `eventId`, mensagem confirmada | Sem exceção, fila vazia, 1 envio | `RQLT:117-119` (PDF) e `RQLT:131-133` (Resend) - `assertDoesNotThrow(...)`, `contains("eventId=" + EVENT_ID)`. `RNEIT:313` - `assertEquals(0, readyMessages())`. `RNEIT:314` - `verify(emailSender, times(1))`. `RNEIT:316-317` - estoque 9 e 1 log | ✅ PASS |
| RABBIT-11 | IF mensagem sem um dos 4 campos THEN log, confirma, sem e-mail | Sem exceção, sem alerta, log de erro | `RQLT:89` - `assertDoesNotThrow(() -> listener.on(message))`. `RQLT:91` - `verifyNoInteractions(stockAlertService)`. `RQLT:92` - `contains("ERROR")`. `RQLT:94` - `contains("eventId=" + message.eventId())`. Parametrizado para os 4 campos | ✅ PASS |
| RABBIT-12 | Job chama `checkInventoryAndNotify` no mesmo processo | Método do job continua `@Scheduled` e envia e-mail direto | `SAST:141` - `assertNotNull(...getMethod("checkInventoryAndNotify").getAnnotation(Scheduled.class))`. `RNEIT:296` - `verify(emailSender).sendLowStockAlert(anyList(), any())` com o consumidor parado | ✅ PASS |
| RABBIT-13 | Job não publica no RabbitMQ | Fila vazia | `RNEIT:297` - `assertNull(rabbitTemplate.receive(...))`. `SAST:147` - `noneMatch(type -> AmqpTemplate.class.isAssignableFrom(type) || ApplicationEventPublisher.class.isAssignableFrom(type))` | ✅ PASS |

**Status**: ✅ Todos os critérios cobertos. Nenhum spec-precision gap.

---

## Discrimination Sensor

Worktree temporária em `/tmp/restock-sensor`, criada no HEAD `145bc95` com `git worktree add --detach`. Um mutante por execução, com `git checkout -- .` na worktree temporária entre elas. O `git status --porcelain` da árvore real estava vazio antes e depois. A worktree foi removida com `git worktree remove --force`.

| Mutation | File:line | Description | Killed? | Killed by |
| -------- | --------- | ----------- | ------- | --------- |
| M1 | `backend/src/main/java/br/com/hanrry/inventory/inventory/event/RestockEventPublisher.java:21` | `AFTER_COMMIT` → `AFTER_COMPLETION` (publica também em rollback) | ✅ Killed | `REPT.shouldPublishOnlyAfterCommit` |
| M2 | `RestockEventPublisher.java:21` | `@TransactionalEventListener` → `@EventListener` síncrono (publica antes do commit) | ✅ Killed | `REPT.shouldPublishOnlyAfterCommit`. `RNEIT` passou (9/9) |
| M3 | `RestockEventPublisher.java:37` | Relança a falha de broker no `catch` | ✅ Killed | `REPT.shouldLogEventIdAndNotRethrowWhenBrokerFails`, `REPT.shouldNotSendAndShouldLogEventIdWhenOwnerIsNotAuthenticated`. `RPFIT` passou (ver Observação 1) |
| M4 | `RestockEventPublisher.java:29` | `ownerId` recebe `event.productId()` | ✅ Killed | `REPT` 1 falha. `RNEIT` 5 falhas |
| M5 | `backend/src/main/java/br/com/hanrry/inventory/inventory/service/BatchService.java:126-127` | Publica `RestockNeededEvent` antes de lançar `InsufficientStockException` | ✅ Killed | `BatchServiceTest.shouldNotConsumeStockFromExpiredBatch` (`BatchServiceTest.java:354`). `RNEIT` passou porque o `AFTER_COMMIT` segura (ver Observação 2) |
| M6 | `RestockEventPublisher.java:21` + `BatchService.java:126-127` | M1 e M5 juntos. Rollback com mensagem na fila, ponta a ponta | ✅ Killed | `RNEIT.shouldNotPublishAndShouldRollbackWhenStockIsInsufficient` |
| M7 | `backend/src/main/java/br/com/hanrry/inventory/notification/event/RestockQueueListener.java:38-44` | Relança falha de PDF ou e-mail (a mensagem deixa de ser confirmada) | ✅ Killed | `RQLT` 2 falhas. `RNEIT.shouldKeepStockAndNotResendWhenResendFailsAfterConsumingMessage` |
| M8 | `RestockQueueListener.java:31` | Chama o método do job `checkInventoryAndNotify()` em vez do método por dono | ✅ Killed | `RQLT.shouldNotifyLowStockOfMessageOwner`. `RNEIT` 4 falhas |
| M9 | `backend/src/main/java/br/com/hanrry/inventory/notification/service/StockAlertService.java:31` | Releitura com `null` em vez de `owner` | ✅ Killed | `SAST` 2 erros |
| M10 | `StockAlertService.java:23,27` | Job injeta `RabbitTemplate` e publica na fila | ✅ Killed | `SAST.shouldKeepScheduledJobInProcessWithoutQueuePublisher`, `RNEIT.shouldRunScheduledCheckInProcessWithoutPublishing` |
| M11 | `RestockQueueListener.java:24` | Remove a checagem de `ownerId == null` | ✅ Killed | `RQLT.shouldLogAndNotNotifyWhenMessageIsIncomplete` |
| M12 | `backend/src/main/java/br/com/hanrry/inventory/inventory/config/RestockQueueConfig.java:32` | `PERSISTENT` → `NON_PERSISTENT` | ✅ Killed | `RNEIT.shouldPublishOneDurablePersistentMessageAfterConsumeCommit` |

**Sensor depth**: P0-full manual (12 mutações de comportamento ≥ 5). Não há pitest no projeto.
**Result**: 12/12 killed - PASS ✅

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ Publicador, consumidor e config seguem o design. `StockAlertService` extrai só o `notifyLowStock` privado |
| Surgical changes | ✅ `ProductService` ganha um overload e o método antigo delega a ele. Nenhum código vizinho foi reescrito |
| No scope creep | ⚠️ Menor, não bloqueia. Ver T11 abaixo |
| Matches patterns | ✅ Lombok `@RequiredArgsConstructor` e `@Slf4j`, log em português com `eventId`, testes nos pacotes existentes |
| Spec-anchored outcome check (asserted values match spec) | ✅ |
| Per-layer Coverage Expectation met (unit 1:1 no publicador e no consumidor; integração com 204, rollback, broker fora, consumidor parado, job e falha de e-mail) | ✅ |
| Every test maps to a spec requirement - no unclaimed tests | ✅ Testes de dono inexistente e do overload só com `Pageable` mapeiam para os Done-when de T8 e T6 |
| Documented guidelines followed: `AGENTS.md` (JUnit 5, Mockito, MockMvc, Testcontainers, credenciais por variável de ambiente, Conventional Commits sem coautoria) | ✅ |

**T11, extras do Compose.** A tarefa pedia a imagem oficial, bind local da porta de gerenciamento, as variáveis na API e broker saudável antes da API. Os quatro foram entregues (`backend/docker-compose.yml:22`, `:29`, `:58-61`, `:65-66`). `docker compose -f backend/docker-compose.yml config --quiet` retornou 0. Extras:

- `hostname: rabbitmq` e volume nomeado `rabbitmq-data`: não pedidos. Andam juntos: o RabbitMQ grava dados por nome de nó, e o hostname fixo faz o volume ser reaproveitado. Mensagens duráveis sobrevivem a `down/up`, o que combina com a premissa de durabilidade da spec. Extra defensável, fora do texto da tarefa.
- AMQP `127.0.0.1:5672:5672`: não pedido. Atende o caminho "fora do Compose" do README (`backend/README.md:161`). Só loopback, sem exposição de rede.
- `RABBITMQ_DEFAULT_USER/PASS` com padrão `guest`: faz a credencial do broker bater com a da API quando o usuário troca as variáveis. O padrão é a credencial pública da imagem. Nenhum secret versionado.

Julgamento: pequeno excesso de escopo, baixo risco. Não é motivo de FAIL. O ideal era registrar esses extras em `tasks.md` ou num commit próprio.

**Observações (não bloqueiam):**

1. **Camada que protege o `204` em falha de broker.** Com M3 (relançar no publicador), `RPFIT` continuou passando. O Spring 6 despacha listeners `AFTER_COMMIT` no `afterCompletion` e só loga a exceção, então o `204` sobrevive mesmo sem o `catch`. Quem discrimina é `REPT:85` (`assertDoesNotThrow`). A cobertura está completa, só está na camada unitária.
2. **Asserção fraca preexistente em `BatchServiceTest`.** `BatchServiceTest.java:458-459` usa `verify(eventPublisher, never()).publishEvent(any())`. O `any()` resolve para o overload `publishEvent(ApplicationEvent)`, e `RestockNeededEvent` passa por `publishEvent(Object)`. Com M5, esse teste passou. O mutante morreu em `BatchServiceTest.java:354`. O arquivo está fora do diff. Correção sugerida à parte: `publishEvent(any(Object.class))` ou `any(RestockNeededEvent.class)`.
3. **RABBIT-08 e RABBIT-16 têm só evidência estática.** Nenhum teste automatizado impede outro módulo de importar `notification` ou de usar `RabbitTemplate`. O projeto não tem ArchUnit, e adicioná-lo seria dependência nova, fora do escopo. A matriz de cobertura de `tasks.md` não pedia teste para esses dois.
4. **Condição e fórmula de RABBIT-06 dependem de testes preexistentes.** `PdfServiceTest.java:58` checa `contains("7")`, que é frouxo. A igualdade `totalQuantity == minStock` não roda contra o PostgreSQL nesta feature. A consulta e o PDF não mudaram, e o consumidor reusa os dois.
5. `RestockQueueConfig.java:28-36` substitui o `RabbitTemplate` autoconfigurado do Boot, e as propriedades `spring.rabbitmq.template.*` deixam de valer. Está conforme o design.
6. As asserções negativas esperam 2 s em `receive`. Isso deixa a suíte mais lenta, mas não gera flakiness.

---

## Edge Cases

| ID | Edge case | `file:line` + assertion | Result |
| -- | --------- | ----------------------- | ------ |
| RABBIT-14 | Quantidade zero ou negativa que confirma publica | `RNEIT:283` - `assertEquals(2, messages.size())`. `RNEIT:285` - `assertEquals(PRODUCT_ID, toQueueMessage(message).productId())` | ✅ PASS |
| RABBIT-15 | Consumidor parado: mensagem fica na fila e `204` já respondido | `RNEIT:217` - `status().isNoContent()`. `RNEIT:221` - `awaitReadyMessages(1)`. `RNEIT:222` - `verify(emailSender, never())`. `RNEIT:227-229` - entrega após restart e `assertEquals(0, readyMessages())`. Durabilidade: `RNEIT:196` - `assertEquals("true", queueDurability())`. `RNEIT:199-202` - `MessageDeliveryMode.PERSISTENT` | ✅ PASS |
| RABBIT-16 | RabbitMQ só entre o publicador e o consumidor de notificação | `RQLT:59` - `assertArrayEquals(new String[]{"inventory.restock-needed"}, rabbitListener.queues())`. Estático: `rg "RabbitListener\|RabbitTemplate\|AmqpTemplate\|convertAndSend" backend/src/main` só acha `RestockQueueListener.java:21`, `RestockQueueConfig.java:28-29` e `RestockEventPublisher.java:18,25` | ✅ PASS (estático, ver Observação 3) |

- [x] RABBIT-14: tratado
- [x] RABBIT-15: tratado
- [x] RABBIT-16: tratado, sem guarda automatizada contra uso futuro em outro módulo

---

## Gate Check

- **Gate command**: `cd backend && bash ./mvnw -Dtest=RestockNeededEventIntegrationTest,RestockPublishFailureIntegrationTest,RestockEventPublisherTest,RestockQueueListenerTest,StockAlertServiceTest,BatchServiceTest test` (Full gate. O Build gate pula testes)
- **Result**: 43 passed, 0 failed, 0 skipped, exit 0
  - BatchServiceTest 15, RestockEventPublisherTest 4, RestockQueueListenerTest 9, RestockNeededEventIntegrationTest 9, RestockPublishFailureIntegrationTest 1, StockAlertServiceTest 5
- **Extra**: `ProductServiceTest` (Quick gate, citado em RABBIT-06): 13 passed, 0 failed
- **Test count before feature** (mesmas classes no `main`): 21 no Full gate (BatchServiceTest 15, RestockNeededEventIntegrationTest 4, StockAlertServiceTest 2). Todas as classes tocadas: 34 (as 21 + ProductServiceTest 11 + RestockNeededEventListenerTest 2)
- **Test count after feature**: 43 no Full gate. Todas as classes tocadas: 56
- **Delta**: +22 no Full gate. Nas classes tocadas: +24 novos e −2 removidos
- **Removal**: `RestockNeededEventListenerTest` (2 testes) foi removido por T8, de forma explícita. Delegação e captura de exceção foram para `RQLT:70` e `RQLT:117-133`. Os 4 cenários antigos da integração (sem e-mail em rollback, e-mail após commit, estoque mantido quando o e-mail falha, `204` quando o e-mail falha) continuam em `RNEIT:245`, `RNEIT:154`, `RNEIT:301` e `RNEIT:309`, agora pela fila. Nenhuma asserção enfraquecida
- **Skipped tests**: none
- **Failures**: none

---

## Fix Plans (if issues found)

Nenhum fix obrigatório. Melhoria opcional, fora do diff: trocar `any()` por `any(Object.class)` em `BatchServiceTest.java:459` (Observação 2).

---

## Requirement Traceability Update

`spec.md` não foi editado nesta validação. Atualização recomendada:

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| RABBIT-01 a RABBIT-16 | Done | ✅ Verified |

---

## Summary

**Result**: PASS

**Overall**: ✅ Ready

**Spec-anchored check**: 16/16 critérios batem com a saída da spec. 0 spec-precision gaps
**Sensor**: 12/12 mutations killed
**Gate**: 43 passed, 0 failed

**What works**: publicação única e durável depois do commit, com os 4 campos. Nada é publicado em rollback, `createBatch`, `addStock` ou no job. `204` volta antes do e-mail. Falha de broker, PDF ou Resend deixa lote e log gravados e confirma a mensagem. Mensagem incompleta é descartada com log. Releitura limitada ao dono da mensagem.

**Issues found**: nenhum bloqueante. Há extras menores de escopo em T11 e seis observações não bloqueantes (acima).

**Next steps**: marcar RABBIT-01 a RABBIT-16 como Verified em `spec.md`, se autorizado. Opcionalmente, corrigir `BatchServiceTest.java:459` numa mudança separada.
