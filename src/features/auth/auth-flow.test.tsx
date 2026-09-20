import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from '../../app/App'
import { server } from '../../test/server'

describe('authentication flow', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/login')
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.unstubAllGlobals()
    delete (window as Window & { google?: unknown }).google
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

  it('returns to login instead of crashing when session data is invalid', async () => {
    server.use(
      http.get('*/api/v1/users/me', () =>
        HttpResponse.text('<!doctype html><html></html>', {
          headers: { 'Content-Type': 'text/html' },
        }),
      ),
    )
    localStorage.setItem('inventory-manager.token', 'invalid-response-token')
    window.history.pushState({}, '', '/dashboard')

    render(<App />)

    expect(screen.getByText(/carregando sessão/i)).toBeInTheDocument()
    expect(
      await screen.findByRole('heading', { name: /acesse sua conta/i }),
    ).toBeVisible()
    expect(window.location.pathname).toBe('/login')
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

  it('uses only the access tabs to switch modes and shows a pointer cursor', async () => {
    const user = userEvent.setup()
    render(<App />)

    const backLink = await screen.findByRole('link', { name: 'Voltar ao início' })
    expect(backLink).toHaveClass('min-h-11', 'min-w-11')
    expect(backLink).toHaveAttribute('href', '/')
    expect(screen.getByRole('button', { name: 'Mostrar senha' })).toHaveClass('size-11')

    const loginTab = screen.getByRole('tab', { name: 'Entrar' })
    const createAccount = screen.getByRole('tab', { name: 'Criar conta' })
    expect(loginTab).toHaveClass('cursor-pointer')
    expect(createAccount).toHaveClass('cursor-pointer')
    expect(screen.queryByRole('button', { name: 'Criar conta' })).not.toBeInTheDocument()
    expect(screen.queryByText(/ainda não tem uma conta/i)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrar' })).toHaveClass('bg-[#107842]')
    await user.click(createAccount)
    expect(screen.getByLabelText('Nome')).toBeVisible()
    expect(screen.getByRole('button', { name: 'Mostrar senha' })).toHaveClass('size-11')

    const returnToLogin = screen.getByRole('tab', { name: 'Entrar' })
    expect(returnToLogin).toHaveClass('cursor-pointer')
    expect(screen.queryByRole('button', { name: 'Entrar' })).not.toBeInTheDocument()
    expect(screen.queryByText(/já tem uma conta/i)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Criar minha conta' })).toHaveClass('bg-[#107842]')
    await user.click(returnToLogin)
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Entrar' })).toHaveAttribute('aria-selected', 'true')
  })

  it('keeps the card top anchored while animating the access panel height', async () => {
    let resizeCallback: (() => void) | undefined
    class TestResizeObserver {
      constructor(callback: () => void) {
        resizeCallback = callback
      }

      observe() {}
      disconnect() {}
    }
    vi.stubGlobal('ResizeObserver', TestResizeObserver)

    const user = userEvent.setup()
    render(<App />)

    const main = await screen.findByRole('main')
    expect(main).toHaveClass('items-start', 'justify-items-center')

    const animatedPanel = screen.getByTestId('access-panel-height')
    const panelContent = animatedPanel.firstElementChild as HTMLElement
    expect(getComputedStyle(panelContent).display).toBe('flow-root')
    Object.defineProperty(panelContent, 'scrollHeight', {
      configurable: true,
      value: 420,
    })
    act(() => resizeCallback?.())
    expect(animatedPanel).toHaveStyle({ height: '420px' })

    Object.defineProperty(panelContent, 'scrollHeight', {
      configurable: true,
      value: 560,
    })
    await user.click(screen.getByRole('tab', { name: 'Criar conta' }))
    act(() => resizeCallback?.())
    expect(animatedPanel).toHaveStyle({ height: '560px' })
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

  it('explains that Google login is not configured without sending requests', async () => {
    const requests: string[] = []
    const recordRequest = ({ request }: { request: Request }) => requests.push(request.url)
    server.events.on('request:start', recordRequest)
    try {
      render(<App />)

      expect(await screen.findByRole('alert')).toHaveTextContent(/google não foi configurado/i)
      expect(window.location.pathname).toBe('/login')
      expect(requests).toEqual([])
    } finally {
      server.events.removeListener('request:start', recordRequest)
    }
  })

  it('keeps a credential out of a Google login error and allows a retry', async () => {
    let submittedBody: unknown
    let googleAttempts = 0
    server.use(
      http.post('*/api/v1/auth/google', async ({ request }) => {
        submittedBody = await request.json()
        googleAttempts += 1
        if (googleAttempts === 1) {
          return HttpResponse.json(
            { message: 'Credential google-id-token could not be verified' },
            { status: 401 },
          )
        }
        return HttpResponse.json({ token: 'valid-token' })
      }),
    )
    vi.stubEnv('VITE_GOOGLE_CLIENT_ID', 'google-client-id')
    let credentialCallback: ((response: { credential: string }) => void) | undefined
    ;(window as Window & { google?: unknown }).google = {
      accounts: {
        id: {
          initialize: ({ callback }: { callback: (response: { credential: string }) => void }) => {
            credentialCallback = callback
          },
          renderButton: (element: HTMLElement) => {
            const button = document.createElement('button')
            button.type = 'button'
            button.textContent = 'Entrar com Google'
            button.addEventListener('click', () => credentialCallback?.({ credential: 'google-id-token' }))
            element.replaceChildren(button)
          },
        },
      },
    }

    const user = userEvent.setup()
    render(<App />)
    await user.click(await screen.findByRole('button', { name: 'Entrar com Google' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/não foi possível entrar com google/i)
    expect(screen.queryByText(/google-id-token/i)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Entrar com Google' }))

    expect(
      await screen.findByRole('heading', { name: /painel de estoque/i }),
    ).toBeInTheDocument()
    expect(submittedBody).toEqual({ idToken: 'google-id-token' })
    expect(localStorage.getItem('inventory-manager.token')).toBe('valid-token')
  })

  it('requires local authentication before linking an existing account to Google', async () => {
    let linkBody: unknown
    let linkAuthorization: string | null = null
    let linkAttempts = 0
    server.use(
      http.post('*/api/v1/auth/google', () =>
        HttpResponse.json(
          {
            status: 409,
            error: 'GoogleAccountLinkRequired',
            message: 'Google account must be linked from an authenticated session',
            path: '/api/v1/auth/google',
          },
          { status: 409 },
        ),
      ),
      http.post('*/api/v1/auth/google/link', async ({ request }) => {
        linkBody = await request.json()
        linkAuthorization = request.headers.get('Authorization')
        linkAttempts += 1
        if (linkAttempts === 1) {
          return HttpResponse.json(
            { message: 'Credential google-id-token could not be verified' },
            { status: 500 },
          )
        }
        return new HttpResponse(null, { status: 204 })
      }),
    )
    vi.stubEnv('VITE_GOOGLE_CLIENT_ID', 'google-client-id')
    let credentialCallback: ((response: { credential: string }) => void) | undefined
    ;(window as Window & { google?: unknown }).google = {
      accounts: {
        id: {
          initialize: ({ callback }: { callback: (response: { credential: string }) => void }) => {
            credentialCallback = callback
          },
          renderButton: (element: HTMLElement) => {
            const button = document.createElement('button')
            button.type = 'button'
            button.textContent = 'Entrar com Google'
            button.addEventListener('click', () => credentialCallback?.({ credential: 'google-id-token' }))
            element.replaceChildren(button)
          },
        },
      },
    }

    const user = userEvent.setup()
    render(<App />)
    await user.click(await screen.findByRole('button', { name: 'Entrar com Google' }))
    expect(await screen.findByText(/entre com e-mail e senha para vincular/i)).toBeVisible()

    await user.type(screen.getByLabelText('E-mail'), 'admin@email.com')
    await user.type(screen.getByLabelText('Senha'), 'admin123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(await screen.findByRole('status')).toHaveTextContent(/entre novamente com google/i)

    await user.click(screen.getByRole('button', { name: 'Entrar com Google' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/não foi possível vincular sua conta google/i)
    expect(screen.queryByText(/google-id-token/i)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Entrar com Google' }))
    expect(
      await screen.findByRole('heading', { name: /painel de estoque/i }),
    ).toBeInTheDocument()
    expect(linkBody).toEqual({ idToken: 'google-id-token' })
    expect(linkAuthorization).toBe('Bearer valid-token')
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
