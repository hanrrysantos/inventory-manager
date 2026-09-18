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

  it('searches by product name case-insensitively', async () => {
    const user = userEvent.setup()
    render(<App />)

    await screen.findByText('Chá Verde Orgânico')
    await user.type(screen.getByLabelText(/buscar produto ou sku/i), 'mel silvestre')

    expect(screen.getByText('Mel Silvestre 500g')).toBeVisible()
    expect(screen.queryByText('Chá Verde Orgânico')).not.toBeInTheDocument()
  })

  it('searches by SKU case-insensitively', async () => {
    const user = userEvent.setup()
    render(<App />)

    await screen.findByText('Chá Verde Orgânico')
    await user.type(screen.getByLabelText(/buscar produto ou sku/i), 'sab-207')

    expect(screen.getByText('Sabonete Natural Lavanda')).toBeVisible()
    expect(screen.queryByText('Mel Silvestre 500g')).not.toBeInTheDocument()
  })

  it('filters products by derived stock status', async () => {
    const user = userEvent.setup()
    render(<App />)

    await screen.findByText('Chá Verde Orgânico')
    await user.click(screen.getByRole('button', { name: /^estoque baixo$/i }))

    expect(screen.getByText('Mel Silvestre 500g')).toBeVisible()
    expect(screen.queryByText('Chá Verde Orgânico')).not.toBeInTheDocument()
    expect(screen.queryByText('Sabonete Natural Lavanda')).not.toBeInTheDocument()
    expect(window.location.search).toContain('status=LOW_STOCK')
  })

  it('distinguishes an empty catalog from no matching results', async () => {
    server.use(
      http.get('*/api/v1/products', () => HttpResponse.json([])),
    )
    const { unmount } = render(<App />)

    expect(await screen.findByText(/nenhum produto cadastrado/i)).toBeVisible()

    unmount()
    server.use(
      http.get('*/api/v1/products', () => HttpResponse.json(productFixtures)),
    )
    window.history.pushState({}, '', '/products?q=inexistente')
    render(<App />)

    expect(await screen.findByText(/nenhum produto encontrado/i)).toBeVisible()
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
      http.get('*/api/v1/products', () => HttpResponse.json(productFixtures)),
    )
    await user.click(screen.getByRole('button', { name: /tentar novamente/i }))
    expect(await screen.findByText('Chá Verde Orgânico')).toBeVisible()
  })
})
