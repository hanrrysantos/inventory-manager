import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { GoogleLoginButton } from './GoogleLoginButton'

const googleWindow = window as Window & { google?: unknown }

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
  delete googleWindow.google
  document.getElementById('google-identity-services')?.remove()
})

describe('GoogleLoginButton', () => {
  it('asks Google to render a dark, pill-shaped continue button', () => {
    let buttonOptions: unknown
    googleWindow.google = {
      accounts: {
        id: {
          initialize: vi.fn(),
          renderButton: (_element: HTMLElement, options: unknown) => {
            buttonOptions = options
          },
        },
      },
    }

    render(
      <GoogleLoginButton
        clientId="google-client-id"
        onCredential={vi.fn()}
      />,
    )

    expect(buttonOptions).toEqual({
      theme: 'filled_black',
      size: 'large',
      text: 'continue_with',
      shape: 'pill',
      logo_alignment: 'left',
      width: 384,
    })
    expect(screen.getByLabelText('Entrar com Google')).toHaveClass(
      'google-login-button',
    )
  })

  it('re-renders the Google button when its available width changes', () => {
    let availableWidth = 320
    vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockImplementation(
      () => availableWidth,
    )
    let resizeCallback: (() => void) | undefined
    class TestResizeObserver {
      constructor(callback: () => void) {
        resizeCallback = callback
      }

      observe() {}
      disconnect() {}
    }
    vi.stubGlobal('ResizeObserver', TestResizeObserver)
    const renderButton = vi.fn((element: HTMLElement) => {
      element.replaceChildren(document.createElement('span'))
    })
    googleWindow.google = {
      accounts: {
        id: {
          initialize: vi.fn(),
          renderButton,
        },
      },
    }

    render(
      <GoogleLoginButton
        clientId="google-client-id"
        onCredential={vi.fn()}
      />,
    )

    expect(renderButton).toHaveBeenLastCalledWith(
      expect.any(HTMLElement),
      expect.objectContaining({ width: 320 }),
    )

    availableWidth = 280
    act(() => resizeCallback?.())

    expect(renderButton).toHaveBeenCalledTimes(2)
    expect(renderButton).toHaveBeenLastCalledWith(
      expect.any(HTMLElement),
      expect.objectContaining({ width: 280 }),
    )
  })

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
