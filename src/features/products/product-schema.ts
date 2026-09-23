import { z } from 'zod'

const name = z
  .string()
  .trim()
  .min(1, 'Informe o nome')
  .max(255, 'Use no máximo 255 caracteres')

const minStock = z.coerce
  .number()
  .int('Use um número inteiro')
  .min(0, 'O estoque mínimo não pode ser negativo')

export const productCreateSchema = z.object({
  name,
  sku: z
    .string()
    .trim()
    .min(1, 'Informe o SKU')
    .max(50, 'Use no máximo 50 caracteres'),
  minStock,
  categoryId: z.coerce
    .number()
    .int()
    .positive('Selecione uma categoria'),
})

export const productEditSchema = z.object({ name, minStock })

export type ProductCreateFormData = z.input<typeof productCreateSchema>
export type ProductCreateData = z.output<typeof productCreateSchema>
export type ProductEditFormData = z.input<typeof productEditSchema>
export type ProductEditData = z.output<typeof productEditSchema>
