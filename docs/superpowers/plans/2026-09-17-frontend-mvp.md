# Inventory Manager Frontend MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar uma SPA responsiva com login, restauração de sessão, dashboard integrado à API e listagem de produtos com busca e filtros.

**Architecture:** O projeto será uma SPA React organizada por feature, com React Router para navegação, TanStack Query para estado remoto e um único cliente Axios para autenticação e tratamento HTTP. A interface usará Tailwind CSS 4 e componentes próprios para reproduzir a referência visual sem exibir funcionalidades ou dados que o backend não oferece.

**Tech Stack:** React, TypeScript, Vite, React Router, TanStack Query, Axios, React Hook Form, Zod, Tailwind CSS 4, Lucide React, Vitest, Testing Library e MSW.

**Spec:** `docs/superpowers/specs/2026-09-17-frontend-mvp-design.md`

## Global Constraints

- O frontend permanece em repositório separado do backend.
- A URL da API vem exclusivamente de `VITE_API_URL`.
- O MVP usa somente `/auth/login`, `/users/me`, `/dashboard/summary` e `/products`.
- O JWT é persistido em `localStorage` sob a chave `inventory-manager.token`.
- `401` encerra a sessão; outros erros temporários não removem um token válido.
- Dados remotos pertencem ao TanStack Query; não adicionar store global.
- Busca e filtros de produtos são locais porque o endpoint não possui paginação.
- Não exibir preço, última atualização, notificações, fornecedores, relatórios ou ações de CRUD.
- Componentes devem preservar navegação por teclado, foco visível e HTML semântico.
- Cada tarefa começa com teste falhando e termina com validação isolada.

---

## Mapa de arquivos

### Fundação

- `package.json`: scripts e dependências.
- `vite.config.ts`: plugins React e Tailwind, além do ambiente de testes.
- `tsconfig*.json`: compilação estrita da aplicação e ferramentas.
- `eslint.config.js`: regras TypeScript, React Hooks e TanStack Query.
- `index.html`: ponto de entrada e metadados básicos.
- `src/main.tsx`: montagem da aplicação.
- `src/styles/index.css`: Tailwind e tokens globais.
- `src/test/setup.ts`: matchers e limpeza de testes.
- `src/test/server.ts`: servidor MSW.
- `src/test/handlers.ts`: respostas HTTP realistas compartilhadas.

### Infraestrutura da aplicação

- `src/app/App.tsx`: raiz dos providers e router.
- `src/app/providers/AppProviders.tsx`: QueryClient e BrowserRouter.
- `src/app/query-client.ts`: política de cache e retry.
- `src/app/router/AppRouter.tsx`: árvore de rotas.
- `src/services/api-client.ts`: Axios e interceptors.
- `src/services/api-error.ts`: normalização de erros.
- `src/services/contracts/*.ts`: contratos do OpenAPI usados pelo MVP.

### Autenticação

- `src/features/auth/auth-api.ts`: login e usuário atual.
- `src/features/auth/auth-storage.ts`: leitura, gravação e remoção do JWT.
- `src/features/auth/AuthProvider.tsx`: sessão e logout.
- `src/features/auth/use-auth.ts`: acesso seguro ao contexto.
- `src/features/auth/ProtectedRoute.tsx`: proteção das rotas internas.
- `src/features/auth/LoginPage.tsx`: formulário e feedback de autenticação.
- `src/features/auth/login-schema.ts`: validação Zod.

### Shell e UI compartilhada

- `src/components/layout/AppLayout.tsx`: estrutura autenticada.
- `src/components/layout/Sidebar.tsx`: navegação desktop e mobile.
- `src/components/layout/AppHeader.tsx`: título, usuário e logout.
- `src/components/ui/StatusBadge.tsx`: estado visual de estoque.
- `src/components/feedback/*`: carregamento, erro e vazio.
- `src/lib/cn.ts`: composição de classes.
- `src/lib/formatters.ts`: moeda, quantidade e role.

### Produtos

- `src/features/products/products-api.ts`: consulta de produtos.
- `src/features/products/product-status.ts`: derivação de status.
- `src/features/products/use-products.ts`: query reutilizável.
- `src/features/products/ProductFilters.tsx`: busca e status.
- `src/features/products/ProductTable.tsx`: tabela reutilizável.
- `src/features/products/ProductsPage.tsx`: página completa.

### Dashboard

- `src/features/dashboard/dashboard-api.ts`: resumo do dashboard.
- `src/features/dashboard/use-dashboard-summary.ts`: query do resumo.
- `src/features/dashboard/SummaryCard.tsx`: cartão de indicador.
- `src/features/dashboard/AttentionPanel.tsx`: itens críticos.
- `src/features/dashboard/DashboardPage.tsx`: composição do painel.

---

### Task 1: Fundação executável e utilitários de domínio

**Files:**
- Create: `package.json`
- Create: `vite.config.ts`
- Create: `tsconfig.json`
- Create: `tsconfig.app.json`
- Create: `tsconfig.node.json`
- Create: `eslint.config.js`
- Create: `index.html`
- Create: `.gitignore`
- Create: `.env.example`
- Create: `src/main.tsx`
- Create: `src/app/App.tsx`
- Create: `src/styles/index.css`
- Create: `src/lib/cn.ts`
- Create: `src/lib/formatters.ts`
- Create: `src/features/products/product-status.ts`
- Create: `src/features/products/product-status.test.ts`
- Create: `src/test/setup.ts`

**Interfaces:**
- Produces: `getProductStatus(totalQuantity: number, minStock: number): InventoryStatus`
- Produces: `formatCurrency(value: number): string`
- Produces: `formatRole(role: string): string`
- Produces: `cn(...inputs: ClassValue[]): string`

- [x] **Step 1: Inicializar o manifesto e instalar dependências**

Run:

```bash
npm init -y
npm install react react-dom react-router-dom @tanstack/react-query axios react-hook-form @hookform/resolvers zod lucide-react clsx tailwind-merge
npm install -D typescript vite @vitejs/plugin-react @types/react @types/react-dom tailwindcss @tailwindcss/vite vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event msw eslint @eslint/js typescript-eslint eslint-plugin-react-hooks eslint-plugin-react-refresh @tanstack/eslint-plugin-query
```

Expected: `package-lock.json` is created and npm reports no failed install.

- [x] **Step 2: Criar configuração mínima do Vite, TypeScript, Tailwind e testes**

Configure `package.json` scripts exactly as:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "lint": "eslint .",
    "typecheck": "tsc -b --pretty false",
    "test": "vitest run",
    "test:watch": "vitest"
  }
}
```

Configure `vite.config.ts` with React, Tailwind, and jsdom:

```ts
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
})
```

Create `.env.example`:

```dotenv
VITE_API_URL=https://inventory.hanrry.top
```

Create `src/styles/index.css` beginning with:

```css
@import "tailwindcss";

:root {
  font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  color: #203127;
  background: #f4fbf6;
  font-synthesis: none;
  text-rendering: optimizeLegibility;
}

body {
  margin: 0;
  min-width: 320px;
  min-height: 100vh;
}

button,
input {
  font: inherit;
}
```

- [x] **Step 3: Escrever teste falhando para a regra visual de status**

Create `src/features/products/product-status.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { getProductStatus } from './product-status'

describe('getProductStatus', () => {
  it.each([
    [0, 10, 'OUT_OF_STOCK'],
    [8, 10, 'LOW_STOCK'],
    [10, 10, 'LOW_STOCK'],
    [11, 10, 'IN_STOCK'],
  ] as const)('maps %s/%s to %s', (quantity, minimum, expected) => {
    expect(getProductStatus(quantity, minimum)).toBe(expected)
  })
})
```

- [x] **Step 4: Executar o teste e confirmar RED**

Run: `npm test -- src/features/products/product-status.test.ts`

Expected: FAIL because `product-status.ts` does not exist.

- [x] **Step 5: Implementar o mínimo e montar a aplicação**

Create `product-status.ts`:

```ts
export type InventoryStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'

export function getProductStatus(totalQuantity: number, minStock: number): InventoryStatus {
  if (totalQuantity <= 0) return 'OUT_OF_STOCK'
  if (totalQuantity <= minStock) return 'LOW_STOCK'
  return 'IN_STOCK'
}
```

Create `main.tsx` and `App.tsx` with a temporary semantic heading so the project
builds. Implement `formatCurrency` with `Intl.NumberFormat('pt-BR', {
style: 'currency', currency: 'BRL' })`, and `formatRole` mapping `ADMIN` to
`Administradora` and `USER` to `Usuária` while returning unknown values intact.

- [x] **Step 6: Validar a fundação**

Run:

```bash
npm test -- src/features/products/product-status.test.ts
npm run typecheck
npm run build
```

Expected: all commands exit 0 and `dist/` is generated.

- [x] **Step 7: Commit**

```bash
git add package.json package-lock.json vite.config.ts tsconfig*.json eslint.config.js index.html .gitignore .env.example src
git commit -m "configura fundacao do frontend"
```

---

### Task 2: Cliente HTTP, sessão e login

**Files:**
- Create: `src/services/contracts/auth.ts`
- Create: `src/services/contracts/user.ts`
- Create: `src/services/api-error.ts`
- Create: `src/services/api-client.ts`
- Create: `src/app/query-client.ts`
- Create: `src/app/providers/AppProviders.tsx`
- Create: `src/app/router/AppRouter.tsx`
- Create: `src/features/auth/auth-api.ts`
- Create: `src/features/auth/auth-storage.ts`
- Create: `src/features/auth/AuthProvider.tsx`
- Create: `src/features/auth/use-auth.ts`
- Create: `src/features/auth/ProtectedRoute.tsx`
- Create: `src/features/auth/login-schema.ts`
- Create: `src/features/auth/LoginPage.tsx`
- Create: `src/features/auth/LoginPage.test.tsx`
- Create: `src/features/auth/ProtectedRoute.test.tsx`
- Create: `src/test/render.tsx`
- Create: `src/test/handlers.ts`
- Create: `src/test/server.ts`
- Modify: `src/test/setup.ts`
- Modify: `src/app/App.tsx`

**Interfaces:**
- Produces: `apiClient: AxiosInstance`
- Produces: `authStorage.getToken/setToken/clearToken`
- Produces: `login(input: LoginRequest): Promise<AuthResponse>`
- Produces: `getCurrentUser(): Promise<User>`
- Produces: `useAuth(): { user, isRestoring, login, logout }`

- [x] **Step 1: Definir handlers MSW com os contratos reais**

Add handlers for `POST */api/v1/auth/login` and `GET */api/v1/users/me`.
Successful login returns `{ token: 'valid-token' }`; current user returns an
`ADMIN` user with all OpenAPI fields. Invalid credentials return status `401`
and a `StandardError`-shaped body.

- [x] **Step 2: Escrever testes falhando de login e rota protegida**

Cover these exact behaviors:

```ts
it('submits valid credentials and navigates to the dashboard')
it('shows the API message for invalid credentials')
it('does not submit an invalid email or a password shorter than six characters')
it('redirects an anonymous visitor to login')
it('restores a stored session through users/me')
```

- [x] **Step 3: Confirmar RED**

Run:

```bash
npm test -- src/features/auth/LoginPage.test.tsx src/features/auth/ProtectedRoute.test.tsx
```

Expected: FAIL because auth components and providers do not exist.

- [x] **Step 4: Implementar contratos, storage e cliente HTTP**

Use these exact types:

```ts
export interface LoginRequest { email: string; password: string }
export interface AuthResponse { token: string }
export interface User {
  id: number
  name: string
  email: string
  role: 'ADMIN' | 'USER'
  createdAt: string
}
```

Use storage key `inventory-manager.token`. Configure Axios with
`baseURL: import.meta.env.VITE_API_URL`, `timeout: 10_000`, and bearer token
injection. Expose a registration function for a single unauthorized callback so
the provider can own logout and navigation behavior without importing React
inside `api-client.ts`.

- [x] **Step 5: Implementar provider, schema e tela de login**

The schema must require a valid email and password with at least six characters.
`AuthProvider` restores `/users/me` only when a token exists. On successful
login, store the token, fetch the current user, then navigate to `/dashboard`.
On logout or `401`, clear token and private query cache.

The login page must include labeled e-mail and password fields, submit loading
state, inline validation, API error feedback, and no demo credentials in source.

- [x] **Step 6: Implementar router inicial**

Route `/login` publicly. Wrap `/dashboard` and `/products` in `ProtectedRoute`.
Use temporary semantic pages for the private routes until their tasks replace
them. Redirect unknown routes according to authentication state.

- [x] **Step 7: Confirmar GREEN e validar tipos**

Run:

```bash
npm test -- src/features/auth/LoginPage.test.tsx src/features/auth/ProtectedRoute.test.tsx
npm run typecheck
```

Expected: tests and type checking pass.

- [x] **Step 8: Commit**

```bash
git add src
git commit -m "implementa autenticacao do frontend"
```

---

### Task 3: Shell responsivo da aplicação

**Files:**
- Create: `src/components/layout/AppLayout.tsx`
- Create: `src/components/layout/Sidebar.tsx`
- Create: `src/components/layout/AppHeader.tsx`
- Create: `src/components/layout/AppLayout.test.tsx`
- Create: `src/components/ui/BrandMark.tsx`
- Create: `src/components/ui/StatusBadge.tsx`
- Create: `src/components/feedback/PageLoader.tsx`
- Create: `src/components/feedback/ErrorState.tsx`
- Create: `src/components/feedback/EmptyState.tsx`
- Modify: `src/app/router/AppRouter.tsx`
- Modify: `src/styles/index.css`

**Interfaces:**
- Produces: `AppLayout` rendering an `<Outlet />`
- Produces: `StatusBadge({ status }: { status: InventoryStatus })`
- Consumes: `useAuth()` and formatting utilities from Tasks 1 and 2.

- [ ] **Step 1: Escrever teste falhando do shell**

Cover:

```ts
it('shows Dashboard and Products navigation without unsupported entries')
it('shows the current user name and formatted role')
it('logs out through the header action')
it('opens and closes mobile navigation with accessible controls')
```

Explicitly assert that Suppliers, Reports, Settings, and Movements are absent.

- [ ] **Step 2: Confirmar RED**

Run: `npm test -- src/components/layout/AppLayout.test.tsx`

Expected: FAIL because `AppLayout` does not exist.

- [ ] **Step 3: Implementar shell e tokens visuais**

Use a 260px persistent sidebar at desktop widths, a top header, and a mobile
drawer controlled by a button with `aria-expanded` and `aria-controls`.
Navigation entries use `NavLink` for active state. The header derives initials
from the user's name and exposes logout as a labeled button.

Add reusable CSS theme values for canvas, surface, border, primary, success,
warning, danger, muted text, card radius, and shadow. Preserve focus rings and
avoid color-only state communication.

- [ ] **Step 4: Conectar rotas privadas ao layout**

Nest `/dashboard` and `/products` under `AppLayout`. Ensure route changes close
the mobile drawer and update the page title displayed by the header.

- [ ] **Step 5: Confirmar GREEN**

Run:

```bash
npm test -- src/components/layout/AppLayout.test.tsx
npm run typecheck
npm run lint
```

Expected: all commands pass.

- [ ] **Step 6: Commit**

```bash
git add src
git commit -m "cria layout responsivo do painel"
```

---

### Task 4: Listagem, busca e filtros de produtos

**Files:**
- Create: `src/services/contracts/product.ts`
- Create: `src/features/products/products-api.ts`
- Create: `src/features/products/use-products.ts`
- Create: `src/features/products/ProductFilters.tsx`
- Create: `src/features/products/ProductTable.tsx`
- Create: `src/features/products/ProductsPage.tsx`
- Create: `src/features/products/ProductsPage.test.tsx`
- Modify: `src/test/handlers.ts`
- Modify: `src/app/router/AppRouter.tsx`

**Interfaces:**
- Produces: `Product` matching the OpenAPI response.
- Produces: `useProducts()` backed by query key `['products']`.
- Produces: `ProductTable({ products, compact? })`.
- Consumes: `getProductStatus` and shared feedback components.

- [ ] **Step 1: Adicionar resposta MSW de produtos**

Use at least three products: one in stock, one low stock, and one out of stock.
Every object includes `id`, `name`, `sku`, `totalQuantity`, `categoryName`, and
`minStock` exactly as the OpenAPI schema.

- [ ] **Step 2: Escrever testes falhando da página**

Cover:

```ts
it('renders product data returned by the API')
it('searches by product name case-insensitively')
it('searches by SKU case-insensitively')
it('filters products by derived stock status')
it('distinguishes an empty catalog from no matching results')
it('shows an error with a retry action when the API fails')
```

Assert that price, updated time, and product CRUD buttons are absent.

- [ ] **Step 3: Confirmar RED**

Run: `npm test -- src/features/products/ProductsPage.test.tsx`

Expected: FAIL because products feature components do not exist.

- [ ] **Step 4: Implementar contrato, query e filtragem**

Use:

```ts
export interface Product {
  id: number
  name: string
  sku: string
  totalQuantity: number
  categoryName: string
  minStock: number
}
```

Fetch `GET /api/v1/products`. Keep API results unchanged in the query cache.
Derive filtered results with `useMemo` from normalized search text and selected
status. Keep the filter value in the URL query parameter `status` and the search
term in `q` so refresh and back navigation preserve the view.

- [ ] **Step 5: Implementar tabela e estados**

Use semantic table markup with columns Product, Category, Quantity, Minimum, and
Status. Render name and SKU together. `compact` may limit visible rows but must
not alter cached data. Provide accessible labels for search and filter controls.

- [ ] **Step 6: Confirmar GREEN**

Run:

```bash
npm test -- src/features/products/ProductsPage.test.tsx
npm run typecheck
npm run lint
```

Expected: all commands pass.

- [ ] **Step 7: Commit**

```bash
git add src
git commit -m "adiciona consulta e filtros de produtos"
```

---

### Task 5: Dashboard integrado à API

**Files:**
- Create: `src/services/contracts/dashboard.ts`
- Create: `src/features/dashboard/dashboard-api.ts`
- Create: `src/features/dashboard/use-dashboard-summary.ts`
- Create: `src/features/dashboard/SummaryCard.tsx`
- Create: `src/features/dashboard/AttentionPanel.tsx`
- Create: `src/features/dashboard/DashboardPage.tsx`
- Create: `src/features/dashboard/DashboardPage.test.tsx`
- Modify: `src/test/handlers.ts`
- Modify: `src/app/router/AppRouter.tsx`

**Interfaces:**
- Produces: `DashboardSummary` and `AttentionItem` matching OpenAPI.
- Produces: `useDashboardSummary()` with query key `['dashboard-summary']`.
- Consumes: `useProducts()`, `ProductTable`, `formatCurrency`, and feedback UI.

- [ ] **Step 1: Adicionar resposta MSW do resumo**

Return all fields from `DashboardSummaryDTO`, including `expiredBatchCount`, and
attention items using only `IN_STOCK`, `LOW_STOCK`, or `OUT_OF_STOCK`.

- [ ] **Step 2: Escrever testes falhando do dashboard**

Cover:

```ts
it('renders the four primary summary cards with formatted values')
it('shows product count as support text for total quantity')
it('renders attention items with current and minimum quantities')
it('renders a compact product preview from the products query')
it('keeps summary and product errors isolated with retry actions')
```

Assert BRL currency formatting and the absence of unsupported price and update
columns.

- [ ] **Step 3: Confirmar RED**

Run: `npm test -- src/features/dashboard/DashboardPage.test.tsx`

Expected: FAIL because dashboard components do not exist.

- [ ] **Step 4: Implementar contrato, query e cartões**

Use exact OpenAPI fields and a discriminated `InventoryStatus` for attention
items. Build cards for total quantity, low stock, out of stock, and inventory
value. Use Lucide icons with hidden decorative SVG semantics or accessible
labels where the icon itself triggers an action.

- [ ] **Step 5: Implementar painel de atenção e preview**

Calculate progress display as:

```ts
const progress = minStock <= 0
  ? 100
  : Math.min(100, Math.max(0, (quantity / minStock) * 100))
```

Render readable quantities beside the bar so color and bar length are not the
only status indicators. Use `ProductTable` in compact mode for the dashboard.

- [ ] **Step 6: Implementar responsividade da composição**

Use four columns at large desktop widths, two on tablet, and one on mobile.
Below the cards, use a wider products panel and narrower attention panel on
desktop, stacking them on narrower screens.

- [ ] **Step 7: Confirmar GREEN**

Run:

```bash
npm test -- src/features/dashboard/DashboardPage.test.tsx
npm run typecheck
npm run lint
```

Expected: all commands pass.

- [ ] **Step 8: Commit**

```bash
git add src
git commit -m "implementa dashboard de estoque"
```

---

### Task 6: Validação integrada, documentação e acabamento

**Files:**
- Create: `src/app/App.test.tsx`
- Modify: `README.md`
- Modify: `.env.example`
- Modify: visual files only where validation identifies a concrete defect

**Interfaces:**
- Consumes: all public components and flows from Tasks 1–5.
- Produces: a documented and production-buildable MVP.

- [ ] **Step 1: Escrever teste integrado falhando**

Test the complete user path with MSW:

```text
anonymous access -> login -> dashboard -> products -> logout -> login
```

The test must assert route changes, current-user rendering, real dashboard
values, product search, and token removal after logout.

- [ ] **Step 2: Confirmar RED**

Run: `npm test -- src/app/App.test.tsx`

Expected: FAIL until missing integration wiring or accessibility defects are
resolved.

- [ ] **Step 3: Corrigir somente falhas comprovadas pelo fluxo**

Keep corrections scoped to route/provider wiring, query cleanup, accessible
labels, focus behavior, and responsive navigation demonstrated by the failing
test. Do not add new screens or backend operations.

- [ ] **Step 4: Atualizar README**

Document prerequisites, `npm install`, `.env` creation, `npm run dev`, test,
lint, typecheck, build, backend URL configuration, and the MVP route list.
State that the deployed backend must allow the frontend origin through CORS.

- [ ] **Step 5: Executar a validação completa**

Run:

```bash
npm test
npm run lint
npm run typecheck
npm run build
git diff --check
```

Expected: all commands exit 0, all tests pass, and `dist/` is generated without
being tracked.

- [ ] **Step 6: Revisar manualmente em viewport desktop e mobile**

Run: `npm run dev`

Verify at approximately 1440px and 390px widths: login usability, sidebar or
drawer navigation, card wrapping, attention panel, horizontal table behavior,
focus visibility, error retry, and logout.

- [ ] **Step 7: Commit**

```bash
git add README.md .env.example src
git commit -m "finaliza primeira versao do frontend"
```

## Final review gate

After Task 6, run a code review over the complete diff. Report findings by
severity, test evidence, known limitations, and any mismatch with
`docs/superpowers/specs/2026-09-17-frontend-mvp-design.md`. Do not push without
explicit authorization.
