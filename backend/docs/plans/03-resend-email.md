# Migração de envio de e-mail para Resend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir o envio de alertas de estoque via SMTP/Gmail pelo Resend, mantendo o PDF, o agendamento e o comportamento do estoque.

**Architecture:** `StockAlertService` dependerá de uma porta interna `EmailSender`. O adaptador `ResendEmailSender` implementará essa porta usando o SDK Java do Resend, mantendo o provedor externo isolado no módulo `notification`. O envio continuará síncrono nesta etapa; RabbitMQ e outbox permanecem fora do escopo.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Maven, Resend Java SDK 4.11.0, JUnit 5, Mockito, OpenPDF.

**Spec:** Este documento contém a especificação aprovada e o plano de implementação.

## Global Constraints

- Preservar `PdfService.generateLowStockReport(List<ProductResponseDTO>)` e seu retorno `byte[]`.
- Preservar `StockAlertService.checkInventoryAndNotify()` e o `@Scheduled(initialDelay = 10000, fixedRate = 36000000)`.
- Não introduzir RabbitMQ, Transactional Outbox, templates hospedados, webhooks ou alteração das regras de estoque.
- Usar as variáveis `RESEND_API_KEY`, `RESEND_FROM` e `RESEND_TO`; não versionar secrets.
- Usar quatro espaços de indentação e os packages existentes por domínio.
- Executar os comandos Maven com `bash ./mvnw`.
- Preservar alterações não relacionadas já existentes no `Dockerfile` e em `docs/frontend-readiness.md`.

---

## Especificação aprovada

### Escopo

Esta etapa inclui:

- substituir `JavaMailSender` pelo cliente Java do Resend;
- manter `StockAlertService` como orquestrador do alerta;
- manter `PdfService` e sua saída `byte[]`;
- enviar o relatório PDF como anexo;
- configurar API key, remetente e destinatário por variáveis de ambiente;
- remover a dependência da configuração SMTP;
- preservar o tratamento do envio como efeito externo da notificação;
- atualizar testes unitários e de integração necessários ao contrato alterado.

Esta etapa não inclui:

- RabbitMQ;
- Transactional Outbox;
- alteração do `@Scheduled`;
- alteração das regras de estoque;
- alteração da construção do PDF;
- templates hospedados, envio em lote ou webhooks do Resend.

### Design e fluxo

O fluxo permanecerá síncrono nesta etapa:

```text
StockAlertService
    ├── ProductService
    ├── PdfService → byte[]
    └── EmailSender
            └── ResendEmailSender → Resend API
```

`StockAlertService` continuará identificando produtos com estoque baixo,
gerando o PDF e solicitando o envio. `PdfService` não terá alteração. A
abstração interna `EmailSender` isolará o módulo de notificação do SDK e do
modelo de requisição do Resend. `ResendEmailSender` será o adaptador
responsável por montar e enviar a mensagem.

O contrato do envio receberá os nomes dos produtos e o conteúdo binário do
relatório PDF. O adaptador enviará assunto, corpo textual e o arquivo
`relatorio_reposicao.pdf` como anexo. O destinatário não permanecerá
hardcoded no código.

### Configuração e erros

As variáveis de ambiente serão:

```text
RESEND_API_KEY
RESEND_FROM
RESEND_TO
```

`RESEND_API_KEY` será usada somente pelo adaptador. `RESEND_FROM` deverá ser um
remetente autorizado pelo Resend, preferencialmente de um domínio verificado.
`RESEND_TO` substituirá o destinatário fixo existente. As propriedades SMTP
atuais (`EMAIL_USER`, `EMAIL_CODE` e `spring.mail`) serão removidas quando não
houver mais consumidores delas. Nenhum secret será versionado.

Falhas de autenticação, validação de remetente, comunicação ou resposta de
erro do Resend serão convertidas em uma exceção da camada de notificação com
mensagem contextual. O erro não alterará regras de estoque nesta etapa; retry
por mensageria permanece como trabalho futuro.

### Critérios de aceitação

- A aplicação deixa de depender de SMTP/Gmail para enviar alertas.
- O alerta continua gerando o mesmo relatório PDF.
- O PDF é enviado como anexo pela API do Resend.
- Remetente e destinatário são configuráveis por ambiente.
- Nenhum segredo é versionado.
- Os testes relevantes passam sem chamadas externas.
- O comportamento do estoque e do agendamento permanece inalterado.

---

### Task 1: Adicionar o cliente Resend e a configuração da aplicação

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/br/com/hanrry/inventory/notification/email/ResendConfiguration.java`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/test/resources/application-test.yaml`
- Modify: `.env.example`
- Modify: `docker-compose.yml`
- Test: `src/test/java/br/com/hanrry/inventory/notification/emailTest/ResendConfigurationTest.java`

**Interfaces:**
- Produces: propriedades `resend.api-key`, `resend.from` e `resend.to` disponíveis para o adaptador.
- Consumes: variáveis de ambiente `RESEND_API_KEY`, `RESEND_FROM` e `RESEND_TO`.

- [ ] **Step 1: Write the failing configuration test**

Criar um teste de contexto que forneça as três propriedades Resend por
`@TestPropertySource` e verifique que o bean cliente é criado. O teste deverá
usar uma configuração de teste mínima, sem chamar a API externa.

```java
@SpringBootTest(classes = ResendConfiguration.class)
@TestPropertySource(properties = {
        "resend.api-key=re_test",
        "resend.from=alerts@example.com",
        "resend.to=owner@example.com"
})
class ResendConfigurationTest {

    @Autowired
    private Resend resend;

    @Test
    void shouldCreateResendClientFromConfiguredApiKey() {
        assertNotNull(resend);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
bash ./mvnw -Dtest=ResendConfigurationTest test
```

Expected: FAIL because `ResendConfiguration` and the Resend dependency do not
exist yet.

- [ ] **Step 3: Add the pinned Resend dependency**

Adicionar ao `pom.xml`:

```xml
<dependency>
    <groupId>com.resend</groupId>
    <artifactId>resend-java</artifactId>
    <version>4.11.0</version>
</dependency>
```

- [ ] **Step 4: Add the Resend configuration class**

Criar `src/main/java/br/com/hanrry/inventory/notification/email/ResendConfiguration.java`:

```java
package br.com.hanrry.inventory.notification.email;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResendConfiguration {

    @Bean
    public Resend resend(@Value("${resend.api-key:}") String apiKey) {
        return new Resend(apiKey);
    }
}
```

- [ ] **Step 5: Replace SMTP properties with Resend properties**

Em `src/main/resources/application.yaml`, remover todo o bloco
`spring.mail` e adicionar:

```yaml
resend:
  api-key: ${RESEND_API_KEY:}
  from: ${RESEND_FROM:}
  to: ${RESEND_TO:}
```

Remover `spring.mail` do arquivo de teste. Atualizar `.env.example` com as
três variáveis sem valores secretos e atualizar `docker-compose.yml` para
repassá-las ao serviço `api`:

```yaml
RESEND_API_KEY: ${RESEND_API_KEY:-}
RESEND_FROM: ${RESEND_FROM:-}
RESEND_TO: ${RESEND_TO:-}
```

- [ ] **Step 6: Run the configuration test to verify it passes**

Run:

```bash
bash ./mvnw -Dtest=ResendConfigurationTest test
```

Expected: PASS, sem chamada HTTP ao Resend.

- [ ] **Step 7: Commit the configuration change**

```bash
git add pom.xml src/main/java/br/com/hanrry/inventory/notification/email/ResendConfiguration.java src/main/resources/application.yaml src/test/resources/application-test.yaml .env.example docker-compose.yml src/test/java/br/com/hanrry/inventory/notification/emailTest/ResendConfigurationTest.java
git commit -m "adiciona configuracao do resend"
```

### Task 2: Criar a porta e o adaptador de envio com anexo PDF

**Files:**
- Create: `src/main/java/br/com/hanrry/inventory/notification/email/EmailSender.java`
- Create: `src/main/java/br/com/hanrry/inventory/notification/email/ResendEmailSender.java`
- Create: `src/main/java/br/com/hanrry/inventory/shared/exception/notification/email/EmailSendException.java`
- Create: `src/test/java/br/com/hanrry/inventory/notification/emailTest/ResendEmailSenderTest.java`
- Verify: `src/main/java/br/com/hanrry/inventory/notification/email/EmailService.java`
- Verify: `src/test/java/br/com/hanrry/inventory/notification/emailTest/EmailServiceTest.java`

**Interfaces:**
- Produces: `EmailSender.sendLowStockAlert(List<String> productNames, byte[] pdfAttachment)`.
- Consumes: bean `Resend`, `resend.from` e `resend.to`.
- Error contract: falhas do SDK ou da API são convertidas em `EmailSendException`.

- [ ] **Step 1: Write the failing port and adapter tests**

Criar testes que capturem `CreateEmailOptions` enviado ao cliente e verifiquem
remetente, destinatário, assunto, corpo e anexo `relatorio_reposicao.pdf`.
O teste de erro deverá simular `ResendException` e esperar
`EmailSendException`.

O adaptador deverá expor esta porta:

```java
public interface EmailSender {

    void sendLowStockAlert(List<String> productNames, byte[] pdfAttachment);
}
```

O corpo esperado no teste deverá permanecer:

```text
Olá,

Os seguintes produtos atingiram o nível crítico de estoque:
Notebook, Mouse.

Segue em anexo o relatório detalhado de reposição para todos os itens em falta.
```

- [ ] **Step 2: Run the adapter tests to verify they fail**

Run:

```bash
bash ./mvnw -Dtest=ResendEmailSenderTest test
```

Expected: FAIL because the port, adapter, exception and Resend payload do not
exist.

- [ ] **Step 3: Implement the port and exception**

Criar a interface `EmailSender` com o método acima. Criar
`EmailSendException` como exceção não verificada com construtor para mensagem
e causa:

```java
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Implement the Resend adapter**

Criar `ResendEmailSender` como `@Service`, injetando `Resend`,
`@Value("${resend.from:}") String from` e `@Value("${resend.to:}") String to`.
Montar `CreateEmailOptions` com `from`, `to`, assunto, texto e um objeto de
anexo do modelo `com.resend.services.emails.model.Attachment`, usando o nome
`relatorio_reposicao.pdf` e o conteúdo Base64 de `pdfAttachment`. Chamar
`resend.emails().send(options)` e converter `ResendException` para
`EmailSendException` com a mensagem `Falha ao enviar alerta de estoque pelo Resend`.

Antes da chamada, rejeitar configuração vazia com `EmailSendException` contendo
o nome da configuração ausente, sem incluir o valor da API key na mensagem.

- [ ] **Step 5: Run the adapter tests to verify they pass**

Run:

```bash
bash ./mvnw -Dtest=ResendEmailSenderTest test
```

Expected: PASS sem conexão externa e com validação do conteúdo do anexo.

- [ ] **Step 6: Commit the adapter change**

```bash
git add src/main/java/br/com/hanrry/inventory/notification/email/EmailSender.java src/main/java/br/com/hanrry/inventory/notification/email/ResendEmailSender.java src/main/java/br/com/hanrry/inventory/shared/exception/notification/email/EmailSendException.java src/test/java/br/com/hanrry/inventory/notification/emailTest/ResendEmailSenderTest.java
git rm src/main/java/br/com/hanrry/inventory/notification/email/EmailService.java src/test/java/br/com/hanrry/inventory/notification/emailTest/EmailServiceTest.java
git commit -m "adiciona adaptador de email do resend"
```

### Task 3: Conectar o alerta de estoque à porta de envio

**Files:**
- Modify: `src/main/java/br/com/hanrry/inventory/notification/service/StockAlertService.java`
- Modify: `src/test/java/br/com/hanrry/inventory/notification/serviceTest/StockAlertServiceTest.java`

**Interfaces:**
- Consumes: `EmailSender` com o mesmo método de alerta já usado pelo serviço.
- Produces: `StockAlertService` sem dependência direta de `ResendEmailSender` ou do SDK.

- [ ] **Step 1: Change the service test dependency to the port**

Substituir o mock de `EmailService` por:

```java
@Mock
private EmailSender emailSender;
```

Atualizar as verificações para `verify(emailSender).sendLowStockAlert(...)`.

- [ ] **Step 2: Run the service tests to verify they fail**

Run:

```bash
bash ./mvnw -Dtest=StockAlertServiceTest test
```

Expected: FAIL porque `StockAlertService` ainda exige `EmailService`.

- [ ] **Step 3: Inject the port into StockAlertService**

Trocar o campo e o construtor gerado por Lombok para `EmailSender`, mantendo
inalterado o método `checkInventoryAndNotify()` e o `@Scheduled`.

- [ ] **Step 4: Run the service tests to verify they pass**

Run:

```bash
bash ./mvnw -Dtest=StockAlertServiceTest test
```

Expected: PASS, incluindo o caso sem produtos com estoque baixo, sem gerar PDF
ou chamar o remetente.

- [ ] **Step 5: Commit the integration change**

```bash
git add src/main/java/br/com/hanrry/inventory/notification/service/StockAlertService.java src/test/java/br/com/hanrry/inventory/notification/serviceTest/StockAlertServiceTest.java
git commit -m "desacopla alerta do provedor de email"
```

### Task 4: Remover o SMTP, atualizar documentação de ambiente e validar a migração

**Files:**
- Modify: `pom.xml`
- Delete: `src/main/java/br/com/hanrry/inventory/notification/email/EmailService.java`
- Delete: `src/test/java/br/com/hanrry/inventory/notification/emailTest/EmailServiceTest.java`
- Modify: `README.md` sections `Tecnologias` e `Rodando localmente`
- Modify: `.env.example`
- Modify: `docker-compose.yml`
- Verify: `src/main/resources/application.yaml`
- Verify: `src/test/resources/application-test.yaml`
- Verify: all notification tests under `src/test/java/br/com/hanrry/inventory/notification`

**Interfaces:**
- Produces: aplicação sem referências a `JavaMailSender`, `EMAIL_USER`, `EMAIL_CODE` ou `spring.mail`.
- Consumes: configuração Resend documentada e pronta para Render.

- [ ] **Step 1: Search for stale SMTP references**

Remover o `spring-boot-starter-mail` do `pom.xml`, excluir os arquivos antigos
`EmailService.java` e `EmailServiceTest.java`, atualizar a tabela de tecnologias
do `README.md` para substituir Spring Mail por Resend e substituir a tabela de
ambiente `EMAIL_USER`/`EMAIL_CODE` por `RESEND_API_KEY`, `RESEND_FROM` e
`RESEND_TO`.

Run:

```bash
rg -n "JavaMailSender|spring\.mail|EMAIL_USER|EMAIL_CODE|smtp\.gmail\.com|EmailService" src pom.xml .env.example docker-compose.yml README.md
```

Expected: nenhum resultado de produção ou configuração antiga. Referências
históricas em documentação devem ser atualizadas, não mantidas.

- [ ] **Step 2: Run the complete Maven verification**

Run:

```bash
bash ./mvnw clean test
```

Expected: build e testes concluídos com sucesso, incluindo JaCoCo, sem chamada
externa ao Resend.

- [ ] **Step 3: Build the production image**

Run:

```bash
docker build -t inventory-manager:resend .
```

Expected: imagem construída com o SDK Resend e sem erro de empacotamento.

- [ ] **Step 4: Validate the local container configuration**

Executar o Compose com `RESEND_API_KEY`, `RESEND_FROM` e `RESEND_TO`
fornecidos apenas no ambiente local. Confirmar que a aplicação inicia, que
`/v3/api-docs` responde `200` e que o health check do Compose fica saudável.

```bash
RESEND_API_KEY=re_test RESEND_FROM=onboarding@resend.dev RESEND_TO=owner@example.com docker compose up -d --wait
curl -i http://localhost:8080/v3/api-docs
docker compose ps
docker compose down
```

Expected: resposta HTTP `200`, serviço `healthy` e encerramento limpo dos
containers.

- [ ] **Step 5: Configure Render without committing secrets**

No painel do Render, cadastrar `RESEND_API_KEY`, `RESEND_FROM` e `RESEND_TO`.
Usar um remetente autorizado pelo Resend e manter o Health Check Path como
`/v3/api-docs` até existir um endpoint dedicado de saúde.

- [ ] **Step 6: Run a final status and diff review**

Run:

```bash
git diff --check
git status --short
git diff --stat
```

Expected: somente arquivos da migração estarão nos commits da tarefa; as
alterações preexistentes do `Dockerfile` e `docs/frontend-readiness.md` não
serão incluídas por engano.

- [ ] **Step 7: Commit the migration validation and documentation**

```bash
git add pom.xml src/main/java/br/com/hanrry/inventory/notification/email/EmailService.java src/test/java/br/com/hanrry/inventory/notification/emailTest/EmailServiceTest.java README.md .env.example docker-compose.yml src/main/resources/application.yaml src/test/resources/application-test.yaml
git commit -m "finaliza migracao de email para resend"
```

## Review Checklist

- [ ] O PDF continua sendo gerado pelo `PdfService` sem alterações.
- [ ] O alerta continua sendo disparado pelo agendamento existente.
- [ ] `StockAlertService` depende de `EmailSender`, não do SDK Resend.
- [ ] O anexo mantém o nome `relatorio_reposicao.pdf` e o conteúdo original.
- [ ] O destinatário não está hardcoded.
- [ ] Não existem credenciais no repositório.
- [ ] Os testes não fazem chamadas externas.
- [ ] RabbitMQ e outbox não foram introduzidos nesta etapa.
