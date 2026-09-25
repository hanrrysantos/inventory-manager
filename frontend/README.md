# EstoqueHub

Interface web para acompanhamento de estoque, com autenticação, indicadores e
consulta de produtos. Frontend e API são mantidos no mesmo monorepo.

[Acessar aplicação](https://controle-de-estoque.hanrry.top) ·
[Documentação da API](https://api-controle-de-estoque.hanrry.top/swagger-ui/index.html) ·
[Código do backend](../backend)

## Funcionalidades

| Tela | Recursos |
| --- | --- |
| Landing (`/`) | Apresentação do EstoqueHub, problema de controle manual e recursos reais de indicadores, alertas e busca de produtos |
| Acesso (`/login`) | Login por e-mail/senha ou Google, vínculo explícito de conta e criação de conta |
| Dashboard (`/dashboard`) | Indicadores de estoque e itens que precisam de atenção |
| Produtos (`/products`) | Consulta paginada, ordenação, estoque baixo, detalhes e administração por `ADMIN` |
| Categorias (`/categories`) | Consulta paginada, detalhes e administração por `ADMIN` |

Interface responsiva com estados de carregamento, erro e lista vazia.
O cadastro de conta envia `POST /api/v1/auth/register` e, após a conclusão,
retorna ao acesso para o login regular. Busca textual e filtros por
categoria/status, movimentações, fornecedores, relatórios e configurações
ainda não estão disponíveis nesta versão.

## Tecnologias

React, TypeScript e Vite; Tailwind CSS e Lucide React; React Router,
TanStack Query e Axios; React Hook Form e Zod. Testes com Vitest,
Testing Library e MSW.

## Executar localmente

Requisitos: Node.js 22.12+ da linha 22 ou Node.js 24, npm e acesso à API.

```bash
git clone https://github.com/hanrrysantos/inventory-manager.git
cd inventory-manager/frontend
npm ci
cp .env.example .env
npm run dev
```

Acesse o endereço informado pelo Vite, normalmente `http://localhost:5173`.
O `.env` define a URL base do backend, sem barra final:

```dotenv
VITE_API_URL=https://api-controle-de-estoque.hanrry.top
VITE_GOOGLE_CLIENT_ID=1234567890-abc123.apps.googleusercontent.com
```

Para usar a API local, altere a URL para `http://localhost:8080` e reinicie
o Vite. O backend precisa permitir a origem do frontend em `FRONTEND_ORIGINS`.
Configure `VITE_GOOGLE_CLIENT_ID` com o client ID Web do mesmo projeto Google
usado pelo backend. Adicione a origem local e a publicada nas *Authorized
JavaScript origins* do Google Cloud Console. Esse valor é público; nunca use
ou exponha um client secret no frontend.

Entre com uma conta cadastrada na API. O JWT fica no `localStorage` e é enviado
como `Bearer` nas requisições; respostas HTTP 401 encerram a sessão.
A API publicada pode demorar a responder após inatividade; o cliente aguarda
até 90 segundos por requisição.

## Comandos

| Comando | Finalidade |
| --- | --- |
| `npm run dev` | Iniciar o ambiente de desenvolvimento |
| `npm test` | Executar os testes |
| `npm run test:watch` | Executar testes em modo contínuo |
| `npm run lint` | Verificar o código com ESLint |
| `npm run typecheck` | Verificar tipos TypeScript |
| `npm run build` | Verificar tipos e gerar o build em `dist/` |

## Publicação

O frontend é hospedado na Vercel, em
[controle-de-estoque.hanrry.top](https://controle-de-estoque.hanrry.top).
Configuração do projeto:

- Framework: **Vite**; build: `npm run build`; saída: `dist`.
- Variáveis: `VITE_API_URL=https://api-controle-de-estoque.hanrry.top` e
  `VITE_GOOGLE_CLIENT_ID=<client-id-web-do-google>`.
- Rotas: o [vercel.json](vercel.json) direciona acessos da SPA para `index.html`.

Configure `VITE_API_URL` nos ambientes usados na Vercel e faça um novo deploy
quando alterar o valor, pois ele é incorporado ao build. Variáveis `VITE_*`
são públicas: não use senhas ou secrets nelas.

No **backend**, libere as origens necessárias via CORS, separadas por vírgula:

```dotenv
FRONTEND_ORIGINS=http://localhost:5173,https://controle-de-estoque.hanrry.top
```

URLs de Preview têm origens diferentes e precisam de liberação própria para
acessar a API pelo navegador. Arquivos `.env` locais não devem ser versionados.
