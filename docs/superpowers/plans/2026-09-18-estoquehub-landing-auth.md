# EstoqueHub Landing and Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the temporary Verdejar entry experience with the EstoqueHub public landing page and a single access card that supports regular login, future Google login feedback, and real account registration.

**Architecture:** Add a lazy public `/` route composed from focused landing components and keep `/login` as the only access route. Split login and registration into focused forms that share one password field component; keep authentication state in the existing provider and call registration directly through `auth-api` because registration does not create a session.

**Tech Stack:** React 19, TypeScript 6, React Router 7, React Hook Form, Zod 4, Axios, Tailwind CSS 4, Lucide React, Vitest, Testing Library, and MSW.

**Spec:** `docs/superpowers/specs/2026-09-18-estoquehub-landing-auth-design.md`

## Global Constraints

- Work directly on `main`; do not create a feature branch or Git worktree.
- Preserve the protected `/dashboard` and `/products` behavior and JWT session restoration.
- Keep one public landing route at `/`; problem and features are in-page sections, not routes.
- The landing page has exactly one access CTA, `Acesse a plataforma`, in the header.
- Do not add pricing, legal, blog, contact, or Google OAuth screens.
- Google remains a clickable informational action and must not make a network request.
- Registration uses `POST /api/v1/auth/register` with `name`, `email`, and `password` and does not authenticate the new user.
- Use the approved `src/assets/estoquehub-logo.png` asset; do not regenerate it.
- Write each behavior test first, run it red, implement the minimum production change, and run it green.
- Keep all touch targets at least 44 pixels and all feedback keyboard/screen-reader accessible.

---

### Task 1: Establish the EstoqueHub identity

**Files:**
- Create: `src/components/ui/BrandMark.test.tsx`
- Modify: `src/components/ui/BrandMark.tsx`
- Modify: `src/components/layout/AppHeader.tsx`
- Modify: `src/components/layout/AppLayout.test.tsx`
- Add existing asset: `src/assets/estoquehub-logo.png`

**Interfaces:**
- Consumes: approved transparent logo file at `src/assets/estoquehub-logo.png`.
- Produces: `BrandMark({ inverse?: boolean }: BrandMarkProps): JSX.Element`, reused by landing, access, sidebar, and footer.

- [ ] **Step 1: Install dependencies and establish the baseline**

Run:

```bash
npm ci
npm test
```

Expected: the existing suite passes before production changes. If it does not, stop and diagnose the baseline instead of changing feature code.

- [ ] **Step 2: Write the failing brand tests**

Create `src/components/ui/BrandMark.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { BrandMark } from './BrandMark'

describe('BrandMark', () => {
  it('shows the EstoqueHub identity with a decorative logo', () => {
    const { container } = render(<BrandMark />)

    expect(screen.getByText('EstoqueHub')).toBeVisible()
    expect(screen.getByText('Seu estoque, sempre sob controle')).toBeVisible()
    expect(container.querySelector('img')).toHaveAttribute(
      'src',
      expect.stringContaining('estoquehub-logo'),
    )
  })

  it('supports the inverse treatment used on dark surfaces', () => {
    render(<BrandMark inverse />)

    expect(screen.getByTestId('brand-wordmark')).toHaveClass('text-white')
  })
})
```

In `src/components/layout/AppLayout.test.tsx`, add this assertion to the supported-navigation test after the navigation is found:

```tsx
expect(screen.getAllByText('EstoqueHub').length).toBeGreaterThan(0)
expect(screen.queryByText('Verdejar')).not.toBeInTheDocument()
```

- [ ] **Step 3: Run the focused tests and verify red**

Run:

```bash
npx vitest run src/components/ui/BrandMark.test.tsx src/components/layout/AppLayout.test.tsx
```

Expected: FAIL because `BrandMark` still renders `Verdejar` and has no `inverse` prop.

- [ ] **Step 4: Implement the reusable brand mark**

Replace `src/components/ui/BrandMark.tsx` with:

```tsx
import logoUrl from '../../assets/estoquehub-logo.png'
import { cn } from '../../lib/cn'

interface BrandMarkProps {
  inverse?: boolean
}

export function BrandMark({ inverse = false }: BrandMarkProps) {
  return (
    <div className="flex items-center gap-3">
      <img className="size-11 object-contain" src={logoUrl} alt="" />
      <div>
        <p
          className={cn('font-semibold tracking-tight text-[#173b27]', inverse && 'text-white')}
          data-testid="brand-wordmark"
        >
          EstoqueHub
        </p>
        <p className={cn('text-xs text-[#66796d]', inverse && 'text-white/65')}>
          Seu estoque, sempre sob controle
        </p>
      </div>
    </div>
  )
}
```

In `src/components/layout/AppHeader.tsx`, replace the fallback title:

```tsx
const title = pageTitles[pathname] ?? 'EstoqueHub'
```

- [ ] **Step 5: Run the focused tests and verify green**

Run:

```bash
npx vitest run src/components/ui/BrandMark.test.tsx src/components/layout/AppLayout.test.tsx
```

Expected: PASS.

- [ ] **Step 6: Commit the identity foundation**

```bash
git add src/assets/estoquehub-logo.png src/components/ui/BrandMark.tsx src/components/ui/BrandMark.test.tsx src/components/layout/AppHeader.tsx src/components/layout/AppLayout.test.tsx
git commit -m "aplica identidade estoquehub"
```

---

### Task 2: Build the public landing page and routing

**Files:**
- Create: `src/features/landing/LandingPage.tsx`
- Create: `src/features/landing/LandingHeader.tsx`
- Create: `src/features/landing/ProductPreview.tsx`
- Create: `src/features/landing/LandingFooter.tsx`
- Create: `src/features/landing/LandingPage.test.tsx`
- Modify: `src/app/router/AppRouter.tsx`

**Interfaces:**
- Consumes: `BrandMark({ inverse?: boolean })` from Task 1 and React Router's `Link`.
- Produces: `LandingPage(): JSX.Element`, loaded at `/`; section IDs `problema` and `funcionalidades`; exactly one `Acesse a plataforma` link to `/login`.

- [ ] **Step 1: Write the failing landing and routing tests**

Create `src/features/landing/LandingPage.test.tsx`:

```tsx
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from '../../app/App'

describe('EstoqueHub landing page', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/')
  })

  it('presents the product with problem and feature sections', async () => {
    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: /controle seu estoque sem perder tempo/i,
      }),
    ).toBeVisible()
    expect(screen.getByRole('heading', { name: /o estoque não pode depender de adivinhação/i })).toBeVisible()
    expect(screen.getByRole('heading', { name: /o essencial para decidir com clareza/i })).toBeVisible()
    expect(document.querySelector('#problema')).toBeInTheDocument()
    expect(document.querySelector('#funcionalidades')).toBeInTheDocument()
    expect(screen.queryByText(/planos/i)).not.toBeInTheDocument()
  })

  it('has one access call to action and opens the existing login route', async () => {
    const user = userEvent.setup()
    render(<App />)

    const header = await screen.findByRole('banner')
    const accessLinks = screen.getAllByRole('link', { name: 'Acesse a plataforma' })
    expect(accessLinks).toHaveLength(1)
    expect(within(header).getByRole('link', { name: 'Acesse a plataforma' })).toBe(accessLinks[0])

    await user.click(accessLinks[0])

    expect(window.location.pathname).toBe('/login')
    expect(await screen.findByRole('heading', { name: /acesse sua conta/i })).toBeVisible()
  })

  it('sends an unknown anonymous route back to the landing page', async () => {
    window.history.pushState({}, '', '/nao-existe')
    render(<App />)

    expect(await screen.findByRole('heading', { name: /controle seu estoque sem perder tempo/i })).toBeVisible()
    expect(window.location.pathname).toBe('/')
  })
})
```

- [ ] **Step 2: Run the landing test and verify red**

Run:

```bash
npx vitest run src/features/landing/LandingPage.test.tsx
```

Expected: FAIL because `/` redirects to `/login` and the landing components do not exist.

- [ ] **Step 3: Create the header with the only CTA**

Create `src/features/landing/LandingHeader.tsx`:

```tsx
import { Link } from 'react-router-dom'
import { BrandMark } from '../../components/ui/BrandMark'

export function LandingHeader() {
  return (
    <header className="sticky top-0 z-50 border-b border-[#dfeae2] bg-white/90 backdrop-blur-xl">
      <div className="mx-auto flex min-h-20 max-w-7xl items-center justify-between gap-5 px-5 md:px-8">
        <a href="#inicio" aria-label="EstoqueHub — início">
          <BrandMark />
        </a>
        <nav className="hidden items-center gap-8 text-sm font-medium text-[#52655a] md:flex" aria-label="Navegação da página inicial">
          <a className="transition hover:text-[#16834b]" href="#problema">Problema</a>
          <a className="transition hover:text-[#16834b]" href="#funcionalidades">Funcionalidades</a>
        </nav>
        <Link className="inline-flex min-h-11 items-center rounded-full bg-[#168f50] px-5 text-sm font-semibold text-white shadow-lg shadow-green-900/10 transition hover:bg-[#107842]" to="/login">
          Acesse a plataforma
        </Link>
      </div>
    </header>
  )
}
```

- [ ] **Step 4: Create the decorative notebook and phone preview**

Create `src/features/landing/ProductPreview.tsx` with a decorative root and fixed, real-product labels:

```tsx
import { AlertTriangle, PackageCheck, TrendingUp } from 'lucide-react'

const metrics = [
  { label: 'Total de itens', value: '1.284', icon: PackageCheck },
  { label: 'Estoque baixo', value: '12', icon: AlertTriangle },
  { label: 'Valor em estoque', value: 'R$ 48 mil', icon: TrendingUp },
]

export function ProductPreview() {
  return (
    <div className="relative mx-auto w-full max-w-[680px]" aria-hidden="true">
      <div className="overflow-hidden rounded-[28px] border-[10px] border-[#17251d] bg-[#f4faf6] shadow-[0_32px_90px_rgba(24,88,50,0.20)]">
        <div className="flex h-11 items-center gap-2 border-b border-[#dce8df] bg-white px-5">
          <span className="size-2.5 rounded-full bg-[#ff766f]" />
          <span className="size-2.5 rounded-full bg-[#f5c95c]" />
          <span className="size-2.5 rounded-full bg-[#4dc879]" />
          <span className="ml-3 text-xs font-semibold text-[#31523d]">EstoqueHub</span>
        </div>
        <div className="p-5 sm:p-7">
          <p className="text-xs font-medium uppercase tracking-[0.18em] text-[#6b7d71]">Painel de estoque</p>
          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            {metrics.map(({ label, value, icon: Icon }) => (
              <div className="rounded-2xl border border-[#dce8df] bg-white p-4" key={label}>
                <Icon className="size-5 text-[#1b9a58]" />
                <p className="mt-3 text-xs text-[#6b7d71]">{label}</p>
                <p className="mt-1 text-lg font-semibold text-[#203127]">{value}</p>
              </div>
            ))}
          </div>
          <div className="mt-4 rounded-2xl border border-[#dce8df] bg-white p-5">
            <div className="flex items-center justify-between"><span className="text-sm font-semibold">Produtos que precisam de atenção</span><span className="text-xs text-[#168f50]">Ver catálogo</span></div>
            <div className="mt-5 space-y-3">
              <div className="h-3 w-full rounded-full bg-[#eef5f0]" />
              <div className="h-3 w-4/5 rounded-full bg-[#d9eee0]" />
              <div className="h-3 w-3/5 rounded-full bg-[#bce3c9]" />
            </div>
          </div>
        </div>
      </div>
      <div className="absolute -bottom-12 -left-3 w-[180px] rounded-[30px] border-[8px] border-[#17251d] bg-white p-4 shadow-[0_22px_55px_rgba(22,66,38,0.24)] sm:-left-10 sm:w-[210px]">
        <div className="mx-auto mb-4 h-1.5 w-14 rounded-full bg-[#17251d]" />
        <p className="text-xs text-[#6b7d71]">Estoque baixo</p>
        <p className="mt-1 text-3xl font-semibold text-[#203127]">12</p>
        <div className="mt-5 h-24 rounded-2xl bg-[linear-gradient(135deg,#e6f6eb,#bde7ca)] p-3">
          <div className="mt-8 flex items-end gap-2"><span className="h-5 w-5 rounded bg-[#74cd8f]" /><span className="h-9 w-5 rounded bg-[#45b96b]" /><span className="h-14 w-5 rounded bg-[#168f50]" /></div>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 5: Build the landing composition and footer**

Create `src/features/landing/LandingFooter.tsx`:

```tsx
import { BrandMark } from '../../components/ui/BrandMark'

export function LandingFooter() {
  return (
    <footer className="bg-[#0e1b13] text-white">
      <div className="mx-auto grid max-w-7xl gap-10 px-5 py-14 md:grid-cols-[1.5fr_1fr] md:px-8">
        <div className="max-w-sm"><BrandMark inverse /><p className="mt-5 text-sm leading-7 text-white/60">Controle simples para pequenos comércios acompanharem produtos, quantidades e alertas em um só lugar.</p></div>
        <nav className="md:justify-self-end" aria-label="Atalhos do rodapé">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-white/40">Navegação</p>
          <div className="mt-4 flex flex-col gap-3 text-sm text-white/70"><a href="#problema">Problema</a><a href="#funcionalidades">Funcionalidades</a></div>
        </nav>
      </div>
      <div className="mx-auto max-w-7xl border-t border-white/10 px-5 py-6 text-xs text-white/40 md:px-8">© 2026 EstoqueHub. Todos os direitos reservados.</div>
    </footer>
  )
}
```

Create `src/features/landing/LandingPage.tsx` using these exact content collections:

```tsx
import { AlertTriangle, BarChart3, PackageSearch, Search } from 'lucide-react'
import { LandingFooter } from './LandingFooter'
import { LandingHeader } from './LandingHeader'
import { ProductPreview } from './ProductPreview'

const problems = [
  ['Planilhas não acompanham o ritmo', 'Atualizações manuais deixam informações importantes espalhadas e atrasadas.'],
  ['Falta de visibilidade custa caro', 'Sem números claros, faltas e excessos de estoque só aparecem quando já viraram problema.'],
  ['Reposição no improviso', 'A ausência de alertas transforma decisões simples em urgências recorrentes.'],
] as const

const features = [
  { title: 'Visão geral', description: 'Indicadores de quantidade, produtos críticos e valor estimado em um painel direto.', icon: BarChart3 },
  { title: 'Catálogo organizado', description: 'Produtos, SKUs e quantidades reunidos para consulta rápida.', icon: PackageSearch },
  { title: 'Alertas para agir', description: 'Destaques para estoque baixo e produtos em falta antes que afetem a operação.', icon: AlertTriangle },
  { title: 'Busca e filtros', description: 'Encontre itens por nome ou SKU e filtre pela situação do estoque.', icon: Search },
]

export function LandingPage() {
  return (
    <div className="min-h-screen bg-[#fbfdfb] text-[#18281e]">
      <LandingHeader />
      <main>
        <section id="inicio" className="overflow-hidden bg-[linear-gradient(rgba(34,120,65,0.045)_1px,transparent_1px),linear-gradient(90deg,rgba(34,120,65,0.045)_1px,transparent_1px)] bg-[size:56px_56px]">
          <div className="mx-auto grid min-h-[720px] max-w-7xl items-center gap-16 px-5 py-20 lg:grid-cols-[0.82fr_1.18fr] lg:px-8">
            <div><p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#168f50]">Controle de estoque para pequenos comércios</p><h1 className="mt-5 max-w-xl text-5xl font-bold leading-[1.04] tracking-[-0.045em] sm:text-6xl">Controle seu estoque sem perder tempo.</h1><p className="mt-6 max-w-lg text-lg leading-8 text-[#617168]">Acompanhe produtos, quantidades e alertas em um painel simples para decidir com clareza e manter sua operação em movimento.</p><div className="mt-8 flex flex-wrap gap-x-7 gap-y-3 text-sm text-[#52655a]"><span>✓ Visão clara do estoque</span><span>✓ Alertas para agir antes</span></div></div>
            <div className="pb-14"><ProductPreview /></div>
          </div>
        </section>
        <section id="problema" className="scroll-mt-24 px-5 py-24 md:px-8"><div className="mx-auto max-w-7xl"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#168f50]">Problema</p><h2 className="mt-4 max-w-2xl text-4xl font-bold tracking-[-0.035em]">O estoque não pode depender de adivinhação</h2><div className="mt-12 grid gap-5 md:grid-cols-3">{problems.map(([title, description], index) => <article className="rounded-[28px] border border-[#dce8df] bg-white p-7 shadow-sm" key={title}><span className="text-sm font-semibold text-[#168f50]">0{index + 1}</span><h3 className="mt-8 text-xl font-semibold">{title}</h3><p className="mt-3 leading-7 text-[#68786f]">{description}</p></article>)}</div></div></section>
        <section id="funcionalidades" className="scroll-mt-24 bg-[#eef7f1] px-5 py-24 md:px-8"><div className="mx-auto max-w-7xl"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#168f50]">Funcionalidades</p><h2 className="mt-4 max-w-2xl text-4xl font-bold tracking-[-0.035em]">O essencial para decidir com clareza</h2><div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">{features.map(({ title, description, icon: Icon }) => <article className="rounded-[28px] bg-white p-7" key={title}><span className="grid size-12 place-items-center rounded-2xl bg-[#dcf2e3] text-[#168f50]"><Icon aria-hidden="true" /></span><h3 className="mt-6 text-xl font-semibold">{title}</h3><p className="mt-3 leading-7 text-[#68786f]">{description}</p></article>)}</div></div></section>
      </main>
      <LandingFooter />
    </div>
  )
}
```

- [ ] **Step 6: Register the lazy public route and anonymous fallback**

In `src/app/router/AppRouter.tsx`, add the lazy import:

```tsx
const LandingPage = lazy(() =>
  import('../../features/landing/LandingPage').then(({ LandingPage }) => ({
    default: LandingPage,
  })),
)
```

Add the public route before `/login`:

```tsx
<Route path="/" element={<LandingPage />} />
```

Change only the catch-all destination:

```tsx
<Navigate to={user ? '/dashboard' : '/'} replace />
```

- [ ] **Step 7: Run focused and routing tests**

Run:

```bash
npx vitest run src/features/landing/LandingPage.test.tsx src/app/App.test.tsx src/features/auth/auth-flow.test.tsx
```

Expected: landing tests PASS; existing auth and application flow tests remain PASS because protected routes still redirect anonymous visitors to `/login`.

- [ ] **Step 8: Commit the public experience**

```bash
git add src/features/landing src/app/router/AppRouter.tsx
git commit -m "cria landing page da estoquehub"
```

---

### Task 3: Add the registration contract and validation

**Files:**
- Create: `src/features/auth/register-schema.ts`
- Create: `src/features/auth/register-schema.test.ts`
- Create: `src/features/auth/auth-api.test.ts`
- Modify: `src/services/contracts/auth.ts`
- Modify: `src/features/auth/auth-api.ts`
- Modify: `src/test/handlers.ts`

**Interfaces:**
- Consumes: existing `apiClient` and backend `POST /api/v1/auth/register` contract.
- Produces: `RegisterRequest`, `RegisterResponse`, `RegisterFormData`, and `registerAccount(input: RegisterRequest): Promise<RegisterResponse>`.

- [ ] **Step 1: Write failing schema tests**

Create `src/features/auth/register-schema.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { registerSchema } from './register-schema'

describe('registerSchema', () => {
  it('accepts the backend registration contract', () => {
    expect(registerSchema.safeParse({ name: 'Maria Silva', email: 'maria@example.com', password: '123456' }).success).toBe(true)
  })

  it('rejects blank name, invalid email, and short password', () => {
    const result = registerSchema.safeParse({ name: ' ', email: 'invalid', password: '123' })
    expect(result.success).toBe(false)
    if (!result.success) expect(result.error.issues.map((issue) => issue.path[0])).toEqual(['name', 'email', 'password'])
  })
})
```

- [ ] **Step 2: Run the schema test and verify red**

Run:

```bash
npx vitest run src/features/auth/register-schema.test.ts
```

Expected: FAIL because `register-schema.ts` does not exist.

- [ ] **Step 3: Implement the registration schema**

Create `src/features/auth/register-schema.ts`:

```ts
import { z } from 'zod'

export const registerSchema = z.object({
  name: z.string().trim().min(1, 'Informe seu nome'),
  email: z.email('Informe um e-mail válido'),
  password: z.string().min(6, 'A senha deve ter pelo menos 6 caracteres'),
})

export type RegisterFormData = z.infer<typeof registerSchema>
```

Run `npx vitest run src/features/auth/register-schema.test.ts`; expected: PASS.

- [ ] **Step 4: Write the failing registration API test**

Create `src/features/auth/auth-api.test.ts`:

```ts
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server'
import { registerAccount } from './auth-api'

describe('registerAccount', () => {
  it('posts the backend contract and returns the created user', async () => {
    let submittedBody: unknown
    server.use(
      http.post('*/api/v1/auth/register', async ({ request }) => {
        submittedBody = await request.json()
        return HttpResponse.json(
          { id: 2, name: 'Maria Silva', email: 'maria@example.com', role: 'USER', createdAt: '2026-09-18T12:00:00' },
          { status: 201 },
        )
      }),
    )

    const result = await registerAccount({ name: 'Maria Silva', email: 'maria@example.com', password: '123456' })

    expect(submittedBody).toEqual({ name: 'Maria Silva', email: 'maria@example.com', password: '123456' })
    expect(result.email).toBe('maria@example.com')
  })
})
```

- [ ] **Step 5: Run the API test and verify red**

Run:

```bash
npx vitest run src/features/auth/auth-api.test.ts
```

Expected: FAIL because `registerAccount` is not exported.

- [ ] **Step 6: Implement contracts, API call, and default MSW response**

Append to `src/services/contracts/auth.ts`:

```ts
export interface RegisterRequest {
  name: string
  email: string
  password: string
}

export interface RegisterResponse {
  id: number
  name: string
  email: string
  role: 'ADMIN' | 'USER'
  createdAt: string
}
```

Update the type import in `src/features/auth/auth-api.ts` and add:

```ts
export async function registerAccount(
  input: RegisterRequest,
): Promise<RegisterResponse> {
  const { data } = await apiClient.post<RegisterResponse>(
    '/api/v1/auth/register',
    input,
  )
  return data
}
```

Add this handler before the login handler in `src/test/handlers.ts`:

```ts
http.post('*/api/v1/auth/register', async ({ request }) => {
  const input = (await request.json()) as { name: string; email: string; password: string }
  return HttpResponse.json(
    { id: 2, name: input.name, email: input.email, role: 'USER', createdAt: '2026-09-18T12:00:00' },
    { status: 201, headers: { Location: '/api/v1/auth/register/2' } },
  )
}),
```

- [ ] **Step 7: Run registration data tests and verify green**

Run:

```bash
npx vitest run src/features/auth/register-schema.test.ts src/features/auth/auth-api.test.ts
```

Expected: PASS.

- [ ] **Step 8: Commit the registration data layer**

```bash
git add src/features/auth/register-schema.ts src/features/auth/register-schema.test.ts src/features/auth/auth-api.ts src/features/auth/auth-api.test.ts src/services/contracts/auth.ts src/test/handlers.ts
git commit -m "adiciona contrato de cadastro"
```

---

### Task 4: Rebuild the access card with login, Google notice, and registration

**Files:**
- Create: `src/features/auth/PasswordField.tsx`
- Create: `src/features/auth/LoginForm.tsx`
- Create: `src/features/auth/RegisterForm.tsx`
- Modify: `src/features/auth/LoginPage.tsx`
- Modify: `src/features/auth/auth-flow.test.tsx`

**Interfaces:**
- Consumes: `loginSchema`, `registerSchema`, `registerAccount`, `useAuth().login`, `getApiErrorMessage`, and `BrandMark`.
- Produces: one `/login` card with `login | register` local mode; `PasswordField` with show/hide behavior; `RegisterForm({ onSuccess, onBack })`; `LoginForm({ initialEmail, onCreateAccount, onGoogleUnavailable })`.

- [ ] **Step 1: Add failing interaction tests**

In the existing login tests, replace every broad password query
`getByLabelText(/senha/i)` with `getByLabelText('Senha')`, and replace every
login submit query `getByRole('button', { name: /entrar/i })` with
`getByRole('button', { name: 'Entrar' })`. The new visibility and Google
controls also contain those words, so exact accessible names prevent false
ambiguity.

Append these tests inside `describe('authentication flow')` in `src/features/auth/auth-flow.test.tsx`:

```tsx
it('shows and hides the login password accessibly', async () => {
  const user = userEvent.setup()
  render(<App />)
  const password = await screen.findByLabelText('Senha')
  expect(password).toHaveAttribute('type', 'password')
  await user.click(screen.getByRole('button', { name: 'Mostrar senha' }))
  expect(password).toHaveAttribute('type', 'text')
  await user.click(screen.getByRole('button', { name: 'Ocultar senha' }))
  expect(password).toHaveAttribute('type', 'password')
})

it('shows and hides the registration password accessibly', async () => {
  const user = userEvent.setup()
  render(<App />)
  await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
  const password = screen.getByLabelText('Senha')
  expect(password).toHaveAttribute('type', 'password')
  await user.click(screen.getByRole('button', { name: 'Mostrar senha' }))
  expect(password).toHaveAttribute('type', 'text')
})

it('explains that Google access is not available yet', async () => {
  const user = userEvent.setup()
  render(<App />)
  await user.click(await screen.findByRole('button', { name: /entrar com google/i }))
  expect(screen.getByRole('status')).toHaveTextContent(/google estará disponível em breve/i)
  expect(window.location.pathname).toBe('/login')
})

it('switches between login and registration in the same card', async () => {
  const user = userEvent.setup()
  render(<App />)
  await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
  expect(screen.getByLabelText('Nome')).toBeVisible()
  expect(screen.getByRole('button', { name: 'Criar minha conta' })).toBeVisible()
  await user.click(screen.getByRole('tab', { name: 'Entrar' }))
  expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
})

it('validates registration without sending invalid data', async () => {
  let requestCount = 0
  server.use(http.post('*/api/v1/auth/register', () => { requestCount += 1; return HttpResponse.json({}, { status: 201 }) }))
  const user = userEvent.setup()
  render(<App />)
  await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
  await user.type(screen.getByLabelText('E-mail'), 'invalid')
  await user.type(screen.getByLabelText('Senha'), '123')
  await user.click(screen.getByRole('button', { name: 'Criar minha conta' }))
  expect(await screen.findByText('Informe seu nome')).toBeVisible()
  expect(screen.getByText('Informe um e-mail válido')).toBeVisible()
  expect(screen.getByText('A senha deve ter pelo menos 6 caracteres')).toBeVisible()
  expect(requestCount).toBe(0)
})

it('registers an account and returns to login with its email', async () => {
  const user = userEvent.setup()
  render(<App />)
  await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
  await user.type(screen.getByLabelText('Nome'), 'Maria Silva')
  await user.type(screen.getByLabelText('E-mail'), 'maria@example.com')
  await user.type(screen.getByLabelText('Senha'), '123456')
  await user.click(screen.getByRole('button', { name: 'Criar minha conta' }))
  expect(await screen.findByText(/conta criada com sucesso/i)).toBeVisible()
  expect(screen.getByRole('tab', { name: 'Entrar' })).toHaveAttribute('aria-selected', 'true')
  expect(screen.getByLabelText('E-mail')).toHaveValue('maria@example.com')
  expect(screen.getByLabelText('Senha')).toHaveValue('')
})

it('shows the API message when registration fails', async () => {
  server.use(http.post('*/api/v1/auth/register', () => HttpResponse.json({ message: 'E-mail já cadastrado' }, { status: 409 })))
  const user = userEvent.setup()
  render(<App />)
  await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
  await user.type(screen.getByLabelText('Nome'), 'Maria Silva')
  await user.type(screen.getByLabelText('E-mail'), 'maria@example.com')
  await user.type(screen.getByLabelText('Senha'), '123456')
  await user.click(screen.getByRole('button', { name: 'Criar minha conta' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('E-mail já cadastrado')
})
```

- [ ] **Step 2: Run the interaction tests and verify red**

Run:

```bash
npx vitest run src/features/auth/auth-flow.test.tsx
```

Expected: the existing login tests pass and the new tests FAIL because password visibility, mode tabs, Google feedback, and registration UI are absent.

- [ ] **Step 3: Implement the shared password field**

Create `src/features/auth/PasswordField.tsx`:

```tsx
import { Eye, EyeOff, LockKeyhole } from 'lucide-react'
import { useState } from 'react'
import type { UseFormRegisterReturn } from 'react-hook-form'

interface PasswordFieldProps {
  id: string
  autoComplete: 'current-password' | 'new-password'
  error?: string
  registration: UseFormRegisterReturn
}

export function PasswordField({ id, autoComplete, error, registration }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)
  const errorId = `${id}-error`
  return (
    <div>
      <label className="mb-2 block text-sm font-medium" htmlFor={id}>Senha</label>
      <div className="relative">
        <LockKeyhole className="absolute left-3 top-3.5 size-5 text-[#718177]" aria-hidden="true" />
        <input id={id} type={visible ? 'text' : 'password'} autoComplete={autoComplete} className="h-12 w-full rounded-xl border border-[#cfddd3] pl-11 pr-12 focus:border-[#168f50] focus:outline-none focus:ring-2 focus:ring-[#168f50]/20" aria-invalid={Boolean(error)} aria-describedby={error ? errorId : undefined} {...registration} />
        <button type="button" className="absolute right-1 top-1 grid size-10 place-items-center rounded-lg text-[#65766b] hover:bg-[#eef7f0]" aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'} onClick={() => setVisible((current) => !current)}>{visible ? <EyeOff className="size-5" aria-hidden="true" /> : <Eye className="size-5" aria-hidden="true" />}</button>
      </div>
      {error && <p className="mt-1 text-sm text-red-600" id={errorId}>{error}</p>}
    </div>
  )
}
```

- [ ] **Step 4: Extract the regular login form**

Create `src/features/auth/LoginForm.tsx` with this public contract and behavior:

```tsx
interface LoginFormProps {
  initialEmail: string
  onCreateAccount: () => void
  onGoogleUnavailable: () => void
}
```

Move the existing React Hook Form login logic from `LoginPage` into `LoginForm`. Use `defaultValues: { email: initialEmail, password: '' }`, the existing `useAuth().login`, the existing `/dashboard` navigation, and `getApiErrorMessage`. Render `PasswordField` with `id="login-password"`, `autoComplete="current-password"`, and `registration={register('password')}`. Keep the first-access warning.

After the regular submit button, render the future provider separator and action exactly as:

```tsx
<div className="flex items-center gap-3" aria-hidden="true"><span className="h-px flex-1 bg-[#dce8df]" /><span className="text-xs font-medium uppercase tracking-[0.15em] text-[#7a8980]">ou</span><span className="h-px flex-1 bg-[#dce8df]" /></div>
<button className="flex h-12 w-full items-center justify-center gap-3 rounded-xl bg-[#151a17] font-medium text-white hover:bg-black" type="button" onClick={onGoogleUnavailable}><span className="grid size-6 place-items-center rounded-full bg-white font-bold text-[#4285f4]" aria-hidden="true">G</span>Entrar com Google</button>
<p className="text-center text-sm text-[#68786f]">Ainda não tem uma conta? <button className="font-semibold text-[#168f50]" type="button" onClick={onCreateAccount}>Criar conta</button></p>
```

- [ ] **Step 5: Implement the real registration form**

Create `src/features/auth/RegisterForm.tsx` with:

```tsx
interface RegisterFormProps {
  onBack: () => void
  onSuccess: (email: string) => void
}
```

Use `useForm<RegisterFormData>({ resolver: zodResolver(registerSchema), defaultValues: { name: '', email: '', password: '' } })`. Its submit handler must execute this exact data flow:

```tsx
const onSubmit = handleSubmit(async (data) => {
  setApiError(null)
  try {
    await registerAccount(data)
    reset()
    onSuccess(data.email)
  } catch (error) {
    setApiError(getApiErrorMessage(error, 'Não foi possível criar sua conta.'))
  }
})
```

Render labeled `Nome` and `E-mail` inputs, `PasswordField` with `id="register-password"` and `autoComplete="new-password"`, API error with `role="alert"`, submit text `Criar minha conta` / `Criando conta...`, and this return action:

```tsx
<p className="text-center text-sm text-[#68786f]">Já tem uma conta? <button className="font-semibold text-[#168f50]" type="button" onClick={onBack}>Entrar</button></p>
```

- [ ] **Step 6: Recompose the branded access page**

Replace `LoginPage` form ownership with local page state:

```tsx
type AccessMode = 'login' | 'register'

const [mode, setMode] = useState<AccessMode>('login')
const [initialEmail, setInitialEmail] = useState('')
const [notice, setNotice] = useState<string | null>(null)

function selectMode(nextMode: AccessMode) {
  setNotice(null)
  setMode(nextMode)
}

function handleRegistered(email: string) {
  setInitialEmail(email)
  setMode('login')
  setNotice('Conta criada com sucesso. Entre para continuar.')
}
```

Keep the authenticated `<Navigate to="/dashboard" replace />`. Render a full-height green grid background, a `Link` back to `/`, the `BrandMark`, and a white card. Add a tablist before the form:

```tsx
<div className="grid grid-cols-2 rounded-xl bg-[#eef5f0] p-1" role="tablist" aria-label="Forma de acesso">
  {(['login', 'register'] as const).map((item) => {
    const selected = mode === item
    const label = item === 'login' ? 'Entrar' : 'Criar conta'
    return <button key={item} type="button" role="tab" aria-selected={selected} className={selected ? 'min-h-11 rounded-lg bg-white font-semibold text-[#173b27] shadow-sm' : 'min-h-11 rounded-lg font-medium text-[#6b7d71]'} onClick={() => selectMode(item)}>{label}</button>
  })}
</div>
```

Render notices as `<p role="status">`. In login mode, show `LoginForm` and set the Google notice to `O login com Google estará disponível em breve.` In registration mode, show `RegisterForm` and call `handleRegistered` on success.

- [ ] **Step 7: Run auth tests and resolve only test-proven regressions**

Run:

```bash
npx vitest run src/features/auth/auth-flow.test.tsx src/app/App.test.tsx
```

Expected: PASS with no unhandled MSW request and no ambiguous form queries.

- [ ] **Step 8: Commit the access experience**

```bash
git add src/features/auth/LoginPage.tsx src/features/auth/LoginForm.tsx src/features/auth/RegisterForm.tsx src/features/auth/PasswordField.tsx src/features/auth/auth-flow.test.tsx
git commit -m "evolui acesso e cadastro"
```

---

### Task 5: Complete metadata, documentation, and regression verification

**Files:**
- Modify: `index.html`
- Modify: `README.md`
- Modify: `.env.example`
- Modify: `src/app/App.test.tsx`

**Interfaces:**
- Consumes: completed landing and access flows from Tasks 1–4.
- Produces: production metadata and documentation that identify the product as EstoqueHub and reference the validated public domains.

- [ ] **Step 1: Add the public entry to the end-to-end application flow test**

Change `src/app/App.test.tsx` to start at `/` and enter through the only landing CTA before filling credentials:

```tsx
window.history.pushState({}, '', '/')
const user = userEvent.setup()
render(<App />)

await user.click(await screen.findByRole('link', { name: 'Acesse a plataforma' }))
expect(window.location.pathname).toBe('/login')
```

Keep the existing login, dashboard, products, search, and logout assertions.
Change its password query to `screen.getByLabelText('Senha')` and its login
submit query to `screen.getByRole('button', { name: 'Entrar' })`. After logout,
continue to expect the access card because the protected route sends anonymous
users to `/login`.

- [ ] **Step 2: Run the application flow test and verify red if the entry path is incomplete**

Run:

```bash
npx vitest run src/app/App.test.tsx
```

Expected: PASS if Task 2 is complete; otherwise FAIL specifically at landing-to-login navigation and fix only that contract.

- [ ] **Step 3: Update product metadata**

In `index.html`, set:

```html
<meta name="theme-color" content="#168f50" />
<meta
  name="description"
  content="EstoqueHub — controle simples de produtos, quantidades e alertas para pequenos comércios"
/>
<title>EstoqueHub</title>
```

Update `README.md` to:

- use `# EstoqueHub` as the title;
- list Landing (`/`) with presentation, problem, and real features;
- list Access (`/login`) with login, future Google notice, and account creation;
- describe registration as `POST /api/v1/auth/register` followed by regular login;
- retain the production frontend URL `https://controle-de-estoque.hanrry.top`;
- retain the API URL `https://api-controle-de-estoque.hanrry.top`.

Ensure `.env.example` contains exactly:

```dotenv
VITE_API_URL=https://api-controle-de-estoque.hanrry.top
```

- [ ] **Step 4: Run the complete quality gate**

Run:

```bash
npm test
npm run lint
npm run typecheck
npm run build
git diff --check
```

Expected: all tests PASS, lint has zero errors, TypeScript has zero errors, Vite produces `dist/`, and `git diff --check` prints nothing.

- [ ] **Step 5: Inspect production output for stale identity and accidental claims**

Run:

```bash
rg -n 'Verdejar|Inventory Manager|Planos|Trial gratuito|Google OAuth' src index.html README.md
```

Expected: no stale `Verdejar` or `Inventory Manager` identity, no pricing/trial copy, and no claim that Google OAuth works. The README may say that Google is planned, but the UI must say it is coming soon.

- [ ] **Step 6: Commit metadata and documentation**

```bash
git add index.html README.md .env.example src/app/App.test.tsx
git commit -m "finaliza experiencia publica da estoquehub"
```

- [ ] **Step 7: Request final code review and verify the reviewed tree**

Invoke `superpowers:requesting-code-review` over all commits created by this plan. If the review reports defects, verify each finding against the spec, apply only in-scope corrections with their focused tests, and then repeat:

```bash
npm test
npm run lint
npm run typecheck
npm run build
git status --short
```

Expected: every quality command succeeds. `git status --short` is empty unless the user has unrelated pre-existing changes that were deliberately preserved and reported.
