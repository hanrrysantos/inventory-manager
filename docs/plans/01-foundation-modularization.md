# Plano 1: Fundação e Modularização por Domínio

> **Para agentes de implementação:** execute este plano tarefa por tarefa, usando commits independentes e validação após cada grupo relevante.

**Objetivo:** reorganizar o Inventory Manager de package-by-layer para package-by-feature/domain, preservando contratos HTTP, regras de negócio e comportamento atual.

**Arquitetura:** a aplicação continuará sendo um Modular Monolith em Java 21 e Spring Boot 3. Os módulos-alvo serão `auth`, `user`, `product`, `inventory`, `notification` e `shared`. A migração será incremental, sem reescrita geral e sem introduzir ainda RabbitMQ, locking pessimista, retry, DLQ ou observabilidade.

**Stack:** Java 21, Spring Boot 3, Maven Wrapper, Spring Data JPA, PostgreSQL, Flyway, JUnit 5, Mockito, MockMvc, MapStruct, JaCoCo e PostgreSQL Testcontainers.

**Referências:** `AGENTS.md` e `docs/architecture.md`.

## Restrições Globais

- O código existente é a fonte de verdade sobre o estado atual; a arquitetura-alvo não deve ser tratada como implementada.
- Preservar endpoints, payloads, status HTTP e regras de negócio existentes.
- Não corrigir nesta etapa regras identificadas como potencialmente inadequadas; primeiro registrá-las em testes de caracterização.
- Não adicionar RabbitMQ, `PESSIMISTIC_WRITE`, retry, DLQ, Actuator, Micrometer, Prometheus, Grafana, Docker Compose ou novas funcionalidades.
- Usar PostgreSQL Testcontainers somente para persistência real, transações e Flyway; manter Mockito para testes unitários.
- Não editar migrations Flyway já aplicadas e não expor secrets.
- Preservar nomes de classes, métodos e APIs internas sempre que possível; `PdfService` continuará com esse nome neste plano.
- Executar testes após cada grupo relevante e não considerar a etapa concluída com findings bloqueantes.
- Characterization tests que apenas registram comportamento já existente podem nascer GREEN; não alterar a implementação artificialmente para produzir RED. Usar RED → GREEN → REFACTOR obrigatoriamente quando houver comportamento novo ou correção explicitamente autorizada.

## Mapa de Arquivos e Destinos

| Área atual | Destino deste plano |
| --- | --- |
| `controller/AuthController.java`, `security/*` | `auth/controller`, `auth/security` |
| `service/UserService.java`, `entity/User.java`, `repository/UserRepository.java` | `user` |
| `controller/ProductController.java`, `service/ProductService.java`, `entity/Product.java`, `repository/ProductRepository.java`, mappers e DTOs de produto | `product` |
| `controller/BatchController.java`, `service/BatchService.java`, `service/InventoryLogService.java`, entidades e repository de lote/log | `inventory` |
| `service/StockAlertService.java`, `service/PdfService.java`, `service/EmailService.java` | `notification` |
| categorias, exceções comuns, documentação OpenAPI e configuração compartilhada | `product`, `auth`, `shared` conforme responsabilidade |
| `src/test/java/.../controllerTest`, `serviceTest`, `mapperTest` | testes reorganizados para refletir o domínio, sem alterar o escopo comportamental |

## Plano de Execução

### Tarefa 1: Registrar e validar o baseline atual

**Objetivo:** obter uma fotografia verificável do estado antes de qualquer alteração.

**Arquivos atuais envolvidos:** `pom.xml`, `.github/workflows/maven.yml`, todos os testes em `src/test/java` e o relatório gerado em `target/site/jacoco/`.

**Destino arquitetural:** nenhum; esta tarefa somente registra o ponto de partida.

**Testes necessários:** todos os 81 testes existentes, incluindo services, controllers, mappers, PDF e e-mail.

**Comandos de validação:**

```bash
./mvnw clean test
git status --short
```

**Critério de conclusão:** `./mvnw clean test` termina com sucesso, o relatório JaCoCo é gerado e o estado do Git é registrado sem misturar alterações não relacionadas.

**Registro:** guardar o resultado dos testes e o estado do repositório na revisão da tarefa. Não criar commit se nenhum arquivo for alterado.

### Tarefa 2: Adicionar characterization tests unitários faltantes

**Objetivo:** proteger contratos e regras atuais antes de mover classes, inclusive comportamentos que serão reavaliados em planos posteriores.

**Arquivos atuais envolvidos:** `BatchServiceTest.java`, `ProductServiceTest.java`, `StockAlertServiceTest.java`, `EmailServiceTest.java`, `PdfServiceTest.java`, `AuthControllerTest.java`, `BatchControllerTest.java`, `UserServiceTest.java` e os componentes correspondentes em `src/main/java` somente como referência.

**Destino arquitetural:** testes serão associados posteriormente a `auth`, `user`, `product`, `inventory` e `notification`; nesta tarefa permanecem nos packages atuais.

**Testes necessários:** adicionar apenas characterization tests relevantes para proteger a refatoração estrutural. Testes que somente registram comportamento existente podem nascer GREEN; não alterar a implementação para forçar uma etapa RED. Usar RED → GREEN → REFACTOR somente se esta tarefa receber autorização explícita para comportamento novo ou correção.

- rollback esperado quando o consumo é insuficiente em uma transação real será coberto na Tarefa 3;
- regra atual `totalQuantity <= minStock`, incluindo igualdade;
- soma atual de lotes, inclusive lotes vencidos, para registrar o comportamento existente;
- chamada do alerta após consumo bem-sucedido e ausência dela quando ocorre insuficiência;
- formato, destinatário fixo, assunto e anexo do e-mail;
- conteúdo e cálculo `minStock - totalQuantity` do PDF;
- endpoints e status atuais de autenticação, criação de lote, consumo e consulta de estoque baixo.

**Comandos de validação:**

```bash
./mvnw -Dtest=BatchServiceTest,ProductServiceTest,StockAlertServiceTest,EmailServiceTest,PdfServiceTest test
./mvnw -Dtest=AuthControllerTest,BatchControllerTest,ProductControllerTest test
```

**Critério de conclusão:** cada comportamento relevante possui teste reproduzível, os testes passam e nenhum teste altera a implementação para corrigir regra de negócio.

**Commit:** `adicionando testes de caracterizacao`

### Tarefa 3: Proteger persistência, transações e Flyway com PostgreSQL

**Objetivo:** cobrir com banco real os riscos que Mockito não consegue validar.

**Arquivos atuais envolvidos:** `pom.xml`, `src/main/resources/db/migration/V1__Create_Tables.sql`, `V2__Populate_Tables.sql`, `src/main/resources/application*.yaml`, `BatchService.java`, `BatchRepository.java` e `BatchServiceTest.java`.

**Arquivos a criar:** `src/test/java/br/com/hanrry/inventory/integration/PostgresIntegrationTest.java`, `src/test/java/br/com/hanrry/inventory/integration/InventoryTransactionIntegrationTest.java` e configuração de teste necessária em `src/test/resources/application-test.yaml`.

**Destino arquitetural:** infraestrutura de teste compartilhada em `shared` posteriormente; cenários de negócio sob `inventory`.

**Testes necessários:**

- iniciar um container PostgreSQL e validar que Flyway aplica `V1` e `V2` em banco limpo;
- confirmar constraints atuais de produto, lote, quantidade não negativa e chaves estrangeiras;
- criar um lote e consumir estoque usando a aplicação real;
- confirmar que uma exceção de estoque insuficiente não deixa a quantidade nem os logs parcialmente persistidos;
- confirmar que criação de lote, alteração de quantidade e log participam da mesma transação atual;
- não adicionar teste de concorrência ou locking pessimista neste plano.

**Comandos de validação:**

```bash
./mvnw -Dtest=PostgresIntegrationTest,InventoryTransactionIntegrationTest test
./mvnw clean test
```

**Critério de conclusão:** os testes executam contra PostgreSQL Testcontainers, validam migrations e rollback real, e a dependência/configuração adicionada fica limitada a testes. Nenhuma migration existente é editada.

**Commit:** `protegendo transacoes com postgres real`

### Tarefa 4: Proteger a cadeia real de Spring Security

**Objetivo:** registrar o comportamento de autenticação e autorização antes da migração de `auth` e `user`.

**Arquivos atuais envolvidos:** `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `JwtUtil.java`, `UserDetailsServiceImpl.java`, `AuthController.java`, `UserController.java` e `ProductController.java` como endpoints exercitados.

**Arquivos a criar:** `src/test/java/br/com/hanrry/inventory/integration/SecurityIntegrationTest.java` e configuração de teste necessária em `src/test/resources/application-test.yaml`.

**Destino arquitetural:** teste de integração compartilhado durante esta etapa; depois poderá ser associado a `auth` conforme a organização dos testes por domínio.

**Testes necessários:** usar `@SpringBootTest` com `@AutoConfigureMockMvc`, mantendo o `SecurityFilterChain`, `JwtAuthenticationFilter` e validação JWT reais. Mockar apenas dependências periféricas necessárias ao isolamento dos endpoints, sem usar `MockMvcBuilders.standaloneSetup`. Cobrir:

- acesso sem JWT a `GET /api/v1/users/{id}` retornando `401`;
- usuário com role `USER` tentando acessar `GET /api/v1/users/{id}` retornando `403`;
- usuário `ADMIN` acessando o mesmo endpoint permitido;
- acesso sem autenticação a endpoint público, como `GET /v3/api-docs`, sem `401`.

**Comandos de validação:**

```bash
./mvnw -Dtest=SecurityIntegrationTest test
./mvnw clean test
```

**Critério de conclusão:** a cadeia real do Spring Security valida os quatro cenários, os testes são independentes de controllers standalone e nenhuma regra de autorização é alterada.

**Commit:** `adicionando caracterizacao da seguranca`

### Tarefa 5: Migrar `auth` e `user`

**Objetivo:** mover autenticação e ciclo de vida de usuários para seus domínios, preservando JWT, BCrypt, roles e endpoints.

**Arquivos atuais envolvidos:** `AuthController.java`, `AuthControllerDocs.java`, `JwtAuthenticationFilter.java`, `JwtUtil.java`, `UserDetailsServiceImpl.java`, `SecurityConfig.java`, `UserController.java`, `UserControllerDocs.java`, `UserService.java`, `User.java`, `UserRepository.java`, `UserMapper.java`, DTOs de `auth` e `user`, exceções de usuário e testes correspondentes.

**Destino arquitetural:**

- `auth/controller`, `auth/security` e configuração de segurança compartilhada em `auth`;
- `user/controller`, `user/service`, `user/entity`, `user/repository`, `user/mapper` e `user/dto`;
- segurança deve continuar dependendo do contrato de usuário sem mover autenticação para `user`.

**Testes necessários:** manter e ajustar imports dos testes de autenticação e usuários; verificar geração/validação de JWT, login, registro, roles `USER`/`ADMIN`, endpoints públicos e protegidos. Preservar explicitamente o comportamento atual do update de senha até uma etapa específica de correção.

**Comandos de validação:**

```bash
./mvnw -Dtest=AuthControllerTest,UserControllerTest,UserServiceTest test
./mvnw clean test
```

**Critério de conclusão:** não existem cópias funcionais nos packages antigos, o contexto Spring inicia, os endpoints e contratos permanecem iguais e os testes passam.

**Commit:** `movendo auth e user para packages de dominio`

### Tarefa 6: Migrar `product` sem absorver estoque

**Objetivo:** concentrar catálogo, categorias e cálculo de leitura atual no domínio de produto, mantendo movimentação física fora dele.

**Arquivos atuais envolvidos:** `ProductController.java`, `ProductControllerDocs.java`, `CategoryController.java`, `CategoryControllerDocs.java`, `ProductService.java`, `CategoryService.java`, `Product.java`, `Category.java`, repositories, mappers, DTOs, exceções e testes de produto/categoria.

**Destino arquitetural:** `product/controller`, `product/service`, `product/entity`, `product/repository`, `product/mapper`, `product/dto` e exceções específicas de produto/categoria.

**Testes necessários:** preservar CRUD, SKU único, categoria obrigatória, deleção protegida, cálculo de estoque baixo e respostas HTTP. O cálculo atual, inclusive a soma de lotes vencidos e o limite inclusivo, deve continuar caracterizado; não introduzir nova regra de disponibilidade.

**Comandos de validação:**

```bash
./mvnw -Dtest=ProductServiceTest,CategoryServiceTest,ProductControllerTest,CategoryControllerTest,ProductMapperTest,CategoryMapperTest test
./mvnw clean test
```

**Critério de conclusão:** `product` contém somente catálogo/categorias e seus contratos; nenhuma movimentação ou integração externa foi adicionada; todos os testes passam.

**Commit:** `movendo product para package de dominio`

### Tarefa 7: Migrar `inventory`, incluindo `Batch` e logs

**Objetivo:** colocar lotes, entradas, saídas, logs, validade e FEFO dentro de `inventory`, sem implementar locking ou alterar o algoritmo atual.

**Arquivos atuais envolvidos:** `BatchController.java`, `BatchControllerDocs.java`, `BatchService.java`, `InventoryLogService.java`, `Batch.java`, `InventoryLog.java`, `BatchRepository.java`, `InventoryLogRepository.java`, mappers, DTOs, exceções de lote e testes de batch/log.

**Destino arquitetural:** `inventory/controller`, `inventory/service`, `inventory/batch`, `inventory/movement`, `inventory/repository`, `inventory/mapper`, `inventory/dto` e exceções do domínio.

**Testes necessários:** manter criação de lote com `ENTRY`, adição de quantidade, consumo `OUTPUT`, consumo sequencial FEFO, consumo em múltiplos lotes, insuficiência, lotes vencidos, mapeamentos e logs. Os testes PostgreSQL da Tarefa 3 devem continuar cobrindo rollback real.

**Comandos de validação:**

```bash
./mvnw -Dtest=BatchServiceTest,InventoryLogServiceTest,BatchControllerTest,BatchMapperTest,InventoryLogMapperTest,InventoryTransactionIntegrationTest test
./mvnw clean test
```

**Critério de conclusão:** `Batch` está dentro de `inventory`, o comportamento FEFO continua baseado na ordenação atual por `expiryDate`, não há `@Lock`, concorrência nova, evento ou RabbitMQ, e os testes passam.

**Commit:** `movendo inventory e batch para package de dominio`

### Tarefa 8: Isolar `notification` sem assíncrono novo

**Objetivo:** organizar o alerta, PDF e e-mail no limite arquitetural de `notification`, preservando o fluxo síncrono atual.

**Arquivos atuais envolvidos:** `StockAlertService.java`, `PdfService.java`, `EmailService.java`, `ProductService.java` como dependência de leitura, `ProductResponseDTO.java`, exceção de PDF e testes de alerta/PDF/e-mail.

**Destino arquitetural:** `notification/service`, `notification/document`, `notification/email` e exceções do módulo. Manter `PdfService` com o mesmo nome, métodos e API interna sempre que possível; a evolução para `RestockReportGenerator` ficará para o plano específico de notification.

**Testes necessários:** manter detecção agendada e pós-consumo, ausência de envio quando não há estoque baixo, conteúdo/cálculo do PDF e montagem/envio do e-mail. Registrar que não haverá idempotência, estado persistido, retry, DLQ ou publicação pós-commit nesta tarefa.

**Comandos de validação:**

```bash
./mvnw -Dtest=StockAlertServiceTest,PdfServiceTest,EmailServiceTest,BatchServiceTest test
./mvnw clean test
```

**Critério de conclusão:** os componentes estão em `notification`, o fluxo síncrono e o destinatário atual permanecem iguais, e nenhuma dependência de RabbitMQ foi criada.

**Commit:** `movendo notificacoes para package de dominio`

### Tarefa 9: Consolidar `shared`, documentação e exceções transversais

**Objetivo:** remover resíduos da organização por camada e manter apenas elementos realmente compartilhados.

**Arquivos atuais envolvidos:** `config/OpenApiConfig.java`, `config/SecurityConfig.java` conforme o destino definido na Tarefa 5, `controller/docs`, `exception/StandardError.java`, `exception/handler/GlobalExceptionHandler.java`, enums e configuração comum.

**Destino arquitetural:** `shared/config`, `shared/exception` e documentação próxima ao módulo que expõe cada contrato. Exceções de domínio devem permanecer no domínio correspondente; o handler pode permanecer compartilhado, importando-as sem concentrar regras de negócio.

**Testes necessários:** executar todos os controllers para preservar Swagger, status HTTP, payloads e tratamento global de exceções; executar a suíte completa para detectar beans ou imports antigos.

**Comandos de validação:**

```bash
./mvnw clean test
./mvnw package -DskipTests
```

**Critério de conclusão:** packages técnicos antigos não são mais a residência das classes migradas, não há duplicações, todos os imports apontam para os domínios e contratos HTTP permanecem inalterados.

**Commit:** `consolidando elementos compartilhados`

### Tarefa 10: Validação final do Plano 1 e preparação para review

**Objetivo:** confirmar que a fundação modular está funcional e pronta para revisão independente.

**Arquivos atuais envolvidos:** todos os arquivos tocados nas Tarefas 1–9, `.github/workflows/maven.yml` somente para conferir compatibilidade com o CI, e relatórios em `target/`.

**Destino arquitetural:** estado final do Plano 1, com módulos por domínio e sem os componentes reservados para planos posteriores.

**Testes necessários:** suíte completa, integração PostgreSQL, relatório JaCoCo e build Docker conforme o CI existente.

**Comandos de validação:**

```bash
./mvnw clean test
./mvnw package -DskipTests
docker build -t inventory-manager .
git diff --check
git status --short
```

**Critério de conclusão:** todos os comandos aplicáveis passam; não há alteração intencional em endpoints ou regras; a documentação de implementação lista limitações preservadas e não há arquivos fora do escopo.

**Commit:** `finalizando fundacao modular`

## Revisão de Consistência Arquitetural

Antes de executar o plano, conferir que cada tarefa preserva o documento arquitetural:

- mantém Modular Monolith e organização por domínio;
- trata `Batch` como parte de `inventory`;
- deixa `Product` responsável pelo catálogo, sem absorver movimentações;
- mantém JWT próprio e não introduz Keycloak;
- usa PostgreSQL Testcontainers somente para persistência, transações e Flyway;
- não introduz RabbitMQ, locking pessimista, retry, DLQ, observabilidade ou novas funcionalidades;
- não implementa Transactional Outbox;
- não acopla novas notificações a transações de estoque;
- não altera migrations já aplicadas, contracts HTTP ou regras de negócio;
- deixa concorrência real, eventos após commit, mensageria resiliente, observabilidade, Docker Compose e deploy para planos posteriores.

Qualquer contradição encontrada deve ser resolvida no próprio plano antes da implementação. Nenhuma tarefa deve começar enquanto o baseline e os testes de caracterização não estiverem definidos.

## Gate Obrigatório de Qualidade

Cada grupo relevante deve seguir:

```text
implementação
    → testes
    → code-review
```

Se o resultado for `APPROVED` ou `APPROVED_WITH_NOTES` sem bloqueadores, a próxima tarefa poderá começar.

Se o resultado for `CHANGES_REQUESTED`:

```text
fix-findings
    → testes
    → code-review novamente
```

O `fix-findings` deve confirmar a causa raiz, aplicar a menor correção segura e reportar `FIX_APPLIED`, `FINDING_NOT_CONFIRMED` ou `FIX_BLOCKED`; ele não aprova a própria alteração. O Plano 1 não será considerado concluído enquanto existirem findings `CRITICAL`, `HIGH` ou `MEDIUM` bloqueantes.
