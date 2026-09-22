import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { getApiErrorMessage } from '../../services/api-error'
import type { Category } from '../../services/contracts/category'
import { createCategory, updateCategory } from './categories-api'
import { categorySchema, type CategoryFormData } from './category-schema'

interface CategoryFormProps {
  initialValue?: Category
  onCancel: () => void
  onSuccess: (message: string) => void
}

export function CategoryForm({
  initialValue,
  onCancel,
  onSuccess,
}: CategoryFormProps) {
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CategoryFormData>({
    resolver: zodResolver(categorySchema),
    defaultValues: {
      name: initialValue?.name ?? '',
      description: initialValue?.description ?? '',
    },
  })

  const onSubmit = handleSubmit(async (data) => {
    setApiError(null)
    try {
      if (initialValue) await updateCategory(initialValue.id, data)
      else await createCategory(data)

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['categories'] }),
        queryClient.invalidateQueries({ queryKey: ['products'] }),
      ])
      onSuccess(initialValue ? 'Categoria atualizada.' : 'Categoria criada.')
    } catch (error) {
      setApiError(
        getApiErrorMessage(error, 'Não foi possível salvar a categoria.'),
      )
    }
  })

  return (
    <form className="space-y-5" onSubmit={onSubmit} noValidate>
      <div>
        <label className="mb-2 block text-sm font-medium" htmlFor="category-name">
          Nome
        </label>
        <input
          id="category-name"
          type="text"
          className="h-12 w-full rounded-xl border border-[#cfddd3] px-3 focus:border-[#168f50] focus:outline-none focus:ring-2 focus:ring-[#168f50]/20"
          aria-invalid={Boolean(errors.name)}
          aria-describedby={errors.name ? 'category-name-error' : undefined}
          autoFocus
          {...register('name')}
        />
        {errors.name && (
          <p className="mt-1 text-sm text-red-600" id="category-name-error">
            {errors.name.message}
          </p>
        )}
      </div>

      <div>
        <label
          className="mb-2 block text-sm font-medium"
          htmlFor="category-description"
        >
          Descrição
        </label>
        <textarea
          id="category-description"
          rows={4}
          className="w-full resize-y rounded-xl border border-[#cfddd3] px-3 py-3 focus:border-[#168f50] focus:outline-none focus:ring-2 focus:ring-[#168f50]/20"
          {...register('description')}
        />
      </div>

      {apiError && (
        <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">
          {apiError}
        </p>
      )}

      <div className="flex justify-end gap-3">
        <button
          type="button"
          className="h-11 rounded-xl border border-[#cfddd3] px-4 font-medium text-[#52645a]"
          disabled={isSubmitting}
          onClick={onCancel}
        >
          Cancelar
        </button>
        <button
          type="submit"
          className="h-11 rounded-xl bg-[#107842] px-4 font-medium text-white disabled:opacity-60"
          disabled={isSubmitting}
        >
          {isSubmitting
            ? 'Salvando...'
            : initialValue
              ? 'Salvar alterações'
              : 'Salvar categoria'}
        </button>
      </div>
    </form>
  )
}
