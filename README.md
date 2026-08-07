# Inventory Manager

API REST para gestão inteligente de estoque e lotes, com autenticação JWT, consumo FEFO, alertas automáticos e documentação interativa.

[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Supabase](https://img.shields.io/badge/Supabase-3ECF8E?style=flat-square&logo=supabase&logoColor=white)](https://supabase.com/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![Render](https://img.shields.io/badge/Render-46E3B7?style=flat-square&logo=render&logoColor=white)](https://render.com/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/badge/version-v2.0-green?style=flat-square)](#)
[![Status](https://img.shields.io/badge/status-concluído-brightgreen?style=flat-square)](#)

**Demo online:** [Swagger UI](https://inventory.hanrry.top/swagger-ui/index.html) · `https://inventory.hanrry.top/swagger-ui/index.html`

---

## Sumário

- [Visão geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Demonstração](#demonstração)
- [Como testar online](#como-testar-online)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Principais endpoints](#principais-endpoints)
- [Modelo de dados](#modelo-de-dados)
- [Testes e performance](#testes-e-performance)
- [Rodando localmente](#rodando-localmente)
- [Autor](#autor)
- [Licença](#licença)

---

## Visão geral

O **Inventory Manager** não é só um CRUD de estoque. A API automatiza decisões operacionais críticas: prioriza lotes próximos do vencimento (**FEFO**), registra cada movimentação e dispara alertas quando o estoque fica abaixo do mínimo.

| Problema comum | Como a API ajuda |
| :--- | :--- |
| Produtos vencendo no fundo do estoque | Consumo FEFO (primeiro a vencer, primeiro a sair) |
| Estoque crítico sem aviso | Monitoramento agendado + e-mail com PDF |
| Falta de histórico | Logs imutáveis de entrada e saída |
| Acesso sem controle | JWT + roles `ADMIN` e `USER` |

**Deploy:** backend no [Render](https://render.com/) · banco PostgreSQL no [Supabase](https://supabase.com/) · domínio via Hostinger (`inventory.hanrry.top`).

---

## Funcionalidades

- **Autenticação e autorização** - login/registro com JWT, sessão stateless e controle por roles (`ADMIN` / `USER`)
- **Gestão de lotes** - fabricação, validade, preço unitário e quantidade por lote
- **Consumo FEFO** - saída de estoque priorizando o lote que vence primeiro
- **Alertas automáticos** - `@Scheduled` detecta estoque baixo, gera PDF e envia e-mail
- **Rastreabilidade** - histórico de movimentações (entradas e saídas) em logs
- **Documentação OpenAPI 3.0** - Swagger UI pronto para testar e integrar com front-end
- **Infraestrutura** - Docker (multi-stage), Flyway para versionamento do banco
- **Segurança de dados** - senhas com BCrypt; constraints e FKs no PostgreSQL

---

## Demonstração

<table width="100%">
  <tr>
    <td align="center" width="33%">
      <b>Swagger UI</b><br>
      <img src="https://github.com/user-attachments/assets/f898032f-63af-4885-b8bd-0408406910fa" width="100%" alt="Swagger UI">
      <p><i>Documentação interativa dos endpoints</i></p>
    </td>
    <td align="center" width="33%">
      <b>Alerta por e-mail</b><br>
      <img src="https://github.com/user-attachments/assets/10b3b170-d52f-4a79-8ca3-6fad61722bc5" width="100%" alt="E-mail de alerta">
      <p><i>Notificação de estoque crítico</i></p>
    </td>
    <td align="center" width="33%">
      <b>Relatório PDF</b><br>
      <img src="https://github.com/user-attachments/assets/a176c88b-486a-4850-82ea-fb696d522b92" width="100%" alt="Relatório PDF">
      <p><i>Anexo para reposição de estoque</i></p>
    </td>
  </tr>
</table>

---

## Como testar online

1. Abra: [https://inventory.hanrry.top/swagger-ui/index.html](https://inventory.hanrry.top/swagger-ui/index.html)
2. Faça login em `POST /api/v1/auth/login` com o body:

```json
{
  "email": "admin@email.com",
  "password": "admin123"
}
```

3. Copie o token JWT da resposta
4. Clique em **Authorize** no Swagger
5. Informe: `Bearer {seu_token}`
6. Teste os endpoints protegidos

URL alternativa (Render): [https://inventory-manager-3l2o.onrender.com/swagger-ui/index.html](https://inventory-manager-3l2o.onrender.com/swagger-ui/index.html)

> No plano free do Render, a primeira requisição após inatividade pode demorar alguns segundos (cold start).

---

## Tecnologias

| Camada | Stack |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Spring Mail, MapStruct, Lombok, OpenPDF, JWT, Maven |
| **Banco** | PostgreSQL, Flyway, Hibernate, Supabase |
| **Docs & qualidade** | SpringDoc OpenAPI (Swagger), JUnit 5, Mockito, MockMvc, JaCoCo, k6, Postman |
| **Infra** | Docker, Render, Hostinger (DNS) |

---

## Arquitetura

Arquitetura em camadas para separar responsabilidades, facilitar testes e evoluir com segurança.

| Camada | Responsabilidade |
| :--- | :--- |
| **Controller** | Endpoints REST (`/api/v1/...`) |
| **Service** | Regras de negócio, validações e orquestração |
| **Repository** | Acesso a dados (JPA / Hibernate) |
| **Security** | Filtro JWT e autorização por role |
| **DTO + Mapper** | Contrato da API sem expor entidades do banco |

---

## Principais endpoints

| Recurso | Método | Endpoint | Descrição |
| :--- | :---: | :--- | :--- |
| Auth | `POST` | `/api/v1/auth/login` | Autentica e retorna JWT |
| Auth | `POST` | `/api/v1/auth/register` | Registra novo usuário |
| Produtos | `GET` | `/api/v1/products` | Lista produtos |
| Produtos | `GET` | `/api/v1/products/low-stock` | Produtos com estoque baixo |
| Categorias | `GET` | `/api/v1/categories` | Lista categorias |
| Lotes | `POST` | `/api/v1/batches` | Cadastra lote |
| Lotes | `PATCH` | `/api/v1/batches/{id}/add` | Adiciona quantidade ao lote |
| Lotes | `POST` | `/api/v1/batches/consume` | Consome estoque (FEFO) |
| Lotes | `GET` | `/api/v1/batches/expired` | Lista lotes vencidos |
| Usuários | `GET` | `/api/v1/users` | Lista usuários (`ADMIN`) |

A lista completa está no Swagger.

---

## Modelo de dados

A modelagem prioriza integridade e rastreabilidade: constraints, FKs e índices únicos evitam inconsistências.

<div align="center">
  <img width="500" alt="Diagrama do banco de dados" src="https://github.com/user-attachments/assets/c12bc591-4048-4fee-9542-3d4f419cc480">
</div>

- **Rastreabilidade** - `tb_inventory_logs` guarda o histórico de entradas e saídas, ligado ao produto e (quando houver) ao lote
- **Gestão por lotes** - `tb_products` ↔ `tb_batches` permite validade e custo, base do FEFO
- **Normalização** - categorias e produtos separados para filtros e escala

---

## Testes e performance

### Cobertura (JaCoCo)

| Camada | Cobertura |
| :--- | :---: |
| Services | 97% |
| Controllers | 100% |
| Mappers | 92% |
| **Geral** | **70%** |

**Ferramentas:** JUnit 5, Mockito, MockMvc, JaCoCo.

**Cenários cobertos:** CRUD de produtos, categorias, usuários e lotes; entrada/saída de estoque; FEFO; logs; alertas; PDF; autenticação e autorização.

### Carga (k6)

| Métrica | Resultado |
| :--- | :---: |
| Throughput | ~149 RPS |
| Latência p95 | 9,9 ms |
| Falhas HTTP | 0% |

### Comando

```bash
./mvnw clean test
```

---

## Rodando localmente

### Pré-requisitos

- Java 21
- Docker (opcional)
- PostgreSQL acessível (local ou Supabase)
- Arquivo `.env` na raiz do projeto:

| Variável | Descrição |
| :--- | :--- |
| `DB_URL` | URL JDBC do PostgreSQL |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `EMAIL_USER` | E-mail remetente dos alertas |
| `EMAIL_CODE` | Senha de app do e-mail |
| `JWT_SECRET` | Segredo para assinar o JWT |
| `JWT_EXPIRATION` | Expiração do token (ms) |

### 1. Clone

```bash
git clone https://github.com/hanrrysantos/Inventory-Manager
cd Inventory-Manager
```

### 2. Com Maven

```bash
./mvnw spring-boot:run
```

### 3. Com Docker

```bash
docker build -t inventory-manager .
docker run --env-file .env -p 8080:8080 inventory-manager
```

Swagger local: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## Autor

**Hanrry Santos** - Desenvolvedor Backend Java | Spring Boot e arquitetura de software

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2?style=flat-square&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/hanrrysantos)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/hanrrysantos)

---

## Licença

Distribuído sob a [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
