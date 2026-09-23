import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from '../../app/App'
import { productFixtures } from '../../test/handlers'
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
    expect(screen.queryByRole('button', { name: /novo produto/i })).not.toBeInTheDocument()
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
})
