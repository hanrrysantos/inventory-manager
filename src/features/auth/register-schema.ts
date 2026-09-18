import { z } from 'zod'

export const registerSchema = z.object({
  name: z.string().trim().min(1, 'Informe seu nome'),
  email: z.email('Informe um e-mail válido'),
  password: z.string().min(6, 'A senha deve ter pelo menos 6 caracteres'),
})

export type RegisterFormData = z.infer<typeof registerSchema>
