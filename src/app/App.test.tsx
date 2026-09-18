import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it } from 'vitest'
import { App } from './App'

it('supports login, dashboard, products, search, and logout as one flow', async () => {
  window.history.pushState({}, '', '/dashboard')
  const user = userEvent.setup()
  render(<App />)

  expect(
    await screen.findByRole('heading', { name: /acesse sua conta/i }),
  ).toBeVisible()

  await user.type(screen.getByLabelText(/e-mail/i), 'admin@email.com')
  await user.type(screen.getByLabelText('Senha'), 'admin123')
  await user.click(screen.getByRole('button', { name: 'Entrar' }))

  expect(
    await screen.findByRole('heading', { name: /painel de estoque/i }),
  ).toBeVisible()
  expect(await screen.findByText('Total de itens')).toBeVisible()

  await user.click(screen.getByRole('link', { name: /produtos/i }))
  const searchInput = await screen.findByLabelText(/buscar produto ou sku/i)
  expect(screen.getByText('Chá Verde Orgânico')).toBeVisible()

  await user.type(searchInput, 'MEL-014')
  expect(screen.getByText('Mel Silvestre 500g')).toBeVisible()
  expect(screen.queryByText('Chá Verde Orgânico')).not.toBeInTheDocument()

  await user.click(screen.getByRole('button', { name: /sair/i }))
  expect(
    await screen.findByRole('heading', { name: /acesse sua conta/i }),
  ).toBeVisible()
  expect(localStorage.getItem('inventory-manager.token')).toBeNull()
})
