# EstoqueHub landing page redesign

**Date:** 2026-09-22
**Status:** Approved in conversation

## Context

EstoqueHub is a portfolio project that is also intended to work as a real
product for small businesses. Its current landing page communicates the
product's purpose, but its visual vocabulary resembles a generic generated
SaaS template: a sparkle badge, oversized headline, decorative grid and
blobs, large rounded cards, repeated icon panels, and a browser-style product
mockup.

The redesign must make the project feel deliberate and credible as a SaaS
product while giving the portfolio a recognizable visual point of view. The
approved direction is an **editorial operational SaaS**: a restrained visual
system inspired by stock records, SKU labels, status indicators, and the
real product interface.

## Goals

- Present EstoqueHub as a credible, working SaaS product.
- Give the landing page a distinctive and portfolio-worthy identity.
- Make the real interface, rather than decorative marketing elements, the
  primary visual evidence.
- Explain the operational problem and the decisions the product enables.
- Keep the page concise, responsive, accessible, and grounded in features
  that exist today.

## Non-goals

- No redesign of login, dashboard, or products pages in this iteration.
- No new product capability, API contract, or authenticated flow.
- No pricing, testimonials, invented metrics, customer logos, or unsupported
  claims.
- No generic design-system refactor beyond focused landing-page styles and
  components.
- No stock photography or decorative illustration.

## Core positioning

The landing page is organized around this central message:

> Seu estoque, sem pontos cegos.

The supporting copy explains the real benefit without vague marketing
language:

> Acompanhe quantidades, encontre produtos e descubra o que precisa de
> reposição antes que isso afete suas vendas.

The voice is confident, direct, and operational. It avoids broad phrases such
as "no ritmo do seu negócio" and "decidir com clareza" when a concrete action
or consequence can be named instead.

## Information architecture

The page remains a single scrolling document at `/` with four main regions.

### 1. Hero

The hero introduces the positioning, concise supporting copy, and one primary
call to action: **Acessar EstoqueHub**. A large presentation of the real
product interface provides immediate evidence of what the application does.

The hero does not use a marketing badge, benefit checklist, decorative grid,
abstract blob, or fake browser frame. The product view may be composed from
HTML and CSS, but it must resemble the actual application and must not imply
interactions or data that do not exist.

### 2. Operational cost

The transition begins with:

> O problema não é contar produtos. É descobrir tarde demais.

Instead of three interchangeable cards, an editorial strip connects concrete
stock conditions to their consequences. The vocabulary may include **Produto
parado**, **Reposição atrasada**, and **Venda perdida**. These are presented as
operational situations, not as fabricated customer statistics.

### 3. Product in use

Three large interface demonstrations show the product through decisions a
user can make:

1. **Veja o que está acontecendo** — the dashboard summary and attention
   states provide an overview of stock health.
2. **Encontre antes de procurar** — product search and status filters make a
   specific item easier to locate.
3. **Reponha antes de faltar** — low-stock and out-of-stock states reveal
   which products require action.

Each demonstration pairs a short explanation with a substantial crop or
reconstruction of the actual interface. The sections alternate composition
without becoming a repeated grid of feature cards.

### 4. Closing and footer

The final prompt reads:

> Seu estoque já está em movimento. A informação também deveria estar.

It leads to the same access destination as the primary call to action. The
footer remains compact and contains only useful brand and navigation
information.

## Navigation and calls to action

The header contains the brand, links for **Visão geral** and **Produto**, and
an **Acessar** action. Anchor names must match visible sections and remain
usable with keyboard navigation.

The page should avoid competing conversion actions. Access links in the
header, hero, and closing section may repeat the same destination when needed
by the narrative, but they must use consistent wording and visual hierarchy.
There is no secondary signup or demo flow.

## Visual language

### Colour

- A warm off-white is the primary page surface.
- Graphite provides the main text and structural colour.
- Deep green identifies the brand and primary actions.
- The current lime green is reserved for small, intentional highlights.
- Amber identifies attention states already represented in the product.

Colour must support product meaning. Large green gradients, decorative glow,
and sustainability-coded visual effects are excluded.

### Typography

Display typography gives headings an editorial character and compact rhythm.
A neutral sans-serif remains responsible for controls, labels, tables, and
supporting text. The implementation may use a carefully selected web font if
its loading strategy avoids layout instability; otherwise it must use a
distinctive, intentional system-font stack.

Headings should be short enough to benefit from the display face. Product
interface text must remain visually consistent with the application.

### Shape and depth

- Corners generally use an 8–14 px radius.
- Fine borders establish hierarchy.
- Shadows are subtle and used only to separate meaningful layers.
- Repeated 30 px cards, pills used as decoration, and ornamental browser
  chrome are removed.

### Composition

The page uses a disciplined grid with asymmetric arrangements. Editorial
headings and explanations align to fixed columns while selected interface
crops may span or cross columns at wide breakpoints. Generous whitespace and
precise alignment create contrast without relying on decorative backgrounds.

Numbers, SKU fragments, table rules, quantities, and product statuses can
serve as graphic material when they remain legible and contextually relevant.
Lucide icons appear only where an icon communicates a recognizable action or
state.

## Product demonstrations

Product visuals must be derived from the existing dashboard and product-list
experience. Shared sample data may be centralized within the landing feature
when this prevents divergent copies across demonstrations. The previews are
decorative representations and must be hidden from assistive technology when
their information is already explained in surrounding copy.

On smaller screens, previews may show fewer columns or records, but they must
not become illegible desktop screenshots scaled down to fit. The hierarchy of
summary, product, quantity, and status remains visible.

## Motion

Motion demonstrates product behavior rather than decorating the page. Allowed
examples include a search term being applied, a quantity changing, a filter
state selecting, or an attention status appearing. Motion should be brief,
subtle, and optional; the static composition must communicate the same idea.

All non-essential animation must stop under `prefers-reduced-motion`. The
implementation must not use continuous parallax, floating cards, or looping
ambient movement.

## Component boundaries

- `LandingPage` owns page composition and section order.
- `LandingHeader` owns public navigation and access actions.
- Focused section components may own the hero, operational-cost narrative,
  product demonstrations, and closing call to action when extraction keeps
  `LandingPage` readable.
- Product-preview components own only the presentation of real application
  states; they do not fetch data or imitate interactive controls.
- `LandingFooter` remains responsible for the compact footer.

Existing `SmoothAnchor` and `BrandMark` components should be reused when they
fit the approved design. Changes to shared brand presentation must preserve
authenticated layouts that already consume `BrandMark`.

## Responsive behavior

- Wide layouts use the asymmetric grid and large interface crops.
- Medium layouts retain the editorial hierarchy without depending on overlap.
- Small layouts become a clear vertical narrative with appropriately cropped
  product views.
- No section introduces horizontal page scrolling.
- Interactive targets remain at least 44 px in both dimensions.
- Text sizes and line lengths remain readable from 320 px viewports upward.

## Accessibility

- Page landmarks and heading order remain semantic.
- Anchor navigation accounts for the sticky header and visible focus.
- Text and interactive controls meet WCAG AA contrast.
- Product previews do not duplicate verbose content for screen readers.
- Meaning is never communicated by colour alone.
- Reduced-motion preferences are respected.
- The layout remains understandable without animations or hover states.

## Error handling and data flow

The landing page remains static and does not make API requests. Access actions
navigate to the existing `/login` route. No loading or error state is added to
the public page.

Decorative product demonstrations use local representative data and have no
effect on authentication or application state.

## Testing strategy

Tests focus on behavior and stable content rather than visual implementation
details. They cover:

- rendering the approved positioning and section headings;
- navigation from access actions to `/login`;
- working anchor links for overview and product sections;
- the absence of unsupported feature claims;
- semantic landmarks and an ordered heading structure where practical;
- preserving current application routing behavior.

Responsive and visual details should be reviewed in the browser at small,
medium, and wide viewports. Automated quality gates remain `npm test`,
`npm run lint`, `npm run typecheck`, and `npm run build`.

## Success criteria

The redesign is successful when the page can be recognized as EstoqueHub
without depending solely on its logo or green colour; when the real product is
the strongest visual element; when every claim maps to a current capability;
and when the page feels credible both as a public SaaS experience and as a
demonstration of deliberate frontend design.
