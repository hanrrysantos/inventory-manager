import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { GoogleLoginButton } from './GoogleLoginButton'

const googleWindow = window as Window & { google?: unknown }

afterEach(() => {
  delete googleWindow.google
  document.getElementById('google-identity-services')?.remove()
})

describe('GoogleLoginButton', () => {
  it('forwards the credential received from Google after the official script loads', async () => {
    const onCredential = vi.fn()
    render(
      <GoogleLoginButton
        clientId="google-client-id"
        onCredential={onCredential}
      />,
    )

    const script = document.getElementById('google-identity-services') as HTMLScriptElement
    let credentialCallback: ((response: { credential: string }) => void) | undefined
    googleWindow.google = {
      accounts: {
        id: {
          initialize: ({ callback }: { callback: (response: { credential: string }) => void }) => {
            credentialCallback = callback
          },
          renderButton: (element: HTMLElement) => {
            const button = document.createElement('button')
            button.type = 'button'
            button.textContent = 'Continuar com Google'
            button.addEventListener('click', () => credentialCallback?.({ credential: 'google-id-token' }))
            element.replaceChildren(button)
          },
        },
      },
    }

    script.dispatchEvent(new Event('load'))

    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: 'Continuar com Google' }))

    expect(onCredential).toHaveBeenCalledWith('google-id-token')
  })

  it('explains the missing setup without loading the Google script', () => {
    render(<GoogleLoginButton clientId={undefined} onCredential={vi.fn()} />)

    expect(screen.getByRole('alert')).toHaveTextContent(/google não foi configurado/i)
    expect(document.getElementById('google-identity-services')).toBeNull()
  })
})
