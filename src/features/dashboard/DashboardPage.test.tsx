import { render, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from '../../app/App'
import { server } from '../../test/server'

describe('DashboardPage', () => {
  beforeEach(() => {
    localStorage.setItem('inventory-manager.token', 'valid-token')
    window.history.pushState({}, '', '/dashboard')
  })

  it('renders the four primary summary cards with formatted values', async () => {
    render(<App />)

    const indicators = await screen.findByRole('region', {
      name: /indicadores do estoque/i,
    })
    expect(await within(indicators).findByText('Total de itens')).toBeVisible()
    expect(within(indicators).getByText('170')).toBeVisible()
    expect(within(indicators).getByText('Estoque baixo')).toBeVisible()
    expect(within(indicators).getByText('Em falta')).toBeVisible()
    expect(within(indicators).getByText('Valor em estoque')).toBeVisible()
    expect(within(indicators).getByText(/R\$\s21\.480,00/)).toBeVisible()
  })

  it('shows product count as support text for total quantity', async () => {
    render(<App />)

    expect(await screen.findByText('em 3 produtos')).toBeVisible()
  })

  it('renders attention items with current and minimum quantities', async () => {
    render(<App />)

    await screen.findAllByText('Sabonete Natural Lavanda')
    const panel = screen.getByRole('region', {
      name: /produtos que precisam de atenção/i,
    })
    expect(within(panel).getByText('Sabonete Natural Lavanda')).toBeVisible()
    expect(within(panel).getByText('0/25')).toBeVisible()
    expect(within(panel).getByText('Mel Silvestre 500g')).toBeVisible()
    expect(within(panel).getByText('28/30')).toBeVisible()
  })

  it('renders a compact product preview without unsupported columns', async () => {
    render(<App />)

    const productRegion = await screen.findByRole('region', {
      name: /resumo de produtos/i,
    })
    expect(await within(productRegion).findByText('Chá Verde Orgânico')).toBeVisible()
    expect(within(productRegion).queryByText(/preço/i)).not.toBeInTheDocument()
    expect(within(productRegion).queryByText(/atualizado/i)).not.toBeInTheDocument()
  })

  it('keeps summary errors isolated from the product preview', async () => {
    server.use(
      http.get('*/api/v1/dashboard/summary', () =>
        HttpResponse.json({ message: 'Falha temporária' }, { status: 500 }),
      ),
    )
    render(<App />)

    expect(
      await screen.findByText(/não foi possível carregar o resumo/i, {}, { timeout: 3000 }),
    ).toBeVisible()
    expect(await screen.findByText('Chá Verde Orgânico')).toBeVisible()
  })
})
