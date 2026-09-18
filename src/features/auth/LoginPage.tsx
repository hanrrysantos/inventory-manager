import { ArrowLeft } from 'lucide-react'
import { useState, type KeyboardEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { BrandMark } from '../../components/ui/BrandMark'
import { LoginForm } from './LoginForm'
import { RegisterForm } from './RegisterForm'
import { useAuth } from './use-auth'

type AccessMode = 'login' | 'register'

export function LoginPage() {
  const { user } = useAuth()
  const [mode, setMode] = useState<AccessMode>('login')
  const [initialEmail, setInitialEmail] = useState('')
  const [notice, setNotice] = useState<string | null>(null)

  if (user) return <Navigate to="/dashboard" replace />

  function selectMode(nextMode: AccessMode) {
    setNotice(null)
    setMode(nextMode)
  }

  function handleRegistered(email: string) {
    setInitialEmail(email)
    setMode('login')
    setNotice('Conta criada com sucesso. Entre para continuar.')
  }

  function handleTabKeyDown(
    event: KeyboardEvent<HTMLButtonElement>,
    currentMode: AccessMode,
  ) {
    let nextMode: AccessMode | null = null

    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      nextMode = currentMode === 'login' ? 'register' : 'login'
    } else if (event.key === 'Home') {
      nextMode = 'login'
    } else if (event.key === 'End') {
      nextMode = 'register'
    }

    if (!nextMode) return

    event.preventDefault()
    selectMode(nextMode)
    document.getElementById(`${nextMode}-tab`)?.focus()
  }

  return (
    <main className="grid min-h-screen place-items-center bg-[#f4fbf6] bg-[linear-gradient(rgba(34,120,65,0.055)_1px,transparent_1px),linear-gradient(90deg,rgba(34,120,65,0.055)_1px,transparent_1px)] bg-[size:48px_48px] px-5 py-10 text-[#18281e]">
      <div className="w-full max-w-md">
        <Link className="mb-6 inline-flex min-h-11 min-w-11 items-center gap-2 text-sm font-medium text-[#52655a] hover:text-[#107842]" to="/">
          <ArrowLeft className="size-4" aria-hidden="true" />Voltar ao início
        </Link>
        <section className="rounded-[28px] border border-[#dce8df] bg-white p-6 shadow-[0_18px_50px_rgba(39,79,52,0.10)] sm:p-8">
          <div className="mb-8"><BrandMark /></div>
          <h1 className="text-2xl font-semibold">{mode === 'login' ? 'Acesse sua conta' : 'Crie sua conta'}</h1>
          <p className="mt-2 text-sm text-[#617168]">
            {mode === 'login' ? 'Entre para acompanhar o estoque da sua empresa.' : 'Comece a organizar o estoque da sua empresa.'}
          </p>
          <div className="mt-6 grid grid-cols-2 rounded-xl bg-[#eef5f0] p-1" role="tablist" aria-label="Forma de acesso">
            {(['login', 'register'] as const).map((item) => {
              const selected = mode === item
              const label = item === 'login' ? 'Entrar' : 'Criar conta'
              return (
                <button
                  key={item}
                  id={`${item}-tab`}
                  type="button"
                  role="tab"
                  aria-selected={selected}
                  aria-controls="access-panel"
                  tabIndex={selected ? 0 : -1}
                  className={selected ? 'min-h-11 rounded-lg bg-white font-semibold text-[#173b27] shadow-sm' : 'min-h-11 rounded-lg font-medium text-[#617168]'}
                  onClick={() => selectMode(item)}
                  onKeyDown={(event) => handleTabKeyDown(event, item)}
                >
                  {label}
                </button>
              )
            })}
          </div>
          {notice && <p className="mt-5 rounded-xl bg-[#eef7f0] p-3 text-sm text-[#173b27]" role="status">{notice}</p>}
          <div className="mt-6" id="access-panel" role="tabpanel" aria-labelledby={`${mode}-tab`}>
            {mode === 'login' ? (
              <LoginForm
                initialEmail={initialEmail}
                onCreateAccount={() => selectMode('register')}
                onGoogleUnavailable={() => setNotice('O login com Google estará disponível em breve.')}
              />
            ) : (
              <RegisterForm onBack={() => selectMode('login')} onSuccess={handleRegistered} />
            )}
          </div>
        </section>
      </div>
    </main>
  )
}
