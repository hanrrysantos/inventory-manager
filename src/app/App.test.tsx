import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { expect, it } from 'vitest'
import { App } from './App'

it('supports login, dashboard, product catalog, and logout as one flow', async () => {
  window.history.pushState({}, '', '/')
  const user = userEvent.setup()
  render(<App />)

  await user.click(
    await screen.findByRole('link', { name: 'Acesse a plataforma' }),
  )
  expect(window.location.pathname).toBe('/login')

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
  expect(
    await screen.findByRole('heading', { level: 1, name: 'Produtos' }),
  ).toBeVisible()
  expect(await screen.findByText('Chá Verde Orgânico')).toBeVisible()
  expect(screen.getByText('3 produtos no catálogo')).toBeVisible()

  await user.click(screen.getByRole('button', { name: /sair/i }))
  expect(
    await screen.findByRole('heading', { name: /acesse sua conta/i }),
  ).toBeVisible()
  expect(localStorage.getItem('inventory-manager.token')).toBeNull()
})
