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
        options: { theme: 'outline'; size: 'large'; text: 'signin_with'; width: number },
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
    const handleScriptError = () => {
      if (active) setError('Não foi possível carregar o login com Google. Tente novamente.')
    }
    const initialize = () => {
      if (!active || !containerRef.current || !window.google) {
        if (active) setError('Não foi possível carregar o login com Google. Tente novamente.')
        return
      }

      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: ({ credential }) => {
          if (credential) onCredentialRef.current(credential)
        },
      })
      window.google.accounts.id.renderButton(containerRef.current, {
        theme: 'outline',
        size: 'large',
        text: 'signin_with',
        width: 384,
      })
    }

    let script = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null
    if (window.google) {
      initialize()
      return () => {
        active = false
      }
    }

    if (!script) {
      script = document.createElement('script')
      script.id = SCRIPT_ID
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.defer = true
      document.head.append(script)
    }

    script.addEventListener('load', initialize)
    script.addEventListener('error', handleScriptError)

    return () => {
      active = false
      script?.removeEventListener('load', initialize)
      script?.removeEventListener('error', handleScriptError)
    }
  }, [clientId])

  const message = !clientId
    ? 'O login com Google não foi configurado neste ambiente.'
    : error

  if (message) return <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">{message}</p>

  return <div ref={containerRef} aria-label="Entrar com Google" />
}
