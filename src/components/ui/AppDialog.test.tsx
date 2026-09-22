import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AppDialog } from './AppDialog'

describe('AppDialog', () => {
  it('opens with an accessible title and closes from its button', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()

    render(
      <AppDialog title="Novo produto" onClose={onClose}>
        <button type="button">Salvar</button>
      </AppDialog>,
    )

    expect(screen.getByRole('dialog', { name: 'Novo produto' })).toBeVisible()

    await user.click(screen.getByRole('button', { name: /fechar/i }))

    expect(onClose).toHaveBeenCalledOnce()
  })

  it('closes through the native cancel event', () => {
    const onClose = vi.fn()
    render(
      <AppDialog title="Editar categoria" onClose={onClose}>
        Conteúdo
      </AppDialog>,
    )

    fireEvent(
      screen.getByRole('dialog', { name: 'Editar categoria' }),
      new Event('cancel', { cancelable: true }),
    )

    expect(onClose).toHaveBeenCalledOnce()
  })
})
