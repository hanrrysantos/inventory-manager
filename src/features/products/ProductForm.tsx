import { zodResolver } from '@hookform/resolvers/zod'
import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { getApiErrorMessage } from '../../services/api-error'
import type { Product } from '../../services/contracts/product'
import { useCategories } from '../categories/use-categories'
import { createProduct, updateProduct } from './products-api'
import {
  productCreateSchema,
  productEditSchema,
  type ProductCreateData,
  type ProductCreateFormData,
  type ProductEditData,
  type ProductEditFormData,
} from './product-schema'

interface ProductFormProps {
  initialValue?: Product
  onCancel: () => void
  onSuccess: (message: string) => void
}

export function ProductForm(props: ProductFormProps) {
  return props.initialValue ? (
    <EditProductForm {...props} initialValue={props.initialValue} />
  ) : (
    <CreateProductForm {...props} />
  )
}

function CreateProductForm({ onCancel, onSuccess }: ProductFormProps) {
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)
  // ponytail: first 100 categories; add a paginated searchable selector when a catalog exceeds that ceiling.
  const categoriesQuery = useCategories({ page: 0, size: 100, sort: 'name,asc' })
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ProductCreateFormData, unknown, ProductCreateData>({
    resolver: zodResolver(productCreateSchema),
    defaultValues: { name: '', sku: '', minStock: 0, categoryId: '' as never },
  })
  const categories = categoriesQuery.data?.content ?? []
  const cannotSubmit = isSubmitting || categoriesQuery.isPending || categories.length === 0

  const onSubmit = handleSubmit(async (data) => {
    setApiError(null)
    try {
      await createProduct(data)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['products'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ])
      onSuccess('Produto criado.')
    } catch (error) {
      setApiError(getApiErrorMessage(error, 'Não foi possível salvar o produto.'))
    }
  })

  return (
    <form className="space-y-5" onSubmit={onSubmit} noValidate>
      <TextField id="product-name" label="Nome" error={errors.name?.message} register={register('name')} autoFocus />
      <TextField id="product-sku" label="SKU" error={errors.sku?.message} register={register('sku')} />
      <TextField id="product-min-stock" label="Estoque mínimo" type="number" min={0} error={errors.minStock?.message} register={register('minStock')} />

      <div>
        <label className="mb-2 block text-sm font-medium" htmlFor="product-category">Categoria</label>
        <select
          id="product-category"
          className="h-12 w-full rounded-xl border border-[#cfddd3] bg-white px-3 focus:border-[#168f50] focus:outline-none focus:ring-2 focus:ring-[#168f50]/20"
          aria-invalid={Boolean(errors.categoryId)}
          aria-describedby={errors.categoryId ? 'product-category-error' : undefined}
          disabled={categoriesQuery.isPending || categories.length === 0}
          {...register('categoryId')}
        >
          <option value="">Selecione uma categoria</option>
          {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
        </select>
        {errors.categoryId && <p className="mt-1 text-sm text-red-600" id="product-category-error">{errors.categoryId.message}</p>}
        {!categoriesQuery.isPending && categories.length === 0 && (
          <p className="mt-2 text-sm text-[#718177]">Cadastre uma categoria antes de criar um produto</p>
        )}
      </div>

      {categoriesQuery.isError && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">Não foi possível carregar as categorias.</p>}
      {apiError && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">{apiError}</p>}
      <FormActions onCancel={onCancel} isSubmitting={isSubmitting} disabled={cannotSubmit} submitLabel="Salvar produto" />
    </form>
  )
}

function EditProductForm({ initialValue, onCancel, onSuccess }: ProductFormProps & { initialValue: Product }) {
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ProductEditFormData, unknown, ProductEditData>({
    resolver: zodResolver(productEditSchema),
    defaultValues: { name: initialValue.name, minStock: initialValue.minStock },
  })

  const onSubmit = handleSubmit(async (data) => {
    setApiError(null)
    try {
      await updateProduct(initialValue.id, data)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['products'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ])
      onSuccess('Produto atualizado.')
    } catch (error) {
      setApiError(getApiErrorMessage(error, 'Não foi possível salvar o produto.'))
    }
  })

  return (
    <form className="space-y-5" onSubmit={onSubmit} noValidate>
      <TextField id="product-name" label="Nome" error={errors.name?.message} register={register('name')} autoFocus />
      <TextField id="product-min-stock" label="Estoque mínimo" type="number" min={0} error={errors.minStock?.message} register={register('minStock')} />
      {apiError && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">{apiError}</p>}
      <FormActions onCancel={onCancel} isSubmitting={isSubmitting} disabled={isSubmitting} submitLabel="Salvar alterações" />
    </form>
  )
}

interface TextFieldProps {
  id: string
  label: string
  error?: string
  register: ReturnType<ReturnType<typeof useForm>['register']>
  type?: 'text' | 'number'
  min?: number
  autoFocus?: boolean
}

function TextField({ id, label, error, register, type = 'text', min, autoFocus }: TextFieldProps) {
  return (
    <div>
      <label className="mb-2 block text-sm font-medium" htmlFor={id}>{label}</label>
      <input
        id={id}
        type={type}
        min={min}
        className="h-12 w-full rounded-xl border border-[#cfddd3] px-3 focus:border-[#168f50] focus:outline-none focus:ring-2 focus:ring-[#168f50]/20"
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        autoFocus={autoFocus}
        {...register}
      />
      {error && <p className="mt-1 text-sm text-red-600" id={`${id}-error`}>{error}</p>}
    </div>
  )
}

function FormActions({ onCancel, isSubmitting, disabled, submitLabel }: { onCancel: () => void; isSubmitting: boolean; disabled: boolean; submitLabel: string }) {
  return (
    <div className="flex justify-end gap-3">
      <button type="button" className="h-11 rounded-xl border border-[#cfddd3] px-4 font-medium text-[#52645a]" disabled={isSubmitting} onClick={onCancel}>Cancelar</button>
      <button type="submit" className="h-11 rounded-xl bg-[#107842] px-4 font-medium text-white disabled:opacity-60" disabled={disabled}>{isSubmitting ? 'Salvando...' : submitLabel}</button>
    </div>
  )
}
