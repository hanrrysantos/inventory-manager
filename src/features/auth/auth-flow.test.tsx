import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from '../../app/App'
import { server } from '../../test/server'

describe('authentication flow', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/login')
  })

  it('submits valid credentials and navigates to the dashboard', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.type(await screen.findByLabelText(/e-mail/i), 'admin@email.com')
    await user.type(screen.getByLabelText(/senha/i), 'admin123')
    await user.click(screen.getByRole('button', { name: /entrar/i }))

    expect(
      await screen.findByRole('heading', { name: /painel de estoque/i }),
    ).toBeInTheDocument()
    expect(screen.getByText('Ana Souza')).toBeInTheDocument()
    expect(localStorage.getItem('inventory-manager.token')).toBe('valid-token')
  })

  it('shows the API message for invalid credentials', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.type(await screen.findByLabelText(/e-mail/i), 'admin@email.com')
    await user.type(screen.getByLabelText(/senha/i), 'wrong-password')
    await user.click(screen.getByRole('button', { name: /entrar/i }))

    expect(await screen.findByText('E-mail ou senha inválidos')).toBeVisible()
    expect(window.location.pathname).toBe('/login')
  })

  it('warns that the first access can take longer while the API starts', async () => {
    render(<App />)

    expect(
      await screen.findByText(/primeiro acesso pode levar até um minuto/i),
    ).toBeVisible()
  })

  it('does not submit an invalid email or short password', async () => {
    let requestCount = 0
    server.use(
      http.post('*/api/v1/auth/login', () => {
        requestCount += 1
        return HttpResponse.json({ token: 'unexpected' })
      }),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.type(await screen.findByLabelText(/e-mail/i), 'invalid-email')
    await user.type(screen.getByLabelText(/senha/i), '123')
    await user.click(screen.getByRole('button', { name: /entrar/i }))

    expect(await screen.findByText(/informe um e-mail válido/i)).toBeVisible()
    expect(screen.getByText(/senha deve ter pelo menos 6 caracteres/i)).toBeVisible()
    expect(requestCount).toBe(0)
  })

  it('redirects an anonymous visitor to login', async () => {
    window.history.pushState({}, '', '/dashboard')
    render(<App />)

    expect(
      await screen.findByRole('heading', { name: /acesse sua conta/i }),
    ).toBeInTheDocument()
    expect(window.location.pathname).toBe('/login')
  })

  it('restores a stored session through users/me', async () => {
    localStorage.setItem('inventory-manager.token', 'valid-token')
    window.history.pushState({}, '', '/dashboard')
    render(<App />)

    expect(screen.getByText(/carregando sessão/i)).toBeInTheDocument()
    expect(
      await screen.findByRole('heading', { name: /painel de estoque/i }),
    ).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('Ana Souza')).toBeVisible())
  })
})
