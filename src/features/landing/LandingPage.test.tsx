import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from '../../app/App'
import { server } from '../../test/server'

describe('EstoqueHub landing page', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/')
  })

  it('presents the product with problem and feature sections', async () => {
    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: /seu estoque no ritmo do seu negócio/i,
      }),
    ).toBeVisible()
    expect(
      screen.getByRole('heading', {
        name: /o estoque não pode depender de adivinhação/i,
      }),
    ).toBeVisible()
    expect(
      screen.getByRole('heading', {
        name: /o essencial para decidir com clareza/i,
      }),
    ).toBeVisible()
    expect(document.querySelector('#problema')).toBeInTheDocument()
    expect(document.querySelector('#funcionalidades')).toBeInTheDocument()
    expect(screen.queryByText(/planos/i)).not.toBeInTheDocument()
  })

  it('has one access call to action and opens the existing login route', async () => {
    const user = userEvent.setup()
    render(<App />)

    const header = await screen.findByRole('banner')
    const accessLinks = screen.getAllByRole('link', {
      name: 'Acesse a plataforma',
    })
    expect(accessLinks).toHaveLength(1)
    expect(
      within(header).getByRole('link', { name: 'Acesse a plataforma' }),
    ).toBe(accessLinks[0])

    await user.click(accessLinks[0])

    expect(window.location.pathname).toBe('/login')
    expect(
      await screen.findByRole('heading', { name: /acesse sua conta/i }),
    ).toBeVisible()
  })

  it('provides a 44px minimum touch target for every landing link', async () => {
    render(<App />)

    await screen.findByRole('heading', {
      name: /seu estoque no ritmo do seu negócio/i,
    })

    for (const link of screen.getAllByRole('link')) {
      expect(link).toHaveClass('min-h-11')
    }
  })

  it('scrolls smoothly to a landing section without teleporting', async () => {
    const user = userEvent.setup()
    const scrollIntoView = vi.fn()
    const originalScrollIntoView = Element.prototype.scrollIntoView
    Element.prototype.scrollIntoView = scrollIntoView

    try {
      render(<App />)

      const header = await screen.findByRole('banner')
      await user.click(within(header).getByRole('link', { name: 'Problema' }))

      expect(scrollIntoView).toHaveBeenCalledWith({
        behavior: 'smooth',
        block: 'start',
      })
      expect(window.location.hash).toBe('#problema')
      expect(document.querySelector('#problema')).toHaveFocus()
    } finally {
      if (originalScrollIntoView) {
        Element.prototype.scrollIntoView = originalScrollIntoView
      } else {
        delete (Element.prototype as Partial<Element>).scrollIntoView
      }
    }
  })

  it('presents a distinctive inventory workflow in the hero preview', async () => {
    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: /seu estoque no ritmo do seu negócio/i,
      }),
    ).toBeVisible()
    expect(screen.getByText('Café especial 500g')).toBeInTheDocument()
    expect(screen.getByText('Repor hoje')).toBeInTheDocument()
    expect(screen.getByText('Tudo sob controle')).toBeInTheDocument()
    expect(screen.queryByText(/movimentos/i)).not.toBeInTheDocument()
  })

  it('sends an unknown anonymous route back to the landing page', async () => {
    window.history.pushState({}, '', '/nao-existe')
    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: /seu estoque no ritmo do seu negócio/i,
      }),
    ).toBeVisible()
    expect(window.location.pathname).toBe('/')
  })

  it('sends an unknown route to the dashboard after restoring a stored session', async () => {
    localStorage.setItem('inventory-manager.token', 'valid-token')
    window.history.pushState({}, '', '/nao-existe')
    render(<App />)

    expect(screen.getByText(/carregando sessão/i)).toBeInTheDocument()
    expect(
      await screen.findByRole('heading', { name: /painel de estoque/i }),
    ).toBeVisible()
    expect(window.location.pathname).toBe('/dashboard')
  })

  it('renders the public landing page while a stored session is still restoring', async () => {
    localStorage.setItem('inventory-manager.token', 'valid-token')
    let releaseSessionRequest: () => void = () => undefined
    const sessionRequestHeld = new Promise<void>((resolve) => {
      releaseSessionRequest = resolve
    })
    let markSessionRequestStarted: () => void = () => undefined
    const sessionRequestStarted = new Promise<void>((resolve) => {
      markSessionRequestStarted = resolve
    })
    server.use(
      http.get('*/api/v1/users/me', async () => {
        markSessionRequestStarted()
        await sessionRequestHeld
        return HttpResponse.json({
          id: 1,
          name: 'Ana Souza',
          email: 'ana@email.com',
          role: 'ADMIN',
          createdAt: '2026-09-17T12:00:00',
        })
      }),
    )

    render(<App />)
    await sessionRequestStarted

    expect(
      await screen.findByRole('heading', {
        name: /seu estoque no ritmo do seu negócio/i,
      }),
    ).toBeVisible()
    expect(screen.queryByText(/carregando sessão/i)).not.toBeInTheDocument()

    releaseSessionRequest()
  })
})
