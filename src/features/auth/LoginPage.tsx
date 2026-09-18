import { zodResolver } from '@hookform/resolvers/zod'
import { Leaf, LockKeyhole, Mail } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../../services/api-error'
import { loginSchema, type LoginFormData } from './login-schema'
import { useAuth } from './use-auth'

export function LoginPage() {
  const navigate = useNavigate()
  const { user, login } = useAuth()
  const [apiError, setApiError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({ resolver: zodResolver(loginSchema) })

  if (user) return <Navigate to="/dashboard" replace />

  const onSubmit = handleSubmit(async (data) => {
    setApiError(null)
    try {
      await login(data)
      navigate('/dashboard', { replace: true })
    } catch (error) {
      setApiError(getApiErrorMessage(error, 'Não foi possível entrar.'))
    }
  })

  return (
    <main className="grid min-h-screen place-items-center bg-[#f4fbf6] px-5 py-10">
      <section className="w-full max-w-md rounded-[28px] border border-[#dce8df] bg-white p-8 shadow-[0_18px_50px_rgba(39,79,52,0.10)]">
        <div className="mb-8 flex items-center gap-3">
          <span className="grid size-12 place-items-center rounded-2xl bg-[#5cbd79] text-white">
            <Leaf aria-hidden="true" />
          </span>
          <div>
            <p className="text-xl font-semibold">Verdejar</p>
            <p className="text-sm text-[#6b7d71]">Controle de estoque</p>
          </div>
        </div>

        <h1 className="text-2xl font-semibold">Acesse sua conta</h1>
        <p className="mt-2 text-sm text-[#6b7d71]">
          Entre para acompanhar o estoque da sua empresa.
        </p>

        <form className="mt-8 space-y-5" onSubmit={onSubmit} noValidate>
          <div>
            <label className="mb-2 block text-sm font-medium" htmlFor="email">
              E-mail
            </label>
            <div className="relative">
              <Mail className="absolute left-3 top-3.5 size-5 text-[#7b8e81]" aria-hidden="true" />
              <input
                id="email"
                type="email"
                autoComplete="email"
                className="h-12 w-full rounded-xl border border-[#cfddd3] pl-11 pr-3 focus:border-[#58b978] focus:outline-none focus:ring-2 focus:ring-[#58b978]/20"
                aria-invalid={Boolean(errors.email)}
                {...register('email')}
              />
            </div>
            {errors.email && <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>}
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium" htmlFor="password">
              Senha
            </label>
            <div className="relative">
              <LockKeyhole className="absolute left-3 top-3.5 size-5 text-[#7b8e81]" aria-hidden="true" />
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                className="h-12 w-full rounded-xl border border-[#cfddd3] pl-11 pr-3 focus:border-[#58b978] focus:outline-none focus:ring-2 focus:ring-[#58b978]/20"
                aria-invalid={Boolean(errors.password)}
                {...register('password')}
              />
            </div>
            {errors.password && <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>}
          </div>

          {apiError && (
            <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">
              {apiError}
            </p>
          )}

          <button
            className="h-12 w-full rounded-xl bg-[#58b978] font-medium text-white transition hover:bg-[#46a967] disabled:cursor-not-allowed disabled:opacity-60"
            type="submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Entrando...' : 'Entrar'}
          </button>

          <p className="text-center text-xs leading-5 text-[#718177]">
            O primeiro acesso pode levar até um minuto enquanto o servidor inicia.
          </p>
        </form>
      </section>
    </main>
  )
}
