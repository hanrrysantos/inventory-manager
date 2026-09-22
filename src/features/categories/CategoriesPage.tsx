import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, Pencil, Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { PageLoader } from '../../components/feedback/PageLoader'
import { PaginationControls } from '../../components/navigation/PaginationControls'
import { AppDialog } from '../../components/ui/AppDialog'
import { getApiErrorMessage } from '../../services/api-error'
import type { Category } from '../../services/contracts/category'
import { useAuth } from '../auth/use-auth'
import { CategoryForm } from './CategoryForm'
import { deleteCategory, getCategory } from './categories-api'
import { useCategories } from './use-categories'

const PAGE_SIZE = 20
const SORT_PROPERTIES = ['id', 'name'] as const

export function CategoriesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null)
  const [formCategory, setFormCategory] = useState<Category | 'new' | null>(null)
  const [deletingCategory, setDeletingCategory] = useState<Category | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const requestedPage = Number(searchParams.get('page') ?? 0)
  const page = Number.isInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0
  const requestedProperty = searchParams.get('sort')
  const property = SORT_PROPERTIES.includes(
    requestedProperty as (typeof SORT_PROPERTIES)[number],
  )
    ? requestedProperty!
    : 'name'
  const direction = searchParams.get('direction') === 'desc' ? 'desc' : 'asc'
  const categoriesQuery = useCategories({
    page,
    size: PAGE_SIZE,
    sort: `${property},${direction}`,
  })
  const categoryQuery = useQuery({
    queryKey: ['categories', 'detail', selectedCategoryId],
    queryFn: () => getCategory(selectedCategoryId!),
    enabled: selectedCategoryId !== null,
  })
  const isAdmin = user?.role === 'ADMIN'

  const handleDelete = async () => {
    if (!deletingCategory) return
    setDeleteError(null)
    setIsDeleting(true)
    try {
      await deleteCategory(deletingCategory.id)
      await queryClient.invalidateQueries({ queryKey: ['categories'] })
      setDeletingCategory(null)
      setNotice('Categoria excluída.')
    } catch (error) {
      setDeleteError(
        getApiErrorMessage(error, 'Não foi possível excluir a categoria.'),
      )
    } finally {
      setIsDeleting(false)
    }
  }

  const updatePage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams)
    if (nextPage === 0) next.delete('page')
    else next.set('page', String(nextPage))
    setSearchParams(next, { replace: true })
  }

  const updateSort = (value: string) => {
    const [nextProperty, nextDirection] = value.split(',')
    const next = new URLSearchParams(searchParams)
    next.delete('page')
    if (nextProperty === 'name' && nextDirection === 'asc') next.delete('sort')
    else next.set('sort', nextProperty)
    if (nextDirection === 'asc') next.delete('direction')
    else next.set('direction', nextDirection)
    setSearchParams(next, { replace: true })
  }

  if (categoriesQuery.isPending) {
    return <PageLoader label="Carregando categorias..." />
  }

  if (categoriesQuery.isError) {
    return (
      <ErrorState
        title="Não foi possível carregar as categorias"
        onRetry={() => void categoriesQuery.refetch()}
      />
    )
  }

  const categoryPage = categoriesQuery.data

  return (
    <main className="p-5 md:p-8">
      <section className="overflow-hidden rounded-[24px] border border-[#d8e5dc] bg-white shadow-[0_3px_10px_rgba(45,82,56,0.08)]">
        <div className="flex flex-col gap-4 p-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 className="text-lg font-semibold">Categorias</h2>
            <p className="mt-1 text-sm text-[#718177]">
              {categoryPage.totalElements} categorias no catálogo
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <label className="text-sm font-medium text-[#52645a]">
              Ordenar categorias
              <select
                className="mt-1 block h-11 rounded-xl border border-[#cfddd3] bg-white px-3"
                value={`${property},${direction}`}
                onChange={(event) => updateSort(event.target.value)}
              >
                <option value="name,asc">Nome (A–Z)</option>
                <option value="name,desc">Nome (Z–A)</option>
                <option value="id,asc">Mais antigas</option>
                <option value="id,desc">Mais recentes</option>
              </select>
            </label>
            {isAdmin && (
              <button
                type="button"
                className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#107842] px-4 font-medium text-white"
                onClick={() => setFormCategory('new')}
              >
                <Plus className="size-4" aria-hidden="true" />
                Nova categoria
              </button>
            )}
          </div>
        </div>

        {notice && (
          <p className="mx-5 mb-5 rounded-xl bg-[#eef7f0] p-3 text-sm text-[#173b27]" role="status">
            {notice}
          </p>
        )}

        {categoryPage.content.length === 0 ? (
          <EmptyState
            title="Nenhuma categoria cadastrada"
            description="O catálogo ainda não possui categorias."
          />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] border-collapse text-left">
                <thead className="bg-[#f4f9f5] text-xs uppercase text-[#68796e]">
                  <tr>
                    <th className="px-5 py-3 font-medium">Nome</th>
                    <th className="px-5 py-3 font-medium">Descrição</th>
                    <th className="px-5 py-3 text-right font-medium">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#e1ebe4]">
                  {categoryPage.content.map((category) => (
                    <tr key={category.id}>
                      <td className="px-5 py-4 text-sm font-medium">{category.name}</td>
                      <td className="px-5 py-4 text-sm text-[#65766b]">
                        {category.description || '—'}
                      </td>
                      <td className="px-5 py-4 text-right">
                        <button
                          type="button"
                          className="inline-flex min-h-10 items-center gap-2 rounded-xl px-3 text-sm font-medium text-[#107842] hover:bg-[#eef7f0]"
                          aria-label={`Ver ${category.name}`}
                          onClick={() => setSelectedCategoryId(category.id)}
                        >
                          <Eye className="size-4" aria-hidden="true" />
                          Ver
                        </button>
                        {isAdmin && (
                          <>
                            <button
                              type="button"
                              className="inline-flex min-h-10 items-center gap-2 rounded-xl px-3 text-sm font-medium text-[#52645a] hover:bg-[#eef7f0]"
                              aria-label={`Editar ${category.name}`}
                              onClick={() => setFormCategory(category)}
                            >
                              <Pencil className="size-4" aria-hidden="true" />
                              Editar
                            </button>
                            <button
                              type="button"
                              className="inline-flex min-h-10 items-center gap-2 rounded-xl px-3 text-sm font-medium text-red-700 hover:bg-red-50"
                              aria-label={`Excluir ${category.name}`}
                              onClick={() => {
                                setDeleteError(null)
                                setDeletingCategory(category)
                              }}
                            >
                              <Trash2 className="size-4" aria-hidden="true" />
                              Excluir
                            </button>
                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <PaginationControls
              page={categoryPage.page}
              totalPages={categoryPage.totalPages}
              onPageChange={updatePage}
            />
          </>
        )}
      </section>

      {selectedCategoryId !== null && (
        <AppDialog
          title="Detalhes da categoria"
          onClose={() => setSelectedCategoryId(null)}
        >
          {categoryQuery.isPending ? (
            <PageLoader label="Carregando categoria..." />
          ) : categoryQuery.isError ? (
            <ErrorState
              title="Não foi possível carregar a categoria"
              onRetry={() => void categoryQuery.refetch()}
            />
          ) : (
            <dl className="space-y-4 text-sm">
              <div>
                <dt className="text-[#718177]">Nome</dt>
                <dd className="mt-1 font-medium">{categoryQuery.data.name}</dd>
              </div>
              <div>
                <dt className="text-[#718177]">Descrição</dt>
                <dd className="mt-1 font-medium">
                  {categoryQuery.data.description || 'Sem descrição'}
                </dd>
              </div>
            </dl>
          )}
        </AppDialog>
      )}

      {formCategory && (
        <AppDialog
          title={formCategory === 'new' ? 'Nova categoria' : 'Editar categoria'}
          onClose={() => setFormCategory(null)}
        >
          <CategoryForm
            initialValue={formCategory === 'new' ? undefined : formCategory}
            onCancel={() => setFormCategory(null)}
            onSuccess={(message) => {
              setFormCategory(null)
              setNotice(message)
            }}
          />
        </AppDialog>
      )}

      {deletingCategory && (
        <AppDialog
          title="Excluir categoria"
          onClose={() => setDeletingCategory(null)}
        >
          <p className="text-sm text-[#52645a]">
            Excluir <strong>{deletingCategory.name}</strong>? Esta ação não
            pode ser desfeita.
          </p>
          {deleteError && (
            <p className="mt-4 rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">
              {deleteError}
            </p>
          )}
          <div className="mt-6 flex justify-end gap-3">
            <button
              type="button"
              className="h-11 rounded-xl border border-[#cfddd3] px-4 font-medium"
              disabled={isDeleting}
              onClick={() => setDeletingCategory(null)}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="h-11 rounded-xl bg-red-700 px-4 font-medium text-white disabled:opacity-60"
              disabled={isDeleting}
              onClick={() => void handleDelete()}
            >
              {isDeleting ? 'Excluindo...' : 'Confirmar exclusão'}
            </button>
          </div>
        </AppDialog>
      )}
    </main>
  )
}
