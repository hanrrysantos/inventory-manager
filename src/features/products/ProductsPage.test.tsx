import { render, screen } from '@testing-library/react'
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
})
