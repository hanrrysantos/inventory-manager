import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from '../../app/App'

describe('authenticated application shell', () => {
  beforeEach(() => {
    localStorage.setItem('inventory-manager.token', 'valid-token')
    window.history.pushState({}, '', '/dashboard')
  })

  it('shows supported navigation and omits unsupported entries', async () => {
    render(<App />)

    const navigation = await screen.findByRole('navigation', {
      name: /navegação principal/i,
    })
    expect(within(navigation).getByRole('link', { name: /painel/i })).toBeVisible()
    expect(within(navigation).getByRole('link', { name: /produtos/i })).toBeVisible()
    expect(screen.queryByText(/fornecedores/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/relatórios/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/configurações/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/movimentações/i)).not.toBeInTheDocument()
  })

  it('shows the current user and formatted role', async () => {
    render(<App />)

    expect(await screen.findByText('Ana Souza')).toBeVisible()
    expect(screen.getByText('Administradora')).toBeVisible()
  })

  it('logs out through the header action', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: /sair/i }))

    expect(
      await screen.findByRole('heading', { name: /acesse sua conta/i }),
    ).toBeVisible()
    expect(localStorage.getItem('inventory-manager.token')).toBeNull()
  })

  it('opens and closes mobile navigation with accessible controls', async () => {
    const user = userEvent.setup()
    render(<App />)

    const openButton = await screen.findByRole('button', { name: /abrir menu/i })
    expect(openButton).toHaveAttribute('aria-expanded', 'false')
    await user.click(openButton)

    expect(openButton).toHaveAttribute('aria-expanded', 'true')
    const drawer = screen.getByRole('dialog', { name: /menu de navegação/i })
    await user.click(within(drawer).getByRole('button', { name: /fechar menu/i }))
    expect(screen.queryByRole('dialog', { name: /menu de navegação/i })).not.toBeInTheDocument()
  })

  it('moves focus into the mobile drawer and closes it with Escape', async () => {
    const user = userEvent.setup()
    render(<App />)

    const openButton = await screen.findByRole('button', { name: /abrir menu/i })
    await user.click(openButton)

    const drawer = screen.getByRole('dialog', { name: /menu de navegação/i })
    expect(within(drawer).getByRole('button', { name: /fechar menu/i })).toHaveFocus()

    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog', { name: /menu de navegação/i })).not.toBeInTheDocument()
    expect(openButton).toHaveFocus()
  })

  it('keeps Tab navigation inside the open mobile drawer', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: /abrir menu/i }))
    const drawer = screen.getByRole('dialog', { name: /menu de navegação/i })
    const closeButton = within(drawer).getByRole('button', { name: /fechar menu/i })
    const links = within(drawer).getAllByRole('link')

    links.at(-1)?.focus()
    await user.tab()
    expect(closeButton).toHaveFocus()

    await user.tab({ shift: true })
    expect(links.at(-1)).toHaveFocus()
  })
})
