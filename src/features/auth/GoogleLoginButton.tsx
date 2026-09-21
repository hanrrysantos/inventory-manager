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
// Only re-render the Google button for meaningful width changes (e.g. an
// orientation change), not for the small transient fluctuations that occur
// while the page finishes loading.
const WIDTH_RERENDER_THRESHOLD = 16

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
    let resizeObserver: ResizeObserver | undefined
    let renderedWidth: number | undefined
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

      const renderGoogleButton = () => {
        const container = containerRef.current
        if (!active || !container || !window.google) return

        const availableWidth = container.getBoundingClientRect().width || container.clientWidth
        const width = Math.min(Math.floor(availableWidth || 384), 384)
        // Ignore tiny width fluctuations that happen while the page settles
        // (fonts, scrollbar, layout). Re-rendering for them empties the
        // container for a frame and makes the button visibly jump/shake.
        if (
          renderedWidth !== undefined
          && Math.abs(width - renderedWidth) < WIDTH_RERENDER_THRESHOLD
        ) {
          return
        }

        renderedWidth = width
        const options = {
          theme: 'filled_black',
          size: 'large',
          text: 'continue_with',
          shape: 'pill',
          logo_alignment: 'left',
          width,
        } as const

        // Render into a detached node first, then swap it in, so the button
        // never disappears for a frame during a re-render (which caused the
        // visible jump/shake).
        const staging = document.createElement('div')
        window.google.accounts.id.renderButton(staging, options)
        if (staging.childNodes.length > 0) {
          container.replaceChildren(...staging.childNodes)
          return
        }

        // Fallback: some environments only render into an attached node.
        container.replaceChildren()
        window.google.accounts.id.renderButton(container, options)
      }

      renderGoogleButton()
      if (typeof ResizeObserver !== 'undefined') {
        resizeObserver = new ResizeObserver(renderGoogleButton)
        resizeObserver.observe(containerRef.current)
      }
    }

    let script = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null
    if (window.google) {
      initialize()
      return () => {
        active = false
        resizeObserver?.disconnect()
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
      resizeObserver?.disconnect()
      script?.removeEventListener('load', initialize)
      script?.removeEventListener('error', handleScriptError)
    }
  }, [clientId])

  const message = !clientId
    ? 'O login com Google não foi configurado neste ambiente.'
    : error

  if (message) return <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">{message}</p>

  return <div className="google-login-button" ref={containerRef} aria-label="Entrar com Google" />
}
