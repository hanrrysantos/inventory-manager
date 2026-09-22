# EstoqueHub Landing Page Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the generic marketing-template landing page with an editorial operational SaaS experience whose copy and visuals are grounded in EstoqueHub's real dashboard and product catalogue.

**Architecture:** Keep `/` as a static React route and retain the existing landing feature boundary. `LandingPage` composes the narrative, `LandingHeader` and `LandingFooter` own public navigation, and a prop-driven `ProductPreview` renders three non-interactive views from one local data module; no API, authentication, or shared application state changes are required.

**Tech Stack:** React 19, TypeScript 6, React Router 7, Tailwind CSS 4, Lucide React, Vitest, Testing Library, and user-event.

**Spec:** `docs/superpowers/specs/2026-09-22-landing-page-redesign-design.md`

## Global Constraints

- Before implementation, use `superpowers:using-git-worktrees` and create an isolated branch named `feat/landing-editorial-redesign`; do not implement on `feat/product-pagination`.
- Keep `/`, `/login`, `/dashboard`, and `/products` routing behavior unchanged.
- Do not add an API request, authentication behavior, or new product capability.
- Do not add pricing, testimonials, customer logos, invented metrics, unsupported claims, stock photography, or decorative illustration.
- Use only current dependencies; do not add a font or animation package.
- Use Georgia as the editorial display face and the existing Inter/system stack for body and product-interface text, avoiding network font loading and layout shift.
- Use warm off-white, graphite, deep green, restrained lime, and semantic amber; exclude green gradients, glow, decorative grids, blobs, and fake browser chrome.
- General corner radii stay between 8 px and 14 px; pills are allowed only for real statuses or controls.
- Product previews use local representative data, remain non-interactive, and are hidden from assistive technology when adjacent copy explains them.
- Every interactive target remains at least 44 px in both dimensions, and no viewport from 320 px upward gains horizontal page scrolling.
- All non-essential motion must stop under `prefers-reduced-motion: reduce`.
- Quality gates are `npm test`, `npm run lint`, `npm run typecheck`, and `npm run build`.

## Review Focus

- Reduced-motion visitors must receive `behavior: 'auto'` from anchor navigation and no preview animation; Task 1 pins the navigation behavior and Task 4 verifies the CSS override.
- Modified anchor clicks (Ctrl/Meta/Shift/Alt) must preserve normal browser behavior instead of forcing scroll or hash state; Task 1 adds the regression test.
- Public `/` must stay visible while a stored session is restoring, and unknown routes must retain their current anonymous/authenticated redirects; Task 3 keeps those route tests.
- Decorative previews must expose no focusable button, input, or link and must not duplicate their table content to assistive technology; Task 2 pins the contract.
- Header, hero, and closing access actions must all reach `/login` and keep 44 px touch targets on a 320 px layout; Task 3 tests hrefs/classes and Task 4 performs the viewport check.

---

## File Structure

- Modify `src/features/landing/LandingHeader.tsx` — editorial public navigation and compact access action.
- Modify `src/features/landing/LandingFooter.tsx` — compact brand footer and matching section shortcuts.
- Modify `src/features/landing/SmoothAnchor.tsx` only if a Task 1 test exposes a behavior regression; its public `href: \`#${string}\`` interface stays unchanged.
- Create `src/features/landing/landing-preview-data.ts` — readonly local preview records and the exported `PreviewVariant` type.
- Rewrite `src/features/landing/ProductPreview.tsx` — non-interactive overview, search, and attention product views.
- Create `src/features/landing/ProductPreview.test.tsx` — focused preview variant and accessibility tests.
- Create `src/features/landing/ProductStory.tsx` — alternating copy/preview rows for the three product decisions.
- Rewrite `src/features/landing/LandingPage.tsx` — hero, operational-cost strip, product story, and closing CTA composition.
- Modify `src/features/landing/LandingPage.test.tsx` — stable copy, navigation, routing, touch-target, reduced-motion, and unsupported-claim coverage.
- Modify `src/app/App.test.tsx` — use the renamed public access action in the existing end-to-end application flow.
- Modify `src/styles/index.css` — landing design tokens, display typography, entry animation, and reduced-motion override.

### Task 1: Public Navigation Contract

**Files:**
- Modify: `src/features/landing/LandingHeader.tsx`
- Modify: `src/features/landing/LandingFooter.tsx`
- Modify: `src/features/landing/LandingPage.tsx`
- Modify if required: `src/features/landing/SmoothAnchor.tsx`
- Test: `src/features/landing/LandingPage.test.tsx`
- Test: `src/app/App.test.tsx`

**Interfaces:**
- Consumes: `SmoothAnchor({ href: \`#${string}\`, ...anchorProps })` and React Router `Link`.
- Produces: landing section anchors `#visao-geral` and `#produto`, plus a header access link with accessible name `Acessar` and destination `/login`.

- [ ] **Step 1: Replace the old navigation expectations with the new contract**

In `LandingPage.test.tsx`, change the smooth-scroll test to select **Visão geral** and add the reduced-motion and modified-click cases:

```tsx
it('scrolls to overview and moves focus without teleporting', async () => {
  const user = userEvent.setup()
  const scrollIntoView = vi.fn()
  Element.prototype.scrollIntoView = scrollIntoView
  render(<App />)

  const header = await screen.findByRole('banner')
  await user.click(within(header).getByRole('link', { name: 'Visão geral' }))

  expect(scrollIntoView).toHaveBeenCalledWith({
    behavior: 'smooth',
    block: 'start',
  })
  expect(window.location.hash).toBe('#visao-geral')
  expect(document.querySelector('#visao-geral')).toHaveFocus()
})

it('uses immediate anchor scrolling when reduced motion is requested', async () => {
  const user = userEvent.setup()
  const scrollIntoView = vi.fn()
  Element.prototype.scrollIntoView = scrollIntoView
  vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({ matches: true }))
  render(<App />)

  const header = await screen.findByRole('banner')
  await user.click(within(header).getByRole('link', { name: 'Produto' }))

  expect(scrollIntoView).toHaveBeenCalledWith({ behavior: 'auto', block: 'start' })
  vi.unstubAllGlobals()
})

it('does not intercept a modified anchor click', async () => {
  const scrollIntoView = vi.fn()
  Element.prototype.scrollIntoView = scrollIntoView
  render(<App />)

  const header = await screen.findByRole('banner')
  fireEvent.click(within(header).getByRole('link', { name: 'Produto' }), {
    ctrlKey: true,
  })

  expect(scrollIntoView).not.toHaveBeenCalled()
})
```

Preserve the existing `try/finally` restoration of `Element.prototype.scrollIntoView` and add `afterEach(() => vi.unstubAllGlobals())` so a failed assertion cannot leak `matchMedia` state.

In `App.test.tsx`, change the initial link query to the exact header action:

```tsx
await user.click(await screen.findByRole('link', { name: 'Acessar' }))
```

Add `fireEvent` to the existing Testing Library import. Also update the
interim landing assertions from `#funcionalidades` to `#produto`, and change
the old single access-link query from **Acesse a plataforma** to the exact
accessible name **Acessar**. The old problem copy may remain until Task 3, but
the navigation labels and IDs must already be coherent at the end of this
task.

- [ ] **Step 2: Run the focused tests and confirm they fail against the old labels and anchors**

Run: `npm test -- src/features/landing/LandingPage.test.tsx src/app/App.test.tsx`

Expected: FAIL because `Visão geral`, `Produto`, `#visao-geral`, `#produto`, and the `Acessar` link do not exist yet.

- [ ] **Step 3: Implement the compact public header and matching footer navigation**

Replace the header navigation with this structure while retaining the existing `BrandMark` visibility behavior:

```tsx
<header className="sticky top-0 z-50 border-b border-[#d8d8d0] bg-[#f4f2ea]/95 backdrop-blur-md">
  <div className="mx-auto flex min-h-20 max-w-[1440px] items-center justify-between gap-4 px-5 lg:px-10">
    <SmoothAnchor className="inline-flex min-h-11 items-center" href="#visao-geral" aria-label="EstoqueHub — início">
      <BrandMark />
    </SmoothAnchor>
    <nav className="hidden items-center gap-8 text-sm text-[#4d514c] md:flex" aria-label="Navegação da página inicial">
      <SmoothAnchor className="inline-flex min-h-11 items-center hover:text-[#123d2b]" href="#visao-geral">Visão geral</SmoothAnchor>
      <SmoothAnchor className="inline-flex min-h-11 items-center hover:text-[#123d2b]" href="#produto">Produto</SmoothAnchor>
    </nav>
    <Link className="inline-flex min-h-11 items-center border-b border-[#123d2b] text-sm font-semibold text-[#123d2b]" to="/login">
      Acessar
      <ArrowUpRight className="ml-2 size-4" aria-hidden="true" />
    </Link>
  </div>
</header>
```

Update the footer shortcuts to the same two anchors. Give the existing hero section `id="visao-geral"` and the existing features section `id="produto"` as an intermediate state so navigation works before Task 3 replaces the page body.

Do not change `SmoothAnchor` unless the new tests identify a real defect. Its current modifier-key and reduced-motion branches already express the desired behavior.

- [ ] **Step 4: Run the focused tests and make them pass**

Run: `npm test -- src/features/landing/LandingPage.test.tsx src/app/App.test.tsx`

Expected: PASS, including smooth scroll, reduced motion, modified clicks, and the application login flow.

- [ ] **Step 5: Commit the public navigation contract**

```bash
git add src/features/landing/LandingHeader.tsx src/features/landing/LandingFooter.tsx src/features/landing/LandingPage.tsx src/features/landing/LandingPage.test.tsx src/app/App.test.tsx
git commit -m "refatora navegação pública da landing"
```

### Task 2: Product Preview System

**Files:**
- Create: `src/features/landing/landing-preview-data.ts`
- Rewrite: `src/features/landing/ProductPreview.tsx`
- Modify: `src/features/landing/LandingPage.tsx`
- Create: `src/features/landing/ProductPreview.test.tsx`
- Modify: `src/features/landing/LandingPage.test.tsx`

**Interfaces:**
- Consumes: local readonly records from `landing-preview-data.ts`; no React context, router state, or API client.
- Produces: `PreviewVariant = 'overview' | 'search' | 'attention'` and `ProductPreview({ variant, className? }: { variant: PreviewVariant; className?: string })`.

- [ ] **Step 1: Write focused tests for every preview and its accessibility boundary**

Create `ProductPreview.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ProductPreview } from './ProductPreview'

describe('ProductPreview', () => {
  it.each([
    ['overview', '462', 'Precisam de atenção'],
    ['search', 'FEI-001', 'Feijão Carioca 1kg'],
    ['attention', '12 / 15', 'Estoque baixo'],
  ] as const)('renders the %s product state', (variant, value, label) => {
    render(<ProductPreview variant={variant} />)

    const preview = screen.getByTestId(`product-preview-${variant}`)
    expect(preview).toHaveAttribute('aria-hidden', 'true')
    expect(preview).toHaveTextContent(value)
    expect(preview).toHaveTextContent(label)
  })

  it('does not expose interactive controls inside decorative previews', () => {
    render(<ProductPreview variant="search" />)

    const preview = screen.getByTestId('product-preview-search')
    expect(preview.querySelector('a, button, input, select, textarea, [tabindex]')).toBeNull()
  })
})
```

- [ ] **Step 2: Run the preview test and confirm the new interface is absent**

Run: `npm test -- src/features/landing/ProductPreview.test.tsx`

Expected: FAIL because the old `ProductPreview` accepts no `variant` and does not render the three test IDs.

- [ ] **Step 3: Create one typed source for representative preview data**

Create `landing-preview-data.ts` with exact, readonly data used by the previews:

```ts
export type PreviewVariant = 'overview' | 'search' | 'attention'

export const previewSummary = [
  { label: 'Total de itens', value: '462', note: 'em 13 produtos' },
  { label: 'Estoque baixo', value: '4', note: 'abaixo do mínimo' },
  { label: 'Em falta', value: '0', note: 'reposição urgente' },
] as const

export const previewProducts = [
  { name: 'Arroz Agulhinha 5kg', sku: 'ARR-001', quantity: 40, status: 'Em estoque' },
  { name: 'Feijão Carioca 1kg', sku: 'FEI-001', quantity: 12, status: 'Estoque baixo' },
  { name: 'Macarrão Espaguete 500g', sku: 'MAC-001', quantity: 21, status: 'Em estoque' },
] as const

export const attentionProducts = [
  { name: 'Feijão Carioca 1kg', sku: 'FEI-001', stock: '12 / 15', percent: 80 },
  { name: 'Suco de Laranja 1L', sku: 'SUC-001', stock: '2 / 10', percent: 20 },
] as const
```

These are interface examples, not business metrics or customer results. Do not add percentages, revenue gains, or claims derived from them.

- [ ] **Step 4: Implement the prop-driven, non-interactive preview**

Rewrite `ProductPreview.tsx` around one exported component and three internal views:

```tsx
interface ProductPreviewProps {
  variant: PreviewVariant
  className?: string
}

export function ProductPreview({ variant, className }: ProductPreviewProps) {
  return (
    <div
      aria-hidden="true"
      className={cn('overflow-hidden rounded-xl border border-[#cbcfc7] bg-[#f9f8f3]', className)}
      data-testid={`product-preview-${variant}`}
    >
      <PreviewHeader section={variant === 'search' ? 'Produtos' : 'Painel de estoque'} />
      {variant === 'overview' && <OverviewPreview />}
      {variant === 'search' && <SearchPreview />}
      {variant === 'attention' && <AttentionPreview />}
    </div>
  )
}
```

`OverviewPreview` renders `previewSummary` followed by a compact **Precisam de atenção** list. `SearchPreview` renders a visual search field containing `FEI-001`, a status filter label, and a small table from `previewProducts`. `AttentionPreview` renders the two `attentionProducts`, their `stock` values, and semantic amber progress bars using inline `width: \`${percent}%\``. Use `<div>`/`<span>` for visual controls; never render `<input>`, `<button>`, or `<a>` inside an `aria-hidden` subtree.

Update the existing landing usage to `<ProductPreview variant="overview" />`.
In the existing landing preview test, replace the old detergent assertion with
`Feijão Carioca 1kg`, keep the dashboard-summary assertions, and assert that
the overview preview contains no interactive descendant. This keeps the whole
landing suite green before the other variants are added to the narrative.

Use square-ish panels, 1 px borders, tabular numbers (`tabular-nums`), monospace SKU labels, and no browser traffic lights, outer blobs, heavy shadow, or radius above 14 px.

- [ ] **Step 5: Run the preview tests**

Run: `npm test -- src/features/landing/ProductPreview.test.tsx src/features/landing/LandingPage.test.tsx`

Expected: PASS with all three variants and no focusable descendants.

- [ ] **Step 6: Commit the product preview system**

```bash
git add src/features/landing/landing-preview-data.ts src/features/landing/ProductPreview.tsx src/features/landing/ProductPreview.test.tsx src/features/landing/LandingPage.tsx src/features/landing/LandingPage.test.tsx
git commit -m "cria demonstrações editoriais do produto"
```

### Task 3: Editorial Landing Narrative

**Files:**
- Create: `src/features/landing/ProductStory.tsx`
- Rewrite: `src/features/landing/LandingPage.tsx`
- Modify: `src/features/landing/LandingPage.test.tsx`

**Interfaces:**
- Consumes: `ProductPreview({ variant, className? })`, `LandingHeader`, `LandingFooter`, and React Router `Link`.
- Produces: semantic sections `#visao-geral` and `#produto`, one `h1`, product-story headings, three `/login` access actions, and the approved copy from the spec.

- [ ] **Step 1: Replace generic-copy assertions with the approved narrative**

Update the principal content test in `LandingPage.test.tsx`:

```tsx
it('presents the product through concrete operational decisions', async () => {
  render(<App />)

  expect(await screen.findByRole('heading', {
    level: 1,
    name: /seu estoque, sem pontos cegos/i,
  })).toBeVisible()
  expect(screen.getByRole('heading', {
    name: /o problema não é contar produtos\. é descobrir tarde demais/i,
  })).toBeVisible()
  expect(screen.getByRole('heading', { name: 'Veja o que está acontecendo' })).toBeVisible()
  expect(screen.getByRole('heading', { name: 'Encontre antes de procurar' })).toBeVisible()
  expect(screen.getByRole('heading', { name: 'Reponha antes de faltar' })).toBeVisible()
  expect(document.querySelector('#visao-geral')).toBeInTheDocument()
  expect(document.querySelector('#produto')).toBeInTheDocument()

  for (const unsupported of ['Planos', 'Depoimentos', 'Fornecedores', 'Relatórios']) {
    expect(screen.queryByText(unsupported, { exact: false })).not.toBeInTheDocument()
  }
})
```

Replace the old single-CTA test with href and touch-target coverage that does not depend on button styling:

```tsx
it('offers consistent access actions with minimum touch targets', async () => {
  render(<App />)
  await screen.findByRole('heading', { level: 1, name: /sem pontos cegos/i })

  const accessLinks = screen.getAllByRole('link').filter((link) =>
    link.getAttribute('href') === '/login',
  )
  expect(accessLinks).toHaveLength(3)
  for (const link of accessLinks) {
    expect(link).toHaveClass('min-h-11')
  }
})
```

Keep the unknown-route and stored-session tests, changing only their landing `h1` query to `/seu estoque, sem pontos cegos/i`.

- [ ] **Step 2: Run the landing tests and confirm the new narrative fails**

Run: `npm test -- src/features/landing/LandingPage.test.tsx`

Expected: FAIL because the approved hero, operational-cost, story, and closing copy are not rendered.

- [ ] **Step 3: Build the three-row product story component**

Create `ProductStory.tsx` with this fixed content model:

```tsx
const stories = [
  {
    number: '01',
    title: 'Veja o que está acontecendo',
    description: 'Quantidades e estados críticos aparecem juntos para mostrar onde sua atenção é necessária agora.',
    variant: 'overview',
  },
  {
    number: '02',
    title: 'Encontre antes de procurar',
    description: 'Busque pelo nome ou SKU e filtre o catálogo sem percorrer listas intermináveis.',
    variant: 'search',
  },
  {
    number: '03',
    title: 'Reponha antes de faltar',
    description: 'Compare a quantidade atual com o estoque mínimo e identifique a próxima reposição.',
    variant: 'attention',
  },
] satisfies ReadonlyArray<{
  number: string
  title: string
  description: string
  variant: PreviewVariant
}>
```

Render each item as an `<article>` with its number in a monospace label, copy in one grid column, and `ProductPreview` in the wider column. Alternate desktop direction with `lg:[&:nth-child(even)>...]:order-*` or an explicit `reverse` class computed from the map index; mobile order always keeps copy before its preview.

- [ ] **Step 4: Rewrite the page composition around the four approved regions**

Replace old `problems`/`features` icon arrays and the old page body with:

```tsx
<div className="min-h-screen overflow-x-hidden bg-[#f4f2ea] text-[#1e2420]">
  <LandingHeader />
  <main>
    <section id="visao-geral" className="scroll-mt-24 border-b border-[#d8d8d0]">
      {/* eyebrow: CONTROLE DE ESTOQUE / VISÃO OPERACIONAL */}
      {/* h1, supporting copy, hero /login Link, overview ProductPreview */}
    </section>
    <section aria-labelledby="operational-cost-title" className="border-b border-[#2f3932] bg-[#17251e] text-[#f4f2ea]">
      {/* approved transition heading and three bordered terms */}
    </section>
    <section id="produto" aria-labelledby="product-title" className="scroll-mt-24">
      {/* product section introduction and ProductStory */}
    </section>
    <section aria-labelledby="closing-title" className="border-t border-[#cfd2ca]">
      {/* approved closing copy and final /login Link */}
    </section>
  </main>
  <LandingFooter />
</div>
```

Use the exact approved core and closing copy. The operational terms are **Produto parado**, **Reposição atrasada**, and **Venda perdida**; give each one a short cause/effect sentence, not an invented statistic. Hero and closing links use **Acessar EstoqueHub**, header uses **Acessar**, and all three point to `/login`.

The hero grid is intentionally asymmetric (`lg:grid-cols-[0.72fr_1.28fr]`) and the preview crosses the visual midpoint without absolute positioning. Do not restore a pill badge, benefit checklist, browser frame, grid texture, or decorative circle.

- [ ] **Step 5: Run landing, preview, and application-flow tests**

Run: `npm test -- src/features/landing/LandingPage.test.tsx src/features/landing/ProductPreview.test.tsx src/app/App.test.tsx`

Expected: PASS for the approved narrative, all access links, route restoration, preview contract, and full login flow.

- [ ] **Step 6: Commit the editorial narrative**

```bash
git add src/features/landing/ProductStory.tsx src/features/landing/LandingPage.tsx src/features/landing/LandingPage.test.tsx
git commit -m "redesenha narrativa da landing page"
```

### Task 4: Visual System, Motion, and Responsive Verification

**Files:**
- Modify: `src/styles/index.css`
- Modify: `src/features/landing/LandingPage.tsx`
- Modify: `src/features/landing/ProductPreview.tsx`
- Modify: `src/features/landing/ProductStory.tsx`

**Interfaces:**
- Consumes: the semantic markup and `ProductPreview` variants from Tasks 2–3.
- Produces: `.landing-display`, `.landing-enter`, and `.landing-status-enter` CSS hooks, all disabled by the existing reduced-motion media query.

- [ ] **Step 1: Add the exact editorial tokens and motion hooks**

Extend `src/styles/index.css` without changing authenticated-page defaults:

```css
.landing-shell {
  --landing-paper: #f4f2ea;
  --landing-ink: #1e2420;
  --landing-green: #123d2b;
  --landing-line: #d8d8d0;
  --landing-lime: #c8f169;
  --landing-amber: #d89a3d;
}

.landing-display {
  font-family: Georgia, "Times New Roman", serif;
  font-weight: 400;
  letter-spacing: -0.055em;
}

@keyframes landing-enter {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes landing-status-enter {
  from { opacity: 0; transform: scaleX(0.92); }
  to { opacity: 1; transform: scaleX(1); }
}

.landing-enter {
  animation: landing-enter 500ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

.landing-status-enter {
  animation: landing-status-enter 420ms 180ms cubic-bezier(0.22, 1, 0.36, 1) both;
  transform-origin: left center;
}
```

Inside the existing `@media (prefers-reduced-motion: reduce)` block add:

```css
.landing-enter,
.landing-status-enter {
  animation: none;
}
```

Apply `landing-shell` only to the landing root, `landing-display` to marketing headings, `landing-enter` to the hero copy/preview, and `landing-status-enter` only to status/progress elements. Do not animate continuously.

- [ ] **Step 2: Verify the complete page at 320 px, 768 px, and 1440 px**

Run: `npm run dev -- --host 127.0.0.1`

In the browser, inspect `/` at each width and confirm:

- 320 px: no horizontal scrollbar; hero copy is readable; all three previews crop or reflow without tiny scaled text; every link is at least 44 px high.
- 768 px: navigation and story spacing do not collide; operational terms remain readable; interface tables do not overflow the page.
- 1440 px: asymmetric hero and alternating stories align to one grid; interface views dominate decorative space; no card uses a radius over 14 px.
- Keyboard: focus is visible on logo, navigation, and access links; anchors place focus on their target section.
- Reduced motion: enabling the OS/browser preference stops entry/status animation and makes anchor scrolling immediate.

If a viewport fails, adjust only responsive utility classes in `LandingPage.tsx`, `ProductStory.tsx`, or `ProductPreview.tsx`; do not introduce viewport-specific JavaScript.

- [ ] **Step 3: Run the complete automated verification suite**

Run: `npm test`

Expected: all Vitest tests PASS.

Run: `npm run lint`

Expected: ESLint exits with code 0.

Run: `npm run typecheck`

Expected: TypeScript exits with code 0.

Run: `npm run build`

Expected: Vite produces `dist/` successfully with no type or bundle error.

- [ ] **Step 4: Inspect the final branch diff**

Run: `git diff --check`

Expected: no whitespace errors.

Run: `git status --short`

Expected: only the landing, test, and global-style files listed in this plan are modified or untracked.

- [ ] **Step 5: Commit the visual system and responsive polish**

```bash
git add src/styles/index.css src/features/landing/LandingPage.tsx src/features/landing/ProductPreview.tsx src/features/landing/ProductStory.tsx
git commit -m "finaliza identidade editorial da landing"
```

After the commit, rerun `git status --short` and ensure the isolated implementation worktree is clean.
