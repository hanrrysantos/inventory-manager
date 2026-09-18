import { zodResolver } from '@hookform/resolvers/zod'
import { Mail } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../../services/api-error'
import { loginSchema, type LoginFormData } from './login-schema'
import { PasswordField } from './PasswordField'
import { useAuth } from './use-auth'

interface LoginFormProps {
  initialEmail: string
  onCreateAccount: () => void
  onGoogleUnavailable: () => void
}

export function LoginForm({ initialEmail, onCreateAccount, onGoogleUnavailable }: LoginFormProps) {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [apiError, setApiError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: initialEmail, password: '' },
  })

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
    <form className="space-y-5" onSubmit={onSubmit} noValidate>
      <div>
        <label className="mb-2 block text-sm font-medium" htmlFor="login-email">E-mail</label>
        <div className="relative">
          <Mail className="absolute left-3 top-3.5 size-5 text-[#718177]" aria-hidden="true" />
          <input
            id="login-email"
            type="email"
            autoComplete="email"
            className="h-12 w-full rounded-xl border border-[#cfddd3] pl-11 pr-3 focus:border-[#168f50] focus:outline-none focus:ring-2 focus:ring-[#168f50]/20"
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? 'login-email-error' : undefined}
            {...register('email')}
          />
        </div>
        {errors.email && <p className="mt-1 text-sm text-red-600" id="login-email-error">{errors.email.message}</p>}
      </div>
      <PasswordField id="login-password" autoComplete="current-password" error={errors.password?.message} registration={register('password')} />
      {apiError && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">{apiError}</p>}
      <button className="h-12 w-full rounded-xl bg-[#168f50] font-medium text-white transition hover:bg-[#107842] disabled:cursor-not-allowed disabled:opacity-60" type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Entrando...' : 'Entrar'}
      </button>
      <div className="flex items-center gap-3" aria-hidden="true"><span className="h-px flex-1 bg-[#dce8df]" /><span className="text-xs font-medium uppercase tracking-[0.15em] text-[#7a8980]">ou</span><span className="h-px flex-1 bg-[#dce8df]" /></div>
      <button className="flex h-12 w-full items-center justify-center gap-3 rounded-xl bg-[#151a17] font-medium text-white hover:bg-black" type="button" onClick={onGoogleUnavailable}><span className="grid size-6 place-items-center rounded-full bg-white font-bold text-[#4285f4]" aria-hidden="true">G</span>Entrar com Google</button>
      <p className="text-center text-sm text-[#68786f]">Ainda não tem uma conta? <button className="inline-flex min-h-11 min-w-11 items-center justify-center font-semibold text-[#168f50]" type="button" onClick={onCreateAccount}>Criar conta</button></p>
      <p className="text-center text-xs leading-5 text-[#718177]">O primeiro acesso pode levar até um minuto enquanto o servidor inicia.</p>
    </form>
  )
}
