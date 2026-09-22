import { z } from 'zod'

export const categorySchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Informe o nome')
    .max(100, 'Use no máximo 100 caracteres'),
  description: z.string(),
})

export type CategoryFormData = z.infer<typeof categorySchema>
