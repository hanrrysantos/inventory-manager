import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
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

  it('keeps a busy dialog open', () => {
    const onClose = vi.fn()
    render(
      <AppDialog title="Excluindo produto" onClose={onClose} busy>
        {(close) => <button onClick={close}>Cancelar</button>}
      </AppDialog>,
    )

    const dialog = screen.getByRole('dialog')
    fireEvent(dialog, new Event('cancel', { cancelable: true }))
    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(dialog).toHaveAttribute('open')
    expect(screen.getByRole('button', { name: /fechar/i })).toBeDisabled()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('closes natively and restores focus to the trigger', async () => {
    const user = userEvent.setup()

    function Harness() {
      const [open, setOpen] = useState(false)
      return (
        <>
          <button type="button" onClick={() => setOpen(true)}>Abrir</button>
          {open && (
            <AppDialog title="Novo produto" onClose={() => setOpen(false)}>
              {(close) => <button onClick={close}>Cancelar</button>}
            </AppDialog>
          )}
        </>
      )
    }

    render(<Harness />)
    const trigger = screen.getByRole('button', { name: /abrir/i })
    await user.click(trigger)
    await user.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })
})
