import { Eye, EyeOff, LockKeyhole } from 'lucide-react'
import { useState } from 'react'
import type { UseFormRegisterReturn } from 'react-hook-form'

interface PasswordFieldProps {
  id: string
  autoComplete: 'current-password' | 'new-password'
  error?: string
  registration: UseFormRegisterReturn
}

export function PasswordField({ id, autoComplete, error, registration }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)
  const errorId = `${id}-error`
  return (
    <div>
      <label className="mb-2 block text-sm font-medium" htmlFor={id}>Senha</label>
      <div className="relative">
        <LockKeyhole className="absolute left-3 top-3.5 size-5 text-[#718177]" aria-hidden="true" />
        <input id={id} type={visible ? 'text' : 'password'} autoComplete={autoComplete} className="h-12 w-full rounded-xl border border-[#cfddd3] pl-11 pr-12 focus:border-[#168f50] focus:outline-none focus:ring-2 focus:ring-[#168f50]/20" aria-invalid={Boolean(error)} aria-describedby={error ? errorId : undefined} {...registration} />
        <button type="button" className="absolute right-1 top-1 grid size-10 place-items-center rounded-lg text-[#65766b] hover:bg-[#eef7f0]" aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'} onClick={() => setVisible((current) => !current)}>
          {visible ? <EyeOff className="size-5" aria-hidden="true" /> : <Eye className="size-5" aria-hidden="true" />}
        </button>
      </div>
      {error && <p className="mt-1 text-sm text-red-600" id={errorId}>{error}</p>}
    </div>
  )
}
