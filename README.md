# Inventory Manager Frontend

Interface web do sistema de estoque inteligente Inventory Manager. Este MVP
oferece autenticação, restauração de sessão, dashboard de indicadores e
consulta de produtos com busca e filtros de situação do estoque.

## Tecnologias

- React, TypeScript e Vite
- React Router e TanStack Query
- Axios, React Hook Form e Zod
- Tailwind CSS e Lucide React
- Vitest, Testing Library e MSW

## Pré-requisitos

- Node.js 22.12 LTS, 24 LTS ou uma versão par mais recente
- npm 10 ou superior
- API do Inventory Manager disponível

## Configuração

Instale as dependências:

```bash
npm install
```

Crie o arquivo local de ambiente a partir do exemplo:

```bash
cp .env.example .env
```

A variável `VITE_API_URL` deve conter a URL base da API, sem a barra final. O
valor de exemplo aponta para o backend publicado:

```dotenv
VITE_API_URL=https://inventory.hanrry.top
```

O backend precisa liberar via CORS a origem em que o frontend estiver sendo
executado, como `http://localhost:5173` durante o desenvolvimento.

## Desenvolvimento

Inicie o servidor local:

```bash
npm run dev
```

As rotas disponíveis no MVP são:

- `/login`: autenticação do usuário;
- `/dashboard`: resumo do estoque e itens que precisam de atenção;
- `/products`: catálogo com busca por nome ou SKU e filtros por situação.

Cadastro e edição de produtos, movimentações, fornecedores, relatórios e
configurações não fazem parte desta primeira versão.

## Qualidade e build

```bash
npm test
npm run lint
npm run typecheck
npm run build
```

O build de produção é gerado em `dist/`. Para executar os testes continuamente
durante o desenvolvimento, use `npm run test:watch`.

## Autenticação

Após o login, o token JWT é armazenado no `localStorage` com a chave
`inventory-manager.token` e enviado nas chamadas autenticadas. Uma resposta
HTTP 401 encerra a sessão local e direciona o usuário de volta ao login.
