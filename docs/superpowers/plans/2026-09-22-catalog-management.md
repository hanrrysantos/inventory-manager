# Catalog Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the complete product and category catalog supported by the current API, with read-only access for `USER` and mutations for `ADMIN`.

**Architecture:** Keep each feature beside its API, query hooks, forms, page, and tests. Use TanStack Query for server state, URL search parameters for list state, and one small native-dialog component shared by product and category flows.

**Tech Stack:** React 19, TypeScript 6, React Router 7, TanStack Query 5, Axios, React Hook Form, Zod, Tailwind CSS 4, Vitest, Testing Library, MSW.

**Spec:** `docs/superpowers/specs/2026-09-22-catalog-management-design.md`

## Global Constraints

- Add no dependency; use the native `<dialog>` element and installed libraries.
- `USER` and `ADMIN` can read products and categories; only `ADMIN` sees mutation controls.
- Product editing changes only `name` and `minStock`; SKU and category stay immutable.
- List page, sort, direction, and low-stock state live in the URL; changing sort or filter resets `page` to zero.
- Use the API response as the final authorization boundary and display `403` without logging out.
- Text search, category/status filters, batches, movements, suppliers, reports, and settings remain outside this plan.
- Use API-backed filtering only; never filter a single loaded page as if it were the complete catalog.

## Review Focus

- Invalid or negative URL page values fall back to page zero instead of issuing an invalid request.
- Changing sort or low-stock mode resets pagination so an existing later page cannot render as falsely empty.
- A `USER` never receives create, edit, or delete controls, while retaining all read and detail controls.
- A `409` category deletion failure keeps the confirmation open and displays the API message.
- Product creation with no available categories explains that a category must be created first and disables submission.

---

### Task 1: Catalog contracts and API functions

**Files:**
- Create: `src/services/contracts/category.ts`
- Modify: `src/services/contracts/product.ts`
- Modify: `src/features/products/products-api.ts`
- Modify: `src/features/products/products-api.test.ts`
- Create: `src/features/categories/categories-api.ts`
- Create: `src/features/categories/categories-api.test.ts`

**Interfaces:**
- Consumes: `apiClient`, `PageQuery`, and `PageResponse<T>`.
- Produces: `Category`, `CategoryInput`, `ProductInput`, `ProductUpdateInput`, `getProduct`, `getLowStockProducts`, `createProduct`, `updateProduct`, `deleteProduct`, and the five category CRUD functions.

- [ ] **Step 1: Write failing product API tests**

Extend `products-api.test.ts` with MSW handlers that capture method, URL, and JSON body, then assert these exact calls:

```ts
await expect(getProduct(7)).resolves.toMatchObject({ id: 7 })
await getLowStockProducts({ page: 1, size: 20, sort: 'sku,desc' })
expect(new URL(requestedUrl).pathname).toBe('/api/v1/products/low-stock')

await createProduct({ name: 'Café', sku: 'CAF-1', minStock: 3, categoryId: 2 })
expect(requestedBody).toEqual({ name: 'Café', sku: 'CAF-1', minStock: 3, categoryId: 2 })

await updateProduct(7, { name: 'Café especial', minStock: 5 })
expect(requestedBody).toEqual({ name: 'Café especial', minStock: 5 })

await deleteProduct(7)
expect(requestedMethod).toBe('DELETE')
```

- [ ] **Step 2: Run product API tests and verify RED**

Run: `npm test -- src/features/products/products-api.test.ts`

Expected: FAIL because the new types and functions do not exist.

- [ ] **Step 3: Add product request contracts and functions**

Add the request types to `product.ts`:

```ts
export interface ProductInput {
  name: string
  sku: string
  minStock: number
  categoryId: number
}

export type ProductUpdateInput = Pick<ProductInput, 'name' | 'minStock'>
```

Implement the direct Axios calls in `products-api.ts`:

```ts
export async function getProduct(id: number): Promise<Product> {
  const { data } = await apiClient.get<Product>(`/api/v1/products/${id}`)
  return data
}

export async function getLowStockProducts(query: PageQuery) {
  const { data } = await apiClient.get<PageResponse<Product>>(
    '/api/v1/products/low-stock',
    { params: query },
  )
  return data
}

export async function createProduct(input: ProductInput): Promise<Product> {
  const { data } = await apiClient.post<Product>('/api/v1/products', input)
  return data
}

export async function updateProduct(id: number, input: ProductUpdateInput): Promise<Product> {
  const { data } = await apiClient.put<Product>(`/api/v1/products/${id}`, input)
  return data
}

export async function deleteProduct(id: number): Promise<void> {
  await apiClient.delete(`/api/v1/products/${id}`)
}
```

- [ ] **Step 4: Run product API tests and verify GREEN**

Run: `npm test -- src/features/products/products-api.test.ts`

Expected: PASS.

- [ ] **Step 5: Write failing category API tests**

Create `categories-api.test.ts` to assert pagination and all CRUD contracts:

```ts
await getCategories({ page: 1, size: 20, sort: 'name,desc' })
expect(new URL(requestedUrl).searchParams.get('page')).toBe('1')

await expect(getCategory(4)).resolves.toMatchObject({ id: 4 })
await createCategory({ name: 'Bebidas', description: 'Líquidos' })
await updateCategory(4, { name: 'Bebidas frias', description: '' })
await deleteCategory(4)

expect(requests).toEqual([
  ['GET', '/api/v1/categories/4'],
  ['POST', '/api/v1/categories'],
  ['PUT', '/api/v1/categories/4'],
  ['DELETE', '/api/v1/categories/4'],
])
```

- [ ] **Step 6: Run category API tests and verify RED**

Run: `npm test -- src/features/categories/categories-api.test.ts`

Expected: FAIL because the category module does not exist.

- [ ] **Step 7: Implement category contracts and API functions**

Create `category.ts`:

```ts
export interface Category {
  id: number
  name: string
  description: string | null
}

export interface CategoryInput {
  name: string
  description: string
}
```

Create `categories-api.ts`:

```ts
export async function getCategories(query: PageQuery): Promise<PageResponse<Category>> {
  const { data } = await apiClient.get<PageResponse<Category>>('/api/v1/categories', { params: query })
  return data
}

export async function getCategory(id: number): Promise<Category> {
  const { data } = await apiClient.get<Category>(`/api/v1/categories/${id}`)
  return data
}

export async function createCategory(input: CategoryInput): Promise<Category> {
  const { data } = await apiClient.post<Category>('/api/v1/categories', input)
  return data
}

export async function updateCategory(id: number, input: CategoryInput): Promise<Category> {
  const { data } = await apiClient.put<Category>(`/api/v1/categories/${id}`, input)
  return data
}

export async function deleteCategory(id: number): Promise<void> {
  await apiClient.delete(`/api/v1/categories/${id}`)
}
```

- [ ] **Step 8: Run both API test files and verify GREEN**

Run: `npm test -- src/features/products/products-api.test.ts src/features/categories/categories-api.test.ts`

Expected: PASS.

- [ ] **Step 9: Commit the API layer**

```bash
git add src/services/contracts/category.ts src/services/contracts/product.ts src/features/products/products-api.ts src/features/products/products-api.test.ts src/features/categories/categories-api.ts src/features/categories/categories-api.test.ts
git commit -m "adiciona api do catalogo"
```

### Task 2: Accessible native catalog dialog

**Files:**
- Create: `src/components/ui/AppDialog.tsx`
- Create: `src/components/ui/AppDialog.test.tsx`
- Modify: `src/test/setup.ts`

**Interfaces:**
- Consumes: React children and the browser `HTMLDialogElement` API.
- Produces: `AppDialog({ title, onClose, children }: AppDialogProps)`.

- [ ] **Step 1: Write the failing dialog behavior test**

```tsx
const user = userEvent.setup()
const onClose = vi.fn()
render(<AppDialog title="Novo produto" onClose={onClose}><button>Salvar</button></AppDialog>)

expect(screen.getByRole('dialog', { name: 'Novo produto' })).toBeVisible()
await user.click(screen.getByRole('button', { name: /fechar/i }))
expect(onClose).toHaveBeenCalledOnce()
```

Add a second assertion that dispatching the native `cancel` event calls
`onClose`, proving Escape is handled by the platform path.

- [ ] **Step 2: Run the dialog test and verify RED**

Run: `npm test -- src/components/ui/AppDialog.test.tsx`

Expected: FAIL because `AppDialog` does not exist.

- [ ] **Step 3: Add the jsdom dialog shim**

In `src/test/setup.ts`, define only the missing browser methods:

```ts
HTMLDialogElement.prototype.showModal ??= function () {
  this.open = true
}
HTMLDialogElement.prototype.close ??= function () {
  this.open = false
  this.dispatchEvent(new Event('close'))
}
```

- [ ] **Step 4: Implement the minimal native dialog**

`AppDialog.tsx` should call `showModal()` in an effect, convert `cancel` into a
native close, and notify the parent from the dialog's `close` event:

```tsx
export function AppDialog({ title, onClose, children }: AppDialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => { ref.current?.showModal() }, [])

  return (
    <dialog ref={ref} aria-labelledby="app-dialog-title"
      onCancel={(event) => { event.preventDefault(); ref.current?.close() }}
      onClose={onClose}
      className="m-auto w-[min(92vw,34rem)] rounded-3xl p-0 backdrop:bg-[#15281b]/35">
      <section className="p-6">
        <div className="flex items-center justify-between gap-4">
          <h2 id="app-dialog-title" className="text-xl font-semibold">{title}</h2>
          <button type="button" aria-label="Fechar" onClick={() => ref.current?.close()}>
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>
        <div className="mt-5">{children}</div>
      </section>
    </dialog>
  )
}
```

Import `X` from the installed `lucide-react` package.

- [ ] **Step 5: Run the dialog test and verify GREEN**

Run: `npm test -- src/components/ui/AppDialog.test.tsx`

Expected: PASS without accessibility or React warnings.

- [ ] **Step 6: Commit the dialog**

```bash
git add src/components/ui/AppDialog.tsx src/components/ui/AppDialog.test.tsx src/test/setup.ts
git commit -m "adiciona dialogo do catalogo"
```

### Task 3: Read-only categories page and navigation

**Files:**
- Create: `src/features/categories/use-categories.ts`
- Create: `src/features/categories/CategoriesPage.tsx`
- Create: `src/features/categories/CategoriesPage.test.tsx`
- Modify: `src/app/router/AppRouter.tsx`
- Modify: `src/components/layout/Sidebar.tsx`
- Modify: `src/components/layout/AppHeader.tsx`
- Modify: `src/components/layout/AppLayout.test.tsx`
- Modify: `src/test/handlers.ts`

**Interfaces:**
- Consumes: Task 1 category API and Task 2 `AppDialog`.
- Produces: authenticated `/categories`, category list/detail UI, and `useCategories(query)`.

- [ ] **Step 1: Add category fixtures and a default GET handler**

Add to `handlers.ts`:

```ts
export const categoryFixtures = [
  { id: 1, name: 'Bebidas', description: 'Bebidas e infusões' },
  { id: 2, name: 'Alimentos', description: null },
]

http.get('*/api/v1/categories', () => HttpResponse.json({
  content: categoryFixtures,
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
})),
http.get('*/api/v1/categories/:id', ({ params }) =>
  HttpResponse.json(categoryFixtures.find(({ id }) => id === Number(params.id))),
),
```

- [ ] **Step 2: Write failing page and shell tests**

`CategoriesPage.test.tsx` must prove:

```tsx
expect(await screen.findByText('Bebidas')).toBeVisible()
expect(screen.getByText('2 categorias no catálogo')).toBeVisible()
await user.click(screen.getByRole('button', { name: /ver bebidas/i }))
expect(await screen.findByRole('dialog', { name: /detalhes da categoria/i })).toBeVisible()
```

Add tests that `?page=-2` requests page zero, changing the sort control sets
`sort=name,desc` and removes `page`, and an empty response renders
"Nenhuma categoria cadastrada". Update `AppLayout.test.tsx` to expect the
Categorias navigation link.

- [ ] **Step 3: Run the page and shell tests and verify RED**

Run: `npm test -- src/features/categories/CategoriesPage.test.tsx src/components/layout/AppLayout.test.tsx`

Expected: FAIL because the route, page, and link do not exist.

- [ ] **Step 4: Implement the category query hook**

```ts
export function useCategories(query: PageQuery) {
  return useQuery({
    queryKey: ['categories', query],
    queryFn: () => getCategories(query),
  })
}
```

- [ ] **Step 5: Implement the read-only categories page**

Parse URL state with explicit allowlists:

```ts
const pageValue = Number(searchParams.get('page') ?? 0)
const page = Number.isInteger(pageValue) && pageValue >= 0 ? pageValue : 0
const property = ['id', 'name'].includes(searchParams.get('sort') ?? '')
  ? searchParams.get('sort')!
  : 'name'
const direction = searchParams.get('direction') === 'desc' ? 'desc' : 'asc'
```

Render the existing loading, error, empty, and `PaginationControls` patterns.
Add a native `<select>` for property/direction and a "Ver {name}" row button.
On detail click, call `getCategory(id)` through `useQuery` enabled only while
an ID is selected, then show `AppDialog`.

- [ ] **Step 6: Register navigation and route**

Lazy-load `CategoriesPage` in `AppRouter.tsx`, add `/categories`, add a
`Tags` link in `Sidebar.tsx`, and map `/categories` to `Categorias` in
`AppHeader.tsx`.

- [ ] **Step 7: Run the page and shell tests and verify GREEN**

Run: `npm test -- src/features/categories/CategoriesPage.test.tsx src/components/layout/AppLayout.test.tsx`

Expected: PASS.

- [ ] **Step 8: Commit read-only categories**

```bash
git add src/features/categories src/app/router/AppRouter.tsx src/components/layout/Sidebar.tsx src/components/layout/AppHeader.tsx src/components/layout/AppLayout.test.tsx src/test/handlers.ts
git commit -m "adiciona consulta de categorias"
```

### Task 4: Category administration

**Files:**
- Create: `src/features/categories/category-schema.ts`
- Create: `src/features/categories/CategoryForm.tsx`
- Create: `src/features/categories/category-schema.test.ts`
- Modify: `src/features/categories/CategoriesPage.tsx`
- Modify: `src/features/categories/CategoriesPage.test.tsx`

**Interfaces:**
- Consumes: category CRUD API, `AppDialog`, `useAuth`, `getApiErrorMessage`, and `useQueryClient`.
- Produces: validated category create/edit forms and delete confirmation for `ADMIN`.

- [ ] **Step 1: Write failing schema tests**

```ts
expect(categorySchema.safeParse({ name: ' ', description: '' }).success).toBe(false)
expect(categorySchema.safeParse({ name: 'a'.repeat(101), description: '' }).success).toBe(false)
expect(categorySchema.parse({ name: ' Bebidas ', description: '' })).toEqual({
  name: 'Bebidas',
  description: '',
})
```

- [ ] **Step 2: Run schema tests and verify RED**

Run: `npm test -- src/features/categories/category-schema.test.ts`

Expected: FAIL because the schema does not exist.

- [ ] **Step 3: Implement the category schema**

```ts
export const categorySchema = z.object({
  name: z.string().trim().min(1, 'Informe o nome').max(100, 'Use no máximo 100 caracteres'),
  description: z.string(),
})
export type CategoryFormData = z.infer<typeof categorySchema>
```

- [ ] **Step 4: Run schema tests and verify GREEN**

Run: `npm test -- src/features/categories/category-schema.test.ts`

Expected: PASS.

- [ ] **Step 5: Write failing ADMIN and USER page tests**

For `ADMIN`, click "Nova categoria", submit valid data, assert the POST body,
and assert the success notice. Edit a fixture and assert PUT. Confirm deletion
and assert DELETE.

Override `/users/me` with role `USER` and assert:

```tsx
expect(await screen.findByText('Bebidas')).toBeVisible()
expect(screen.queryByRole('button', { name: /nova categoria/i })).not.toBeInTheDocument()
expect(screen.queryByRole('button', { name: /editar bebidas/i })).not.toBeInTheDocument()
expect(screen.queryByRole('button', { name: /excluir bebidas/i })).not.toBeInTheDocument()
expect(screen.getByRole('button', { name: /ver bebidas/i })).toBeVisible()
```

Add the Review Focus failure: return `409` with message
"Esta categoria possui produtos" on DELETE, then assert the dialog remains
visible and the message has `role="alert"`.

- [ ] **Step 6: Run category page tests and verify RED**

Run: `npm test -- src/features/categories/CategoriesPage.test.tsx`

Expected: FAIL because admin controls do not exist.

- [ ] **Step 7: Implement CategoryForm and mutations**

Use `useForm<CategoryFormData>` with `zodResolver(categorySchema)`. Accept
`initialValue?: Category` and `onSuccess(message: string)`. POST when absent,
PUT when present. On success:

```ts
await queryClient.invalidateQueries({ queryKey: ['categories'] })
onSuccess(initialValue ? 'Categoria atualizada.' : 'Categoria criada.')
```

In `CategoriesPage`, derive `const isAdmin = user?.role === 'ADMIN'`. Render
admin buttons only inside that branch. Delete inside `try/catch`; close and
invalidate on success, retain the dialog and call `getApiErrorMessage` on
failure.

- [ ] **Step 8: Run category tests and verify GREEN**

Run: `npm test -- src/features/categories/category-schema.test.ts src/features/categories/CategoriesPage.test.tsx`

Expected: PASS.

- [ ] **Step 9: Commit category administration**

```bash
git add src/features/categories
git commit -m "adiciona gestao de categorias"
```

### Task 5: Product sorting, low-stock filter, and details

**Files:**
- Modify: `src/features/products/use-products.ts`
- Modify: `src/features/products/ProductsPage.tsx`
- Modify: `src/features/products/ProductTable.tsx`
- Modify: `src/features/products/ProductsPage.test.tsx`
- Modify: `src/test/handlers.ts`

**Interfaces:**
- Consumes: product read functions from Task 1 and `AppDialog` from Task 2.
- Produces: `useProducts(query, lowStock?)`, URL-backed toolbar, and product details.

- [ ] **Step 1: Add default product detail and low-stock handlers**

```ts
http.get('*/api/v1/products/low-stock', () => HttpResponse.json({
  content: productFixtures.filter(({ totalQuantity, minStock }) => totalQuantity <= minStock),
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
})),
http.get('*/api/v1/products/:id', ({ params }) =>
  HttpResponse.json(productFixtures.find(({ id }) => id === Number(params.id))),
),
```

- [ ] **Step 2: Write failing product read tests**

Test that selecting "Estoque baixo" requests `/products/low-stock`, adds
`lowStock=true`, and removes an existing `page=1`. Test selecting SKU
descending produces `sort=sku&direction=desc` and sends `sort=sku,desc` to the
API. Test `?page=-1` sends page zero. Click "Ver Chá Verde Orgânico" and
assert a details dialog with SKU, category, quantity, and minimum stock.

- [ ] **Step 3: Run product page tests and verify RED**

Run: `npm test -- src/features/products/ProductsPage.test.tsx`

Expected: FAIL because the toolbar and details actions do not exist.

- [ ] **Step 4: Route the query to the correct endpoint**

```ts
export function useProducts(query: PageQuery, lowStock = false) {
  return useQuery({
    queryKey: ['products', lowStock ? 'low-stock' : 'all', query],
    queryFn: () => lowStock ? getLowStockProducts(query) : getProducts(query),
  })
}
```

- [ ] **Step 5: Add URL-backed product controls**

Allowlist `id`, `name`, and `sku`; allowlist `asc` and `desc`; treat only
`lowStock=true` as active. Use the same URL update helper for all controls and
delete `page` whenever a non-page value changes. Render native selects and a
checkbox/button labeled "Somente estoque baixo".

- [ ] **Step 6: Add row actions and details**

Change `ProductTable` to accept:

```ts
interface ProductTableProps {
  products: Product[]
  compact?: boolean
  onView?: (product: Product) => void
  onEdit?: (product: Product) => void
  onDelete?: (product: Product) => void
}
```

Render an Actions column only when at least one callback exists. Details remain
available to both roles. Fetch the selected product by ID before opening the
details `AppDialog`.

- [ ] **Step 7: Run product read tests and verify GREEN**

Run: `npm test -- src/features/products/ProductsPage.test.tsx`

Expected: PASS, including existing pagination tests.

- [ ] **Step 8: Commit product reading controls**

```bash
git add src/features/products src/test/handlers.ts
git commit -m "adiciona filtros e detalhes de produtos"
```

### Task 6: Product administration

**Files:**
- Create: `src/features/products/product-schema.ts`
- Create: `src/features/products/product-schema.test.ts`
- Create: `src/features/products/ProductForm.tsx`
- Modify: `src/features/products/ProductsPage.tsx`
- Modify: `src/features/products/ProductsPage.test.tsx`

**Interfaces:**
- Consumes: Tasks 1-5, `useCategories`, `useAuth`, and `getApiErrorMessage`.
- Produces: validated create/edit forms and deletion for `ADMIN`.

- [ ] **Step 1: Write failing product schema tests**

```ts
expect(productCreateSchema.safeParse({ name: '', sku: '', minStock: -1, categoryId: 0 }).success).toBe(false)
expect(productCreateSchema.safeParse({ name: 'a'.repeat(256), sku: 'SKU', minStock: 1, categoryId: 1 }).success).toBe(false)
expect(productCreateSchema.safeParse({ name: 'Café', sku: 'a'.repeat(51), minStock: 1, categoryId: 1 }).success).toBe(false)
expect(productCreateSchema.safeParse({ name: 'Café', sku: 'CAF-1', minStock: 1.5, categoryId: 1 }).success).toBe(false)
```

- [ ] **Step 2: Run schema tests and verify RED**

Run: `npm test -- src/features/products/product-schema.test.ts`

Expected: FAIL because the schemas do not exist.

- [ ] **Step 3: Implement create and edit schemas**

```ts
const name = z.string().trim().min(1, 'Informe o nome').max(255, 'Use no máximo 255 caracteres')
const minStock = z.coerce.number().int('Use um número inteiro').min(0, 'O estoque mínimo não pode ser negativo')

export const productCreateSchema = z.object({
  name,
  sku: z.string().trim().min(1, 'Informe o SKU').max(50, 'Use no máximo 50 caracteres'),
  minStock,
  categoryId: z.coerce.number().int().positive('Selecione uma categoria'),
})
export const productEditSchema = z.object({ name, minStock })
```

- [ ] **Step 4: Run schema tests and verify GREEN**

Run: `npm test -- src/features/products/product-schema.test.ts`

Expected: PASS.

- [ ] **Step 5: Write failing product administration tests**

For `ADMIN`, create a product and assert the full POST body. Edit it and assert
PUT contains only `{ name, minStock }`. Delete after confirmation and assert
DELETE. Assert each success notice and refreshed list request.

For `USER`, assert view remains present while new/edit/delete controls are
absent. For an empty categories response, open "Novo produto", assert
"Cadastre uma categoria antes de criar um produto" and that submit is
disabled.

- [ ] **Step 6: Run product page tests and verify RED**

Run: `npm test -- src/features/products/ProductsPage.test.tsx`

Expected: FAIL because product administration is absent.

- [ ] **Step 7: Implement ProductForm**

Use `useCategories({ page: 0, size: 100, sort: 'name,asc' })` for the create
selector. Add this explicit ceiling beside the query:

```ts
// ponytail: first 100 categories; add a paginated searchable selector when a catalog exceeds that ceiling.
```

Create mode renders name, SKU, minimum stock, and category. Edit mode renders
only name and minimum stock. Disable submit while categories are loading,
empty, or the request is pending. On success invalidate:

```ts
await Promise.all([
  queryClient.invalidateQueries({ queryKey: ['products'] }),
  queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
])
```

- [ ] **Step 8: Wire ADMIN actions and delete confirmation**

In `ProductsPage`, pass `onEdit` and `onDelete` only when
`user?.role === 'ADMIN'`. Keep deletion errors inside the confirmation dialog.
On successful deletion close it, show "Produto excluído.", and invalidate
`products` and `dashboard`.

- [ ] **Step 9: Run product administration tests and verify GREEN**

Run: `npm test -- src/features/products/product-schema.test.ts src/features/products/ProductsPage.test.tsx`

Expected: PASS.

- [ ] **Step 10: Commit product administration**

```bash
git add src/features/products
git commit -m "adiciona gestao de produtos"
```

### Task 7: Documentation and complete verification

**Files:**
- Modify: `README.md`
- Modify if needed by verified regressions: files already named in Tasks 1-6 only.

**Interfaces:**
- Consumes: the complete catalog implementation.
- Produces: accurate project documentation and a verified build.

- [ ] **Step 1: Update README functionality**

Replace the product row and obsolete limitation paragraph with:

```md
| Produtos (`/products`) | Consulta paginada, ordenação, estoque baixo, detalhes e administração por `ADMIN` |
| Categorias (`/categories`) | Consulta paginada, detalhes e administração por `ADMIN` |

Busca textual e filtros por categoria/status, movimentações, fornecedores,
relatórios e configurações ainda não estão disponíveis nesta versão.
```

- [ ] **Step 2: Run the focused catalog tests**

Run: `npm test -- src/features/products src/features/categories src/components/ui/AppDialog.test.tsx src/components/layout/AppLayout.test.tsx`

Expected: PASS with no warnings.

- [ ] **Step 3: Run the complete test suite**

Run: `npm test`

Expected: all tests PASS with no unhandled MSW requests or React warnings.

- [ ] **Step 4: Run static verification**

Run: `npm run lint && npm run typecheck && npm run build`

Expected: all commands exit 0.

- [ ] **Step 5: Check the diff**

Run: `git diff --check && git status --short`

Expected: no whitespace errors; only catalog implementation, tests, and README changes remain.

- [ ] **Step 6: Commit documentation or final corrections**

```bash
git add README.md src
git commit -m "documenta funcionalidades do catalogo"
```
