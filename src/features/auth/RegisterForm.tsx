import { zodResolver } from '@hookform/resolvers/zod'
import { Mail, UserRound } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { getApiErrorMessage } from '../../services/api-error'
import { registerAccount } from './auth-api'
import { PasswordField } from './PasswordField'
import { registerSchema, type RegisterFormData } from './register-schema'

interface RegisterFormProps {
  onBack: () => void
  onSuccess: (email: string) => void
}

export function RegisterForm({ onBack, onSuccess }: RegisterFormProps) {
  const [apiError, setApiError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: { name: '', email: '', password: '' },
  })

  const onSubmit = handleSubmit(async (data) => {
    setApiError(null)
    try {
      await registerAccount(data)
      reset()
      onSuccess(data.email)
    } catch (error) {
      setApiError(getApiErrorMessage(error, 'Não foi possível criar sua conta.'))
    }
  })

  return (
    <form className="space-y-5" onSubmit={onSubmit} noValidate>
      <div>
        <label className="mb-2 block text-sm font-medium" htmlFor="register-name">Nome</label>
        <div className="relative">
          <UserRound className="absolute left-3 top-3.5 size-5 text-[#718177]" aria-hidden="true" />
          <input
            id="register-name"
            type="text"
            autoComplete="name"
            className="h-12 w-full rounded-xl border border-[#cfddd3] pl-11 pr-3 focus:border-[#168f50] focus:outline-none focus:ring-2 focus:ring-[#168f50]/20"
            aria-invalid={Boolean(errors.name)}
            aria-describedby={errors.name ? 'register-name-error' : undefined}
            {...register('name')}
          />
        </div>
        {errors.name && <p className="mt-1 text-sm text-red-600" id="register-name-error">{errors.name.message}</p>}
      </div>
      <div>
        <label className="mb-2 block text-sm font-medium" htmlFor="register-email">E-mail</label>
        <div className="relative">
          <Mail className="absolute left-3 top-3.5 size-5 text-[#718177]" aria-hidden="true" />
          <input
            id="register-email"
            type="email"
            autoComplete="email"
            className="h-12 w-full rounded-xl border border-[#cfddd3] pl-11 pr-3 focus:border-[#168f50] focus:outline-none focus:ring-2 focus:ring-[#168f50]/20"
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? 'register-email-error' : undefined}
            {...register('email')}
          />
        </div>
        {errors.email && <p className="mt-1 text-sm text-red-600" id="register-email-error">{errors.email.message}</p>}
      </div>
      <PasswordField id="register-password" autoComplete="new-password" error={errors.password?.message} registration={register('password')} />
      {apiError && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">{apiError}</p>}
      <button className="h-12 w-full rounded-xl bg-[#168f50] font-medium text-white transition hover:bg-[#107842] disabled:cursor-not-allowed disabled:opacity-60" type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Criando conta...' : 'Criar minha conta'}
      </button>
      <p className="text-center text-sm text-[#68786f]">Já tem uma conta? <button className="font-semibold text-[#168f50]" type="button" onClick={onBack}>Entrar</button></p>
    </form>
  )
}
