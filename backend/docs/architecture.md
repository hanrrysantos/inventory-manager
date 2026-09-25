# Inventory Manager — Arquitetura

## Status

Este documento descreve a arquitetura-alvo do Inventory Manager.

A aplicação está sendo evoluída incrementalmente a partir da arquitetura atual.
Nem todos os componentes descritos aqui estão implementados.

O código existente é a fonte de verdade sobre o estado atual da aplicação.

---

## Objetivo

Evoluir o Inventory Manager mantendo as funcionalidades existentes, mas
fortalecendo sua arquitetura, consistência, resiliência, testabilidade e
observabilidade.

O projeto deve continuar sendo simples o suficiente para ser mantido como uma
única aplicação, evitando complexidade distribuída sem necessidade.

---

## Princípios

A evolução do projeto deve seguir estes princípios:

- preservar o comportamento funcional existente;
- realizar mudanças incrementais e verificáveis;
- preferir soluções simples antes de adicionar infraestrutura;
- manter limites claros entre responsabilidades;
- proteger consistência e integridade do estoque;
- desacoplar efeitos externos das transações de negócio;
- exigir justificativa concreta para novas dependências;
- priorizar testes dos fluxos críticos;
- evitar overengineering.

---

# Arquitetura-alvo

O Inventory Manager será estruturado como um **Modular Monolith** em Java 21
e Spring Boot 3.

A aplicação continuará sendo construída e implantada como uma única unidade,
mas o código será organizado por domínio em vez de exclusivamente por camada
técnica.

```text
br.com.hanrry.inventory
│
├── auth/
├── user/
├── product/
├── inventory/
│   ├── batch/
│   ├── movement/
│   └── ...
│
├── notification/
│   ├── consumer/
│   ├── service/
│   ├── email/
│   └── document/
│
└── shared/
```

Os packages internos podem evoluir durante a implementação desde que os limites
de responsabilidade definidos neste documento sejam preservados.

---

# Módulos

## Auth

Responsável por autenticação e autorização.

Tecnologias principais:

- Spring Security;
- JWT;
- BCrypt.

O projeto continuará utilizando autenticação JWT própria.

Keycloak não faz parte do escopo atual.

---

## User

Responsável pelo ciclo de vida dos usuários.

Autenticação e mecanismos de segurança devem permanecer no módulo `auth`,
evitando concentrar essas responsabilidades no domínio de usuários.

---

## Product

Responsável pelas informações e regras relacionadas ao catálogo de produtos.

Regras relacionadas à movimentação física de estoque pertencem ao módulo
`inventory`.

---

## Inventory

Responsável pelo controle e consistência do estoque.

Inclui:

- lotes;
- entradas;
- saídas;
- movimentações;
- validade;
- disponibilidade;
- consumo FEFO;
- detecção da necessidade de reposição.

`Batch` pertence ao domínio de `inventory` e não será tratado como módulo
independente.

---

# Consumo FEFO

O consumo de estoque deve continuar utilizando FEFO:

> First Expired, First Out.

Os lotes disponíveis devem ser consumidos priorizando aqueles com menor data
de validade.

O fluxo de consumo é uma operação crítica de consistência.

```text
Requisição
    │
    ▼
@Transactional
    │
    ▼
Buscar lotes disponíveis
ordenados por validade
    │
    ▼
PESSIMISTIC_WRITE
    │
    ▼
Consumir quantidades
    │
    ▼
Registrar movimentação
    │
    ▼
Commit
```

---

# Concorrência

O sistema deve impedir consumo superior ao estoque disponível mesmo quando
existirem requisições concorrentes.

O fluxo crítico de consumo FEFO utilizará **pessimistic locking** no banco de
dados.

A implementação deverá possuir testes de integração capazes de reproduzir
concorrência real contra PostgreSQL.

Quantidade de estoque negativa ou double spending de estoque são estados
inválidos.

---

# Reposição

Quando uma movimentação fizer o estoque atingir a condição de reposição, o
domínio de inventory deverá produzir um evento representando essa necessidade.

Nome conceitual:

```text
RestockNeededEvent
```

Antes de introduzir nova regra de cálculo de reposição, a implementação atual
deve ser analisada.

Se já existir cálculo adequado da quantidade necessária para reposição, ele
deve ser preservado.

Caso não exista, a regra poderá ser introduzida durante essa etapa da evolução.

---

# Eventos e transações

Eventos relacionados a alterações de estoque não devem produzir efeitos
externos antes da confirmação da transação.

Fluxo desejado:

```text
Inventory Transaction
        │
        ▼
     COMMIT
        │
        ▼
RestockNeededEvent
        │
        ▼
     RabbitMQ
```

A publicação para RabbitMQ deve ocorrer somente após commit bem-sucedido.

Uma transação que sofreu rollback não pode produzir uma notificação de
reposição válida.

Transactional Outbox não será implementado inicialmente.

---

# RabbitMQ

RabbitMQ será utilizado para desacoplar o processamento de notificações do
fluxo transacional de estoque.

Objetivos:

- impedir que falhas de e-mail revertam movimentações de estoque;
- permitir processamento assíncrono;
- suportar retry;
- suportar redelivery;
- permitir DLQ;
- melhorar isolamento entre inventory e notification.

Fluxo:

```text
Inventory
    │
    ▼
RestockNeededEvent
    │
    ▼
RabbitMQ
    │
    ▼
Notification Consumer
```

RabbitMQ não deve ser utilizado para comunicação interna que não necessite
processamento assíncrono.

---

# Notification

Responsável pelo processamento das notificações de reposição.

Estrutura conceitual:

```text
notification/
├── consumer/
├── service/
├── email/
├── document/
├── entity/
└── repository/
```

O módulo deverá:

1. consumir eventos de reposição;
2. impedir processamento duplicado;
3. registrar o estado da notificação;
4. gerar o relatório de reposição;
5. enviar o e-mail ao gestor;
6. registrar sucesso ou falha.

---

# Idempotência

Mensagens podem ser entregues mais de uma vez.

Cada evento deverá possuir um identificador único.

Antes de processar uma notificação, o sistema deve verificar se aquele evento
já foi processado.

Uma redelivery não pode resultar em múltiplas notificações equivalentes ao
gestor.

---

# Estado das notificações

O histórico das notificações será persistido no PostgreSQL.

Estados conceituais iniciais:

```text
PENDING
SENT
FAILED
```

O modelo definitivo será estabelecido durante a implementação.

Esse histórico permitirá rastrear o processamento e auxiliar a garantia de
idempotência.

---

# Retry e DLQ

Falhas temporárias no processamento da notificação devem permitir novas
tentativas.

Fluxo conceitual:

```text
RabbitMQ
    │
    ▼
Consumer
    │
    ├── sucesso ──► ACK
    │
    └── falha
           │
           ▼
         Retry
           │
           ├── sucesso ──► ACK
           │
           └── limite atingido
                    │
                    ▼
                   DLQ
```

Retries infinitos não são permitidos.

Mensagens que excederem a política de tentativas deverão ser encaminhadas para
uma Dead Letter Queue.

---

# Relatório de reposição

O PDF não será tratado inicialmente como um módulo de domínio independente.

Ele faz parte do fluxo de notificação de reposição.

Responsabilidade conceitual:

```text
notification/document/RestockReportGenerator
```

O documento deve fornecer ao gestor informações suficientes para decidir a
reposição, incluindo os dados já disponíveis no sistema sobre estoque atual,
limite e quantidade necessária.

A regra existente deve ser preservada quando adequada.

---

# Observabilidade

A aplicação utilizará:

- Spring Boot Actuator;
- Micrometer;
- Prometheus;
- Grafana.

Além das métricas técnicas de JVM, HTTP e infraestrutura, o sistema deverá
expor métricas relevantes do domínio quando houver valor operacional.

Exemplos conceituais:

```text
stock_consumption_total
restock_events_total
notifications_sent_total
notifications_failed_total
```

Os nomes definitivos deverão seguir as convenções do Micrometer.

Eventos e logs relacionados ao mesmo processamento devem possuir identificadores
que permitam correlação.

---

# Testes

A estratégia atual com JUnit 5, Mockito e MockMvc será preservada.

A evolução adicionará **Testcontainers** para cenários que dependam de
infraestrutura real.

Containers previstos:

```text
PostgreSQL
RabbitMQ
```

Testes de integração devem cobrir principalmente:

- migrations Flyway;
- queries relevantes;
- consumo FEFO;
- pessimistic locking;
- concorrência;
- publicação de eventos;
- RabbitMQ;
- idempotência;
- persistência das notificações.

Mocks continuam apropriados para testes unitários.

Não substituir testes unitários por testes de integração sem necessidade.

---

# Infraestrutura local

O projeto deverá possuir Docker Compose para execução da infraestrutura de
desenvolvimento.

Arquitetura prevista:

```text
Docker Compose
│
├── inventory-manager
├── PostgreSQL
├── RabbitMQ
├── Prometheus
└── Grafana
```

Também deve ser possível executar o Spring Boot diretamente pela IDE/Maven
utilizando a infraestrutura em containers.

---

# CI

GitHub Actions será utilizado para validação contínua.

Fluxo esperado:

```text
Push / Pull Request
        │
        ▼
      Build
        │
        ▼
      Tests
        │
        ▼
Integration Tests
        │
        ▼
     JaCoCo
```

O pipeline não deve realizar deploy se os testes obrigatórios falharem.

---

# Deploy

O projeto deverá possuir um ambiente público de demonstração.

A infraestrutura definitiva será escolhida posteriormente entre VPS ou cloud
de baixo custo.

O deploy deverá utilizar containers.

Serviços internos como PostgreSQL, RabbitMQ e Prometheus não devem ficar
publicamente acessíveis.

HTTPS deverá ser utilizado para os endpoints públicos.

A automação de deploy será definida depois que a arquitetura da aplicação
estiver estabilizada.

---

# Decisões deliberadamente fora do escopo

## Microsserviços

O projeto permanecerá como Modular Monolith.

Distribuir módulos em serviços independentes adicionaria complexidade sem
necessidade atual.

## Redis

Não existe atualmente um problema que justifique adicionar Redis.

Pode ser reavaliado caso surja necessidade concreta de cache ou outro caso de
uso.

## Keycloak

Spring Security + JWT atendem às necessidades atuais.

## Transactional Outbox

A primeira implementação utilizará publicação após commit.

Outbox poderá ser avaliado futuramente caso seja necessário aumentar as
garantias de entrega entre PostgreSQL e RabbitMQ.

---

# Estratégia de evolução

A arquitetura não será implementada por uma reescrita completa.

A evolução deve acontecer incrementalmente:

```text
Sistema atual
     │
     ▼
Caracterização e testes
     │
     ▼
Organização por domínio
     │
     ▼
Concorrência e consistência
     │
     ▼
Eventos após commit
     │
     ▼
RabbitMQ
     │
     ▼
Notificações resilientes
     │
     ▼
Observabilidade
     │
     ▼
Testcontainers
     │
     ▼
Docker / CI
     │
     ▼
Deploy
```

Cada etapa deve deixar o sistema em estado funcional e verificável.

---

# Restrições arquiteturais

Durante a evolução:

- não reescrever o sistema inteiro;
- não alterar funcionalidades existentes sem necessidade documentada;
- não introduzir dependências apenas para demonstrar tecnologia;
- não criar novos módulos para cada entidade;
- não transformar o projeto em microsserviços;
- não acoplar novamente notificações às transações de estoque;
- não permitir que falhas externas revertam movimentações válidas;
- não modificar migrations Flyway já aplicadas;
- não expor secrets;
- não considerar uma mudança concluída sem validação adequada.

Quando uma necessidade real contradizer alguma decisão deste documento, a
decisão arquitetural deve ser revisada explicitamente antes da implementação.