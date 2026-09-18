import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from '../../app/App'

describe('EstoqueHub landing page', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/')
  })

  it('presents the product with problem and feature sections', async () => {
    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: /controle seu estoque sem perder tempo/i,
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

  it('sends an unknown anonymous route back to the landing page', async () => {
    window.history.pushState({}, '', '/nao-existe')
    render(<App />)

    expect(
      await screen.findByRole('heading', {
        name: /controle seu estoque sem perder tempo/i,
      }),
    ).toBeVisible()
    expect(window.location.pathname).toBe('/')
  })
})
