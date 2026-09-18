# Inventory Manager Frontend MVP Design

## Status

Design approved in conversation on 2026-09-17.

This document specifies the first frontend delivery for Inventory Manager. The
frontend is maintained in a repository separate from the Spring Boot backend.

## Objective

Build a responsive inventory administration SPA inspired by the supplied visual
reference. The first delivery includes authentication, authenticated session
restoration, the main application layout, a dashboard backed by real API data,
and a searchable and filterable product list.

Product creation and editing are intentionally deferred to a later delivery.

## Technology stack

- React;
- TypeScript;
- Vite;
- React Router;
- TanStack Query;
- Axios;
- React Hook Form;
- Zod;
- Tailwind CSS;
- Lucide React;
- Vitest;
- Testing Library;
- MSW.

Tailwind CSS and project-owned components will be used instead of a complete UI
framework. This gives the project enough control to reproduce the visual
reference without inheriting another design system's appearance.

## Backend integration

The deployed API base URL is:

```text
https://inventory.hanrry.top
```

The frontend must read the API base URL from `VITE_API_URL`. No production URL
may be hard-coded into feature components.

The OpenAPI contract was validated at:

```text
https://inventory.hanrry.top/v3/api-docs
```

### Endpoints used by the MVP

```text
POST /api/v1/auth/login
GET  /api/v1/users/me
GET  /api/v1/dashboard/summary
GET  /api/v1/products
```

### Core contracts

Login request and response:

```json
{
  "email": "admin@email.com",
  "password": "admin123"
}
```

```json
{
  "token": "jwt-token"
}
```

Current user:

```json
{
  "id": 1,
  "name": "Ana Souza",
  "email": "ana@example.com",
  "role": "ADMIN",
  "createdAt": "2026-09-17T12:00:00"
}
```

Dashboard summary:

```json
{
  "totalQuantity": 714,
  "productCount": 8,
  "lowStockCount": 3,
  "outOfStockCount": 1,
  "expiredBatchCount": 2,
  "inventoryValue": 21480.00,
  "attentionItems": [
    {
      "productId": 3,
      "productName": "Sabonete Natural Lavanda",
      "sku": "SAB-207",
      "quantity": 0,
      "minStock": 25,
      "status": "OUT_OF_STOCK"
    }
  ]
}
```

Product:

```json
{
  "id": 1,
  "name": "Chá Verde Orgânico",
  "sku": "CHA-001",
  "totalQuantity": 142,
  "categoryName": "Bebidas",
  "minStock": 40
}
```

## Application architecture

The SPA will be organized by feature. Shared UI and infrastructure must remain
separate from domain-specific behavior.

```text
src/
├── app/
│   ├── router/
│   ├── providers/
│   └── query-client.ts
├── features/
│   ├── auth/
│   ├── dashboard/
│   └── products/
├── components/
│   ├── layout/
│   ├── feedback/
│   └── ui/
├── services/
│   ├── api-client.ts
│   └── contracts/
├── hooks/
├── lib/
├── styles/
└── test/
```

Each feature owns its API operations, queries, components, and feature-specific
tests. Generic visual primitives remain under `components/ui`, while application
shell components remain under `components/layout`.

## Routes

The first delivery contains:

```text
/login
/dashboard
/products
```

`/login` is public. `/dashboard` and `/products` require a valid authenticated
session. Unknown routes redirect to the appropriate entry point based on session
state.

## Authentication and session flow

1. The login form validates e-mail and password locally.
2. The frontend sends credentials to `/api/v1/auth/login`.
3. The returned JWT is stored in `localStorage` because the current backend uses
   bearer tokens and does not provide an HttpOnly session cookie.
4. Axios includes `Authorization: Bearer <token>` on authenticated requests.
5. On application startup, a stored token triggers `/api/v1/users/me`.
6. A successful response hydrates the current user and role.
7. A `401` response clears the token, clears private query data, and redirects to
   `/login`.
8. Logout performs the same local cleanup without requiring a backend endpoint.

The UI may use the role to control visibility, but backend authorization remains
authoritative. The frontend must not treat hidden controls as a security
boundary.

The accepted local-storage approach carries the normal XSS exposure of
browser-readable tokens. Migrating to an HttpOnly cookie requires a separate
backend contract and is outside this MVP.

## Visual design

The visual language follows the supplied dashboard reference:

- light neutral background with a subtle green tint;
- fixed desktop sidebar and collapsible mobile navigation;
- green as the primary action and healthy-state color;
- rounded white cards with subtle borders and shadows;
- compact typography with strong numeric hierarchy;
- restrained yellow and red colors for warning and critical states;
- tables with generous spacing and status pills;
- clear empty, loading, and error states.

The reference is directional rather than a source of data or functionality.
Unsupported menu entries and fields must not be simulated.

### Responsive behavior

- Desktop: persistent sidebar, four summary cards, product area and attention
  panel displayed side by side when space permits.
- Tablet: reduced gutters, wrapping cards, and stacked lower panels.
- Mobile: drawer-style navigation, single-column cards, stacked panels, and
  horizontally scrollable tables.

The application must remain usable with keyboard navigation and visible focus
states. Semantic headings, labels, buttons, and table elements are required.

## Application shell

### Sidebar

The MVP sidebar contains brand identity, Dashboard, Products, and logout access
on compact layouts when appropriate. Suppliers, Reports, Settings, Movements,
and other unsupported routes are not shown in this delivery.

### Header

The authenticated header displays the current page title, a greeting using the
current user's first name, user initials, full name, formatted role, and logout
action.

The notification control in the visual reference is omitted unless it has a
real interaction. The dashboard attention count may be introduced later as a
real notification source.

## Dashboard

The dashboard consumes `/api/v1/dashboard/summary` and `/api/v1/products`.

### Summary cards

The four primary cards are:

1. total quantity in inventory, with product count as supporting text;
2. low-stock product count;
3. out-of-stock product count;
4. total inventory value formatted as Brazilian currency.

`expiredBatchCount` remains in the typed API contract for a future
expired-batches view or secondary indicator.

### Product preview

The dashboard product table is a compact view built from the same reusable
product table used by the Products page. It includes product name, SKU,
category, current quantity, minimum stock, and derived inventory status.

Price and last-updated columns from the visual reference are omitted because
the product endpoint does not provide those fields.

### Attention panel

The attention panel renders `attentionItems` from the dashboard endpoint. Each
item displays product name, current and minimum quantities, status, and a
progress visualization capped to a safe display range.

Status labels map as follows:

- `IN_STOCK`: Em estoque;
- `LOW_STOCK`: Estoque baixo;
- `OUT_OF_STOCK`: Em falta.

## Products page

The Products page loads `/api/v1/products` and displays all returned products.
The current backend does not paginate this endpoint, so search and filters are
client-side for the MVP.

The page supports search by product name or SKU, filtering by all, in-stock,
low-stock, and out-of-stock, a result count, empty results distinct from an
empty catalog, and retry after API failure.

Product status is derived only for presentation:

```text
totalQuantity <= 0        -> OUT_OF_STOCK
totalQuantity <= minStock -> LOW_STOCK
otherwise                 -> IN_STOCK
```

The product-creation button is omitted because product creation and editing
were explicitly deferred.

## Data fetching and cache

TanStack Query manages server state. Expected query keys are `currentUser`,
`dashboardSummary`, and `products`. The dashboard may share cached product data
with the Products page.

Server data must not be duplicated into a global client-state store. Local
search, filters, navigation state, and dialog state remain component state or
URL state where appropriate.

Axios provides one API client with a base URL from `VITE_API_URL`, JSON
defaults, a bounded timeout, bearer-token request interception, centralized
`401` handling, and normalized access to the backend error payload.

## Error handling

The UI maps failures into actionable messages:

- `400` or `422`: invalid request data;
- `401`: session expired, followed by logout and redirect;
- `403`: authenticated user lacks permission;
- `404`: requested resource is unavailable;
- `409`: business conflict;
- network error or timeout: service unavailable, with retry;
- unexpected failure: generic error with no technical stack or secret data.

Feature-level errors remain within their page or panel. Authentication errors
remain within the login form. A temporary backend failure must not destroy a
valid session unless the response is specifically `401`.

## Loading and empty states

- Initial session restoration shows an application-level loading state.
- Dashboard cards and panels use layout-preserving skeletons.
- Product loading preserves the table frame.
- Empty catalog, no search matches, and API failure are visually distinct.
- Retry actions refetch the failed query.

## Testing strategy

Vitest, Testing Library, and MSW cover behavior at component and integration
boundaries.

Required scenarios:

- successful and failed login;
- local validation prevents invalid login submission;
- private routes redirect anonymous users;
- a stored token restores the user through `/users/me`;
- `401` clears the session and redirects to login;
- logout clears token and private cached data;
- dashboard summary renders and formats API values;
- attention states render from API status values;
- product search matches name and SKU;
- product status filters work;
- loading, empty, no-results, and API-error states render correctly;
- core responsive navigation behavior works without exact-pixel assertions.

MSW handlers must match the real OpenAPI shapes. Tests must not invent fields
that are absent from the API.

## Build and environment

The repository must include:

- `.env.example` with `VITE_API_URL`;
- scripts for development, build, lint, type checking, and tests;
- production build output excluded from Git;
- no real secrets or JWTs committed;
- README instructions for local setup and API configuration.

The production build is a static SPA. Hosting configuration must support
fallback to `index.html` for client-side routes.

## Out of scope

- product creation, editing, and deletion;
- category management;
- batch creation and stock entry forms;
- stock consumption forms;
- movement history;
- suppliers;
- reports and exports;
- settings;
- real-time notifications;
- charts based on unavailable historical data;
- password reset;
- refresh tokens;
- backend changes;
- OpenAPI client generation automation.

These exclusions prevent the UI from implying capabilities that the MVP does
not yet support.

## Completion criteria

The MVP is complete when:

- users can sign in against the deployed API;
- authenticated sessions restore correctly after page reload;
- anonymous users cannot access private routes;
- dashboard cards, attention items, and product preview use real API data;
- the Products page supports local search and status filters;
- desktop and mobile layouts are usable and visually consistent with the
  reference;
- errors, loading, empty states, logout, and token expiration are handled;
- automated tests pass;
- linting and type checking pass;
- the production build completes successfully;
- no unsupported backend features or fabricated data are exposed.
