# Inventory Manager

API REST para controle de produtos, categorias e lotes, com autenticação JWT, consumo FEFO (primeiro a vencer, primeiro a sair), histórico de movimentações e alertas de reposição por e-mail com PDF.

[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)

[Swagger da demonstração](https://inventory.hanrry.top/swagger-ui/index.html) · [Arquitetura](docs/architecture.md) · [Planos de evolução](docs/plans/)

## Funcionalidades

- Cadastro de produtos, categorias, usuários e lotes com quantidade, preço e validade.
- Entrada e consumo de estoque por FEFO, com histórico de movimentações e proteção contra consumo concorrente.
- Consulta de estoque baixo, lotes vencidos e resumo do dashboard.
- Autenticação JWT, permissões `ADMIN`/`USER` e consulta do usuário autenticado.
- Verificação agendada de estoque baixo e envio de alertas via Resend com relatório PDF.
- CORS configurável para integração com o frontend e documentação OpenAPI/Swagger.

## Tecnologias e organização

Java 21, Spring Boot 3, Spring Security, Spring Data JPA, PostgreSQL e Flyway. DTOs e mapeamento com MapStruct/Lombok; e-mails com Resend e PDFs com OpenPDF. Testes com JUnit 5, Mockito, MockMvc e Testcontainers, cobertura com JaCoCo e CI com GitHub Actions.

O código está organizado nos módulos `auth`, `user`, `product`, `inventory`, `dashboard`, `notification` e `shared`. A evolução é incremental: [architecture.md](docs/architecture.md) descreve a arquitetura-alvo, incluindo etapas ainda não implementadas.

## Executando localmente

### Com Docker Compose

Pré-requisito: Docker com Compose. Na raiz do repositório:

```bash
cp .env.example .env
# Edite a .env antes de iniciar.
docker compose up -d --build
```

Se já possui uma `.env`, atualize-a usando o exemplo como referência, sem sobrescrevê-la. O Compose inicia a API e o PostgreSQL, com dados persistidos em volume. As migrations Flyway são aplicadas na inicialização.

- Swagger: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Logs: `docker compose logs -f api`
- Encerrar: `docker compose down` (preserva o volume do banco).

### Variáveis de ambiente

Use [.env.example](.env.example) como referência e mantenha a `.env` fora do versionamento.

| Variável | Uso |
| :--- | :--- |
| `API_PORT` | Porta da API no host pelo Compose; padrão `8080`. |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Banco e credenciais do PostgreSQL no Compose. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão JDBC ao executar fora do Compose; nele, são definidas automaticamente. |
| `JWT_SECRET` | Segredo de assinatura; use um valor aleatório com pelo menos 32 bytes. |
| `JWT_EXPIRATION` | Validade do token em milissegundos; exemplo: `3600000` (1 hora). |
| `FRONTEND_ORIGINS` | Origens CORS separadas por vírgulas; padrão `http://localhost:5173`. |
| `RESEND_API_KEY` | Chave de API para envio de e-mails. |
| `RESEND_FROM`, `RESEND_TO` | Remetente autorizado no Resend e destinatário dos alertas. |

Substitua os valores de exemplo de senha e JWT. Para os alertas funcionarem, configure as três variáveis do Resend com valores válidos.

Exemplo para frontend local e publicado:

```dotenv
FRONTEND_ORIGINS=http://localhost:5173,https://meu-frontend.com
```

O Compose lê a `.env` automaticamente. Ao executar pelo Maven ou pela IDE, configure as variáveis no ambiente do processo; a aplicação não carrega a `.env` por conta própria.

### Com Maven ou IDE

Requer Java 21 e PostgreSQL acessível. Configure as variáveis da API acima, incluindo `DB_URL` (por exemplo, `jdbc:postgresql://localhost:5432/inventory`), `DB_USERNAME` e `DB_PASSWORD`.

```bash
bash ./mvnw spring-boot:run
```

A porta padrão é `8080` e pode ser alterada com `PORT`. O PostgreSQL do Compose não publica uma porta no host; para rodar via Maven/IDE, use uma instância acessível ou configure explicitamente essa publicação.

## Autenticação e endpoints

No Swagger local ou da demonstração, cadastre um usuário em `POST /api/v1/auth/register` e faça login em `POST /api/v1/auth/login`. Copie o campo `token` da resposta e cole em **Authorize**, sem o prefixo `Bearer`. Em chamadas HTTP, use `Authorization: Bearer <token>`.

| Método | Endpoint | Finalidade |
| :--- | :--- | :--- |
| `GET` | `/api/v1/users/me` | Usuário autenticado e sua role. |
| `GET` | `/api/v1/dashboard/summary` | Resumo do estoque. |
| `GET` | `/api/v1/products` | Lista de produtos. |
| `GET` | `/api/v1/products/low-stock` | Produtos com estoque baixo. |
| `GET` | `/api/v1/categories` | Lista de categorias. |
| `POST` | `/api/v1/batches` | Cadastro de lote. |
| `PATCH` | `/api/v1/batches/{id}/add` | Entrada de quantidade no lote. |
| `POST` | `/api/v1/batches/consume` | Consumo de estoque por FEFO. |
| `GET` | `/api/v1/batches/expired` | Lotes vencidos. |
| `GET` | `/api/v1/users` | Lista de usuários (`ADMIN`). |

`USER` e `ADMIN` podem consultar produtos/categorias, consumir estoque e adicionar quantidades. Gestão de usuários, alterações no catálogo e cadastro de lotes exigem `ADMIN`. Consulte o Swagger para os contratos completos.

## Testes

Com Java 21 e Docker disponível para os testes de integração com PostgreSQL:

```bash
bash ./mvnw clean test
```

A suíte inclui testes unitários, de API e de integração, com cenários de FEFO, rollback e concorrência. O relatório de cobertura é gerado em `target/site/jacoco/index.html`.

## Autor e licença

[Hanrry Santos](https://github.com/hanrrysantos) · [LinkedIn](https://www.linkedin.com/in/hanrrysantos)

Licença declarada: [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).
