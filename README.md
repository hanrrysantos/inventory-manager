# Inventory Manager

Sistema web para acompanhar estoque por produto e lote. A API registra entradas
e saídas, prioriza lotes pela validade (FEFO) e mantém o histórico das
movimentações. O frontend oferece acesso, dashboard e gestão de produtos e
categorias.

## Demonstração

- [Aplicação web](https://controle-de-estoque.hanrry.top)
- [API e Swagger UI](https://api-controle-de-estoque.hanrry.top/swagger-ui/index.html)

## Componentes

| Diretório | Conteúdo | Guia |
| --- | --- | --- |
| `backend/` | API REST Java 21, Spring Boot e PostgreSQL | [Backend](backend/README.md) |
| `frontend/` | Aplicação React, TypeScript e Vite | [Frontend](frontend/README.md) |
| `docs/` | Specs, planos, tasks e arquitetura do monorepo | [Documentação](docs/README.md) |

## Executar localmente

### API e dependências com Docker

Requer Docker com Docker Compose. O Compose sobe PostgreSQL, RabbitMQ, API,
Prometheus e Grafana:

```bash
cd backend
cp .env.example .env
```

Edite `backend/.env` para definir `JWT_SECRET` e, se for usar alertas por
e-mail, as configurações do Resend. Depois execute:

```bash
docker compose up -d --build
```

A API fica em <http://localhost:8080>; Swagger em
<http://localhost:8080/swagger-ui/index.html>. Para encerrar, use
`docker compose down`. Consulte [backend/README.md](backend/README.md) para
configuração e execução sem Docker.

### Frontend

Em outro terminal, com Node.js 22.12+ ou 24 e npm:

```bash
cd frontend
npm ci
cp .env.example .env
npm run dev
```

Abra <http://localhost:5173>. Configure `VITE_API_URL` em `frontend/.env` para
a API local (`http://localhost:8080`) ou para a demonstração publicada.
Consulte [frontend/README.md](frontend/README.md) para configurar o login com
Google e conhecer os comandos disponíveis.

## Documentação e desenvolvimento

Os guias de cada aplicação descrevem tecnologias, configuração, comandos,
testes e publicação. A documentação de requisitos e evolução arquitetural do
monorepo está em [`docs/`](docs/README.md).
