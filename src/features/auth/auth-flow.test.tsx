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
    await user.type(screen.getByLabelText('Senha'), 'admin123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

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
    await user.type(screen.getByLabelText('Senha'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

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
    await user.type(screen.getByLabelText('Senha'), '123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

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

  it('shows and hides the login password accessibly', async () => {
    const user = userEvent.setup()
    render(<App />)
    const password = await screen.findByLabelText('Senha')
    expect(password).toHaveAttribute('type', 'password')
    await user.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    expect(password).toHaveAttribute('type', 'text')
    await user.click(screen.getByRole('button', { name: 'Ocultar senha' }))
    expect(password).toHaveAttribute('type', 'password')
  })

  it('provides 44px touch targets for password, inline mode actions, and the home link', async () => {
    const user = userEvent.setup()
    render(<App />)

    const backLink = await screen.findByRole('link', { name: 'Voltar ao início' })
    expect(backLink).toHaveClass('min-h-11', 'min-w-11')
    expect(backLink).toHaveAttribute('href', '/')
    expect(screen.getByRole('button', { name: 'Mostrar senha' })).toHaveClass('size-11')

    const createAccount = screen.getByRole('button', { name: 'Criar conta' })
    expect(createAccount).toHaveClass('min-h-11', 'min-w-11', 'text-[#107842]')
    expect(screen.getByRole('button', { name: 'Entrar' })).toHaveClass('bg-[#107842]')
    expect(screen.getByRole('tab', { name: 'Criar conta' })).toHaveClass('text-[#617168]')
    await user.click(createAccount)
    expect(screen.getByLabelText('Nome')).toBeVisible()
    expect(screen.getByRole('button', { name: 'Mostrar senha' })).toHaveClass('size-11')

    const returnToLogin = screen.getByRole('button', { name: 'Entrar' })
    expect(returnToLogin).toHaveClass('min-h-11', 'min-w-11', 'text-[#107842]')
    expect(screen.getByRole('button', { name: 'Criar minha conta' })).toHaveClass('bg-[#107842]')
    await user.click(returnToLogin)
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Entrar' })).toHaveAttribute('aria-selected', 'true')
  })

  it('shows and hides the registration password accessibly', async () => {
    const user = userEvent.setup()
    render(<App />)
    await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
    const password = screen.getByLabelText('Senha')
    expect(password).toHaveAttribute('type', 'password')
    await user.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    expect(password).toHaveAttribute('type', 'text')
    await user.click(screen.getByRole('button', { name: 'Ocultar senha' }))
    expect(password).toHaveAttribute('type', 'password')
  })

  it('explains that Google access is not available yet without sending requests', async () => {
    const requests: string[] = []
    const recordRequest = ({ request }: { request: Request }) => requests.push(request.url)
    server.events.on('request:start', recordRequest)
    try {
      const user = userEvent.setup()
      render(<App />)
      await user.click(await screen.findByRole('button', { name: /entrar com google/i }))
      expect(screen.getByRole('status')).toHaveTextContent(/google estará disponível em breve/i)
      expect(window.location.pathname).toBe('/login')
      expect(requests).toEqual([])
    } finally {
      server.events.removeListener('request:start', recordRequest)
    }
  })

  it('switches between login and registration in the same card', async () => {
    const user = userEvent.setup()
    render(<App />)
    await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
    expect(screen.getByLabelText('Nome')).toBeVisible()
    expect(screen.getByRole('button', { name: 'Criar minha conta' })).toBeVisible()
    expect(window.location.pathname).toBe('/login')
    await user.click(screen.getByRole('tab', { name: 'Entrar' }))
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
  })

  it('supports roving focus and arrow, Home, and End keys across access tabs', async () => {
    const user = userEvent.setup()
    render(<App />)
    const loginTab = await screen.findByRole('tab', { name: 'Entrar' })
    const registerTab = screen.getByRole('tab', { name: 'Criar conta' })

    expect(loginTab).toHaveAttribute('tabindex', '0')
    expect(registerTab).toHaveAttribute('tabindex', '-1')

    loginTab.focus()
    await user.keyboard('{ArrowRight}')
    expect(registerTab).toHaveFocus()
    expect(registerTab).toHaveAttribute('aria-selected', 'true')
    expect(registerTab).toHaveAttribute('tabindex', '0')
    expect(loginTab).toHaveAttribute('tabindex', '-1')

    await user.keyboard('{ArrowLeft}')
    expect(loginTab).toHaveFocus()
    expect(loginTab).toHaveAttribute('aria-selected', 'true')

    await user.keyboard('{End}')
    expect(registerTab).toHaveFocus()
    expect(registerTab).toHaveAttribute('aria-selected', 'true')

    await user.keyboard('{Home}')
    expect(loginTab).toHaveFocus()
    expect(loginTab).toHaveAttribute('aria-selected', 'true')
  })

  it('clears the registration password and stale feedback after changing modes', async () => {
    const user = userEvent.setup()
    render(<App />)
    await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
    await user.type(screen.getByLabelText('Senha'), '123')
    await user.click(screen.getByRole('button', { name: 'Criar minha conta' }))
    expect(
      await screen.findByText('A senha deve ter pelo menos 6 caracteres'),
    ).toBeVisible()

    await user.click(screen.getByRole('tab', { name: 'Entrar' }))
    await user.click(screen.getByRole('tab', { name: 'Criar conta' }))

    expect(screen.getByLabelText('Senha')).toHaveValue('')
    expect(
      screen.queryByText('A senha deve ter pelo menos 6 caracteres'),
    ).not.toBeInTheDocument()
  })

  it('validates registration without sending invalid data', async () => {
    let requestCount = 0
    server.use(http.post('*/api/v1/auth/register', () => {
      requestCount += 1
      return HttpResponse.json({}, { status: 201 })
    }))
    const user = userEvent.setup()
    render(<App />)
    await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
    await user.type(screen.getByLabelText('E-mail'), 'invalid')
    await user.type(screen.getByLabelText('Senha'), '123')
    await user.click(screen.getByRole('button', { name: 'Criar minha conta' }))
    expect(await screen.findByText('Informe seu nome')).toBeVisible()
    expect(screen.getByText('Informe um e-mail válido')).toBeVisible()
    expect(screen.getByText('A senha deve ter pelo menos 6 caracteres')).toBeVisible()
    expect(requestCount).toBe(0)
  })

  it('registers an account and returns to login with its email without creating a session', async () => {
    let registrationBody: unknown
    server.use(http.post('*/api/v1/auth/register', async ({ request }) => {
      registrationBody = await request.json()
      return new HttpResponse(null, { status: 201 })
    }))
    const user = userEvent.setup()
    render(<App />)
    await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
    await user.type(screen.getByLabelText('Nome'), 'Maria Silva')
    await user.type(screen.getByLabelText('E-mail'), 'maria@example.com')
    await user.type(screen.getByLabelText('Senha'), '123456')
    await user.click(screen.getByRole('button', { name: 'Criar minha conta' }))
    expect(await screen.findByText(/conta criada com sucesso/i)).toBeVisible()
    expect(screen.getByRole('tab', { name: 'Entrar' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByLabelText('E-mail')).toHaveValue('maria@example.com')
    expect(screen.getByLabelText('Senha')).toHaveValue('')
    expect(registrationBody).toEqual({ name: 'Maria Silva', email: 'maria@example.com', password: '123456' })
    expect(localStorage.getItem('inventory-manager.token')).toBeNull()
    expect(window.location.pathname).toBe('/login')
  })

  it('shows the API message when registration fails', async () => {
    server.use(http.post('*/api/v1/auth/register', () => HttpResponse.json({ message: 'E-mail já cadastrado' }, { status: 409 })))
    const user = userEvent.setup()
    render(<App />)
    await user.click(await screen.findByRole('tab', { name: 'Criar conta' }))
    await user.type(screen.getByLabelText('Nome'), 'Maria Silva')
    await user.type(screen.getByLabelText('E-mail'), 'maria@example.com')
    await user.type(screen.getByLabelText('Senha'), '123456')
    await user.click(screen.getByRole('button', { name: 'Criar minha conta' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail já cadastrado')
  })

  it('redirects an authenticated visitor from login to the dashboard', async () => {
    localStorage.setItem('inventory-manager.token', 'valid-token')
    render(<App />)
    expect(await screen.findByRole('heading', { name: /painel de estoque/i })).toBeVisible()
    expect(window.location.pathname).toBe('/dashboard')
  })
})
