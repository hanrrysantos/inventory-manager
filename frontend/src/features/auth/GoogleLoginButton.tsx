import { useEffect, useRef, useState } from 'react'

interface GoogleCredentialResponse {
  credential?: string
}

interface GoogleIdentityServices {
  accounts: {
    id: {
      initialize: (configuration: {
        client_id: string
        callback: (response: GoogleCredentialResponse) => void
      }) => void
      renderButton: (
        element: HTMLElement,
        options: {
          theme: 'filled_black'
          size: 'large'
          text: 'continue_with'
          shape: 'pill'
          logo_alignment: 'left'
          width: number
        },
      ) => void
    }
  }
}

declare global {
  interface Window {
    google?: GoogleIdentityServices
  }
}

interface GoogleLoginButtonProps {
  clientId: string | undefined
  onCredential: (credential: string) => void
}

const SCRIPT_ID = 'google-identity-services'
// Google Identity Services renders a fixed-width button (max 400px). We fit it
// to the container width once, at render time, so it matches the screen without
// ever re-rendering (re-rendering is what made the button flicker/fight).
const MAX_BUTTON_WIDTH = 400

export function GoogleLoginButton({ clientId, onCredential }: GoogleLoginButtonProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const onCredentialRef = useRef(onCredential)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    onCredentialRef.current = onCredential
  }, [onCredential])

  useEffect(() => {
    if (!clientId) {
      return
    }

    let active = true
    let hasRendered = false

    const renderGoogleButton = () => {
      const container = containerRef.current
      if (!active || hasRendered || !container || !window.google) return

      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: ({ credential }) => {
          if (credential) onCredentialRef.current(credential)
        },
      })

      const availableWidth =
        container.getBoundingClientRect().width || container.clientWidth || MAX_BUTTON_WIDTH
      const width = Math.min(Math.floor(availableWidth), MAX_BUTTON_WIDTH)

      // Render exactly once. Clearing first keeps it idempotent under React
      // StrictMode's double-invoked effects, so we never end up with two
      // buttons competing to appear.
      container.replaceChildren()
      window.google.accounts.id.renderButton(container, {
        theme: 'filled_black',
        size: 'large',
        text: 'continue_with',
        shape: 'pill',
        logo_alignment: 'left',
        width,
      })
      hasRendered = true
    }

    const handleScriptLoad = () => {
      if (!active) return
      if (!window.google) {
        setError('Não foi possível carregar o login com Google. Tente novamente.')
        return
      }
      renderGoogleButton()
    }

    const handleScriptError = () => {
      if (active) setError('Não foi possível carregar o login com Google. Tente novamente.')
    }

    if (window.google) {
      renderGoogleButton()
      return () => {
        active = false
      }
    }

    let script = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null
    if (!script) {
      script = document.createElement('script')
      script.id = SCRIPT_ID
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.defer = true
      document.head.append(script)
    }

    script.addEventListener('load', handleScriptLoad)
    script.addEventListener('error', handleScriptError)

    return () => {
      active = false
      script?.removeEventListener('load', handleScriptLoad)
      script?.removeEventListener('error', handleScriptError)
    }
  }, [clientId])

  const message = !clientId
    ? 'O login com Google não foi configurado neste ambiente.'
    : error

  if (message) return <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">{message}</p>

  return <div className="google-login-button" ref={containerRef} aria-label="Entrar com Google" />
}
