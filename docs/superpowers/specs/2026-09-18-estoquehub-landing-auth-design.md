# EstoqueHub landing page and authentication design

**Date:** 2026-09-18  
**Status:** Approved in conversation

## Context

The current application opens on the login page and uses the temporary
"Verdejar" identity. The next iteration introduces the EstoqueHub brand, a
public presentation page, and an expanded access card without creating
separate pages for marketing sections or registration.

The approved logo is stored at `src/assets/estoquehub-logo.png`. It is a
transparent PNG that preserves the boxes, analytics, cloud, and confirmation
concept in a simplified green mark.

## Goals

- Introduce EstoqueHub as the product identity.
- Add one public landing page at `/`.
- Present the problem and the real capabilities of the current product.
- Keep "Problema" and "Funcionalidades" as sections on the landing page.
- Provide one landing-page call to action in the header: "Acesse a plataforma".
- Evolve the existing `/login` card to support password visibility, regular
  login, a future Google login entry point, and real account registration.
- Keep the experience responsive and accessible from mobile through desktop.

## Non-goals

- No pricing section.
- No separate routes for problem, features, or registration.
- No Google OAuth integration in this iteration.
- No terms, privacy, blog, or contact pages.
- No claims for features that the application does not currently provide.
- No change to the authenticated dashboard and products flows beyond brand
  references needed for consistency.

## Information architecture

### Public landing page (`/`)

The page is a single scrolling document with these regions:

1. Sticky header with the EstoqueHub mark and wordmark, anchor links for
   "Problema" and "Funcionalidades", and one "Acesse a plataforma" button.
2. Hero with product positioning on the left and a code-built product mockup
   on the right.
3. `#problema` section describing the operational cost of manual control,
   missing visibility, and preventable stock loss.
4. `#funcionalidades` section presenting only existing capabilities: stock
   summary, product catalogue, stock-status filters, and attention alerts.
5. Dark footer with the brand, a short description, section shortcuts, and an
   access-platform link.

The header button and footer access link navigate to `/login`. The hero has no
button, so the header contains the landing page's only prominent call to
action.

### Access page (`/login`)

The access page retains one centered card and provides two modes inside it:

- **Entrar:** email, password, password-visibility control, regular submit,
  and a clickable "Entrar com Google" action.
- **Criar conta:** name, email, password, password-visibility control, real
  registration submit, and a link back to login.

Changing mode does not navigate to a new route. A "Voltar" control returns to
the landing page. Authenticated users who open `/login` continue to be sent to
`/dashboard`.

The Google action does not call an endpoint. It displays a polite status
message explaining that Google access will be available soon. The control is
kept as a clear integration point for the future migration to Google-based
login and registration.

## Visual direction

The reference layouts guide composition, hierarchy, and rhythm, but the page
uses the EstoqueHub identity rather than copying the orange reference brand.

- Fresh green is the primary action and accent colour.
- White and a lightly tinted neutral form the main surfaces.
- Dark near-black text maintains strong contrast.
- Fine grid lines may be used as subtle background texture in the hero and
  access page.
- The footer uses a dark green-black surface.
- Rounded cards, restrained shadows, and generous spacing match the current
  product while making the public experience more polished.

The hero mockup is built with semantic HTML and CSS rather than a static
screenshot. It depicts the EstoqueHub dashboard inside a notebook frame with
a smaller mobile dashboard overlapping it. The mockup is decorative and must
not expose interactive controls or imply unavailable features.

## Registration data flow

The backend already exposes:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "Maria Silva",
  "email": "maria@example.com",
  "password": "minimum-six-characters"
}
```

The endpoint returns `201 Created` with the created user. It does not return a
JWT, so registration must not silently authenticate the visitor.

On success, the card switches back to login, preserves the registered email,
clears the password, and displays a success message asking the user to sign
in. On failure, the existing API-error normalization presents the backend
message in the card and keeps the entered non-sensitive data available.

Client validation mirrors the backend contract:

- name is required;
- email must be valid;
- password must contain at least six characters.

## Component boundaries

- `LandingPage` owns the public page composition.
- Focused landing components own the header, hero mockup, problem section,
  feature section, and footer where extraction improves readability.
- `LoginPage` owns access-mode selection and page-level feedback.
- Login and registration forms remain independently understandable and
  testable, even if they share small field styles or a password control.
- `auth-api` owns the new register request.
- Auth service contracts own request and response types.
- A registration schema owns client-side validation independently from the
  existing login schema.

The implementation should avoid a generic design-system refactor. Shared
pieces are extracted only when both modes genuinely need them.

## Routing behavior

- `/` renders the public landing page.
- `/login` renders the access page.
- `/dashboard` and `/products` remain protected.
- An unknown route sends an authenticated user to `/dashboard` and an
  anonymous visitor to `/`.
- Existing session restoration and HTTP 401 handling remain unchanged.

## Responsive behavior

- On wide screens, the hero is a two-column layout and the access card is
  centered within a branded background.
- On smaller screens, navigation remains compact, the hero becomes one
  column, and the mobile mockup stays visible without horizontal scrolling.
- Landing cards stack naturally and keep touch targets at least 44 pixels.
- The footer collapses into readable vertical groups.

## Accessibility

- Header navigation uses real links and section IDs.
- The password button has a changing accessible name: "Mostrar senha" and
  "Ocultar senha".
- Mode controls communicate their selected state.
- Form fields retain explicit labels, validation messages, and invalid state.
- Success, API error, and Google availability messages use appropriate live
  regions without interrupting normal typing.
- Decorative mockup content is hidden from assistive technology.
- Colour contrast and keyboard focus remain visible on every action.

## Error handling

- Login preserves the current invalid-credentials, timeout, and connection
  messaging.
- Registration uses the same API-error normalization as login.
- A submission disables only its own submit action and shows an in-progress
  label.
- Switching modes clears stale errors and password values.
- The Google availability message is informational and does not imitate a
  successful sign-in.

## Testing strategy

Tests are written before each behavior and cover:

- landing page content, anchor navigation, and the single header CTA;
- navigation from the landing page to `/login`;
- password visibility toggling in both access modes;
- the Google availability message without an API request;
- switching between login and registration in the same card;
- registration validation without a network request;
- the correct registration payload;
- successful registration returning to login with confirmation and email;
- registration API error feedback;
- preservation of the existing login, session restoration, protected routes,
  dashboard, and products behavior.

Quality gates remain `npm test`, `npm run lint`, `npm run typecheck`, and
`npm run build`.

## Future evolution

When Google OAuth exists in the backend, the current Google action becomes the
provider entry point. A later iteration may simplify both login and
registration to Google-only flows. This design deliberately avoids coupling
the current forms to a provider-specific architecture before that contract
exists.
