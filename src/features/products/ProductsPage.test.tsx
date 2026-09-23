import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from '../../app/App'
import { authenticatedUser, productFixtures } from '../../test/handlers'
import { server } from '../../test/server'

describe('ProductsPage', () => {
  beforeEach(() => {
    localStorage.setItem('inventory-manager.token', 'valid-token')
    window.history.pushState({}, '', '/products')
  })

  it('renders product data returned by the API', async () => {
    render(<App />)

    expect(await screen.findByText('Chá Verde Orgânico')).toBeVisible()
    expect(screen.getByText('CHA-001')).toBeVisible()
    expect(screen.getByText('Bebidas')).toBeVisible()
    expect(screen.queryByText(/preço/i)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /novo produto/i })).toBeVisible()
  })

  it('shows the total and navigates between product pages', async () => {
    server.use(
      http.get('*/api/v1/products', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0)
        return HttpResponse.json({
          content: [productFixtures[page]],
          page,
          size: 20,
          totalElements: 2,
          totalPages: 2,
        })
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    expect(await screen.findByText('Chá Verde Orgânico')).toBeVisible()
    expect(screen.getByText('2 produtos no catálogo')).toBeVisible()
    expect(screen.getByText('Página 1 de 2')).toBeVisible()
    expect(screen.getByRole('button', { name: /página anterior/i })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: /próxima página/i }))

    expect(await screen.findByText('Mel Silvestre 500g')).toBeVisible()
    expect(screen.queryByText('Chá Verde Orgânico')).not.toBeInTheDocument()
    expect(screen.getByText('Página 2 de 2')).toBeVisible()
    expect(screen.getByRole('button', { name: /próxima página/i })).toBeDisabled()
    expect(window.location.search).toContain('page=1')
  })

  it('renders an empty catalog', async () => {
    server.use(
      http.get('*/api/v1/products', () =>
        HttpResponse.json({
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        }),
      ),
    )
    render(<App />)

    expect(await screen.findByText(/nenhum produto cadastrado/i)).toBeVisible()
  })

  it('returns to the last valid page instead of showing a false empty catalog', async () => {
    window.history.replaceState({}, '', '/products?page=2')
    server.use(
      http.get('*/api/v1/products', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        return HttpResponse.json({
          content: page === 0 ? productFixtures : [],
          page,
          size: 20,
          totalElements: 3,
          totalPages: 1,
        })
      }),
    )

    render(<App />)

    expect(await screen.findByText('Chá Verde Orgânico')).toBeVisible()
    expect(new URLSearchParams(window.location.search).has('page')).toBe(false)
    expect(screen.queryByText(/nenhum produto cadastrado/i)).not.toBeInTheDocument()
  })

  it('shows an error with a retry action when the API fails', async () => {
    server.use(
      http.get('*/api/v1/products', () =>
        HttpResponse.json({ message: 'Falha temporária' }, { status: 500 }),
      ),
    )
    const user = userEvent.setup()
    render(<App />)

    expect(
      await screen.findByText(/não foi possível carregar os produtos/i, {}, { timeout: 3000 }),
    ).toBeVisible()

    server.use(
      http.get('*/api/v1/products', () =>
        HttpResponse.json({
          content: productFixtures,
          page: 0,
          size: 20,
          totalElements: productFixtures.length,
          totalPages: 1,
        }),
      ),
    )
    await user.click(screen.getByRole('button', { name: /tentar novamente/i }))
    expect(await screen.findByText('Chá Verde Orgânico')).toBeVisible()
  })

  it('uses the low-stock endpoint and resets pagination', async () => {
    window.history.replaceState({}, '', '/products?page=1')
    let requestedUrl = ''
    server.use(
      http.get('*/api/v1/products/low-stock', ({ request }) => {
        requestedUrl = request.url
        return HttpResponse.json({
          content: productFixtures.slice(1),
          page: 0,
          size: 20,
          totalElements: 2,
          totalPages: 1,
        })
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      await screen.findByRole('checkbox', { name: /somente estoque baixo/i }),
    )

    await waitFor(() => expect(requestedUrl).not.toBe(''))
    expect(new URL(requestedUrl).pathname).toBe('/api/v1/products/low-stock')
    expect(window.location.search).toContain('lowStock=true')
    expect(window.location.search).not.toContain('page=')
  })

  it('stores sorting in the URL and sends it to the API', async () => {
    let requestedSort = ''
    server.use(
      http.get('*/api/v1/products', ({ request }) => {
        requestedSort = new URL(request.url).searchParams.get('sort') ?? ''
        return HttpResponse.json({
          content: productFixtures,
          page: 0,
          size: 20,
          totalElements: 3,
          totalPages: 1,
        })
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.selectOptions(
      await screen.findByRole('combobox', { name: /ordenar produtos/i }),
      'sku,desc',
    )

    await waitFor(() => expect(requestedSort).toBe('sku,desc'))
    const searchParams = new URLSearchParams(window.location.search)
    expect(searchParams.get('sort')).toBe('sku')
    expect(searchParams.get('direction')).toBe('desc')
  })

  it('normalizes an invalid page before requesting products', async () => {
    window.history.replaceState({}, '', '/products?page=-1')
    let requestedPage: string | null = null
    server.use(
      http.get('*/api/v1/products', ({ request }) => {
        requestedPage = new URL(request.url).searchParams.get('page')
        return HttpResponse.json({
          content: productFixtures,
          page: 0,
          size: 20,
          totalElements: 3,
          totalPages: 1,
        })
      }),
    )

    render(<App />)

    expect(await screen.findByText('Chá Verde Orgânico')).toBeVisible()
    expect(requestedPage).toBe('0')
  })

  it('opens API-backed product details', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      await screen.findByRole('button', { name: /ver chá verde orgânico/i }),
    )

    const dialog = await screen.findByRole('dialog', {
      name: /detalhes do produto/i,
    })
    expect(within(dialog).getByText('CHA-001')).toBeVisible()
    expect(within(dialog).getByText('Bebidas')).toBeVisible()
    expect(within(dialog).getByText('142')).toBeVisible()
    expect(within(dialog).getByText('40')).toBeVisible()
  })

  it('allows an admin to create a product and refreshes the list', async () => {
    let requestBody: unknown
    let listRequests = 0
    server.use(
      http.get('*/api/v1/products', () => {
        listRequests += 1
        return HttpResponse.json({
          content: productFixtures,
          page: 0,
          size: 20,
          totalElements: 3,
          totalPages: 1,
        })
      }),
      http.post('*/api/v1/products', async ({ request }) => {
        requestBody = await request.json()
        return HttpResponse.json(
          {
            id: 4,
            name: 'Café',
            sku: 'CAF-1',
            totalQuantity: 0,
            categoryName: 'Bebidas',
            minStock: 3,
          },
          { status: 201 },
        )
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: /novo produto/i }))
    await user.type(screen.getByLabelText('Nome'), 'Café')
    await user.type(screen.getByLabelText('SKU'), 'CAF-1')
    await user.type(screen.getByLabelText(/estoque mínimo/i), '3')
    await user.selectOptions(screen.getByLabelText('Categoria'), '1')
    await user.click(screen.getByRole('button', { name: /salvar produto/i }))

    expect(requestBody).toEqual({
      name: 'Café',
      sku: 'CAF-1',
      minStock: 3,
      categoryId: 1,
    })
    expect(await screen.findByText('Produto criado.')).toBeVisible()
    await waitFor(() => expect(listRequests).toBeGreaterThan(1))
  })

  it('allows an admin to edit only supported product fields', async () => {
    let requestBody: unknown
    let listRequests = 0
    server.use(
      http.get('*/api/v1/products', () => {
        listRequests += 1
        return HttpResponse.json({
          content: productFixtures,
          page: 0,
          size: 20,
          totalElements: 3,
          totalPages: 1,
        })
      }),
      http.put('*/api/v1/products/:id', async ({ request }) => {
        requestBody = await request.json()
        return HttpResponse.json({
          ...productFixtures[0],
          ...(requestBody as object),
        })
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      await screen.findByRole('button', { name: /editar chá verde orgânico/i }),
    )
    const name = screen.getByLabelText('Nome')
    await user.clear(name)
    await user.type(name, 'Chá Verde Premium')
    const minimum = screen.getByLabelText(/estoque mínimo/i)
    await user.clear(minimum)
    await user.type(minimum, '50')
    expect(screen.queryByLabelText('SKU')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Categoria')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /salvar alterações/i }))

    expect(requestBody).toEqual({ name: 'Chá Verde Premium', minStock: 50 })
    expect(await screen.findByText('Produto atualizado.')).toBeVisible()
    await waitFor(() => expect(listRequests).toBeGreaterThan(1))
  })

  it('allows an admin to delete a product after confirmation', async () => {
    let requestedId = ''
    let listRequests = 0
    server.use(
      http.get('*/api/v1/products', () => {
        listRequests += 1
        return HttpResponse.json({
          content: productFixtures,
          page: 0,
          size: 20,
          totalElements: 3,
          totalPages: 1,
        })
      }),
      http.delete('*/api/v1/products/:id', ({ params }) => {
        requestedId = String(params.id)
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      await screen.findByRole('button', { name: /excluir chá verde orgânico/i }),
    )
    await user.click(screen.getByRole('button', { name: /confirmar exclusão/i }))

    expect(requestedId).toBe('1')
    expect(await screen.findByText('Produto excluído.')).toBeVisible()
    await waitFor(() => expect(listRequests).toBeGreaterThan(1))
  })

  it('keeps product administration hidden from regular users', async () => {
    server.use(
      http.get('*/api/v1/users/me', () =>
        HttpResponse.json({ ...authenticatedUser, role: 'USER' }),
      ),
    )
    render(<App />)

    expect(await screen.findByText('Chá Verde Orgânico')).toBeVisible()
    expect(screen.queryByRole('button', { name: /novo produto/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /editar chá verde/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /excluir chá verde/i })).not.toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /ver chá verde orgânico/i }),
    ).toBeVisible()
  })

  it('blocks product creation until a category exists', async () => {
    server.use(
      http.get('*/api/v1/categories', () =>
        HttpResponse.json({
          content: [],
          page: 0,
          size: 100,
          totalElements: 0,
          totalPages: 0,
        }),
      ),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: /novo produto/i }))

    expect(
      await screen.findByText(/cadastre uma categoria antes de criar um produto/i),
    ).toBeVisible()
    expect(screen.getByRole('button', { name: /salvar produto/i })).toBeDisabled()
  })
})
