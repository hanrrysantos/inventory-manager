import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { PageLoader } from '../../components/feedback/PageLoader'
import { PaginationControls } from '../../components/navigation/PaginationControls'
import { AppDialog } from '../../components/ui/AppDialog'
import { getApiErrorMessage } from '../../services/api-error'
import type { Product } from '../../services/contracts/product'
import { useAuth } from '../auth/use-auth'
import { ProductForm } from './ProductForm'
import { ProductTable } from './ProductTable'
import { deleteProduct, getProduct } from './products-api'
import { useProducts } from './use-products'

const PAGE_SIZE = 20
const SORT_PROPERTIES = ['name', 'sku'] as const

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null)
  const [formProduct, setFormProduct] = useState<Product | 'new' | null>(null)
  const [deletingProduct, setDeletingProduct] = useState<Product | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isFormBusy, setIsFormBusy] = useState(false)
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
  const lowStock = searchParams.get('lowStock') === 'true'
  const productsQuery = useProducts(
    { page, size: PAGE_SIZE, sort: `${property},${direction}` },
    lowStock,
  )
  const productQuery = useQuery({
    queryKey: ['products', 'detail', selectedProductId],
    queryFn: () => getProduct(selectedProductId!),
    enabled: selectedProductId !== null,
  })
  const isAdmin = user?.role === 'ADMIN'

  const handleDelete = async (close: () => void) => {
    if (!deletingProduct) return
    setDeleteError(null)
    setIsDeleting(true)
    try {
      await deleteProduct(deletingProduct.id)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['products'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ])
      setNotice('Produto excluído.')
      close()
    } catch (error) {
      setDeleteError(getApiErrorMessage(error, 'Não foi possível excluir o produto.'))
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

  const updateLowStock = (checked: boolean) => {
    const next = new URLSearchParams(searchParams)
    next.delete('page')
    if (checked) next.set('lowStock', 'true')
    else next.delete('lowStock')
    setSearchParams(next, { replace: true })
  }

  if (productsQuery.isPending) return <PageLoader label="Carregando produtos..." />
  if (productsQuery.isError) {
    return (
      <ErrorState
        title="Não foi possível carregar os produtos"
        onRetry={() => void productsQuery.refetch()}
      />
    )
  }

  const productPage = productsQuery.data
  if (page > 0 && page >= productPage.totalPages) {
    const next = new URLSearchParams(searchParams)
    const lastPage = Math.max(productPage.totalPages - 1, 0)
    if (lastPage === 0) next.delete('page')
    else next.set('page', String(lastPage))
    return <Navigate replace to={{ search: next.toString() }} />
  }
  const hasProducts = productPage.content.length > 0

  return (
    <main className="p-5 md:p-8">
      <section className="overflow-hidden rounded-[24px] border border-[#d8e5dc] bg-white shadow-[0_3px_10px_rgba(45,82,56,0.08)]">
        <div className="flex flex-col gap-4 p-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 className="text-lg font-semibold">Produtos</h2>
            <p className="mt-1 text-sm text-[#718177]">
              {productPage.totalElements} produtos no catálogo
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <label className="text-sm font-medium text-[#52645a]">
              Ordenar produtos
              <select
                className="mt-1 block h-11 rounded-xl border border-[#cfddd3] bg-white px-3"
                value={`${property},${direction}`}
                onChange={(event) => updateSort(event.target.value)}
              >
                <option value="name,asc">Nome (A–Z)</option>
                <option value="name,desc">Nome (Z–A)</option>
                <option value="sku,asc">SKU (A–Z)</option>
                <option value="sku,desc">SKU (Z–A)</option>
              </select>
            </label>
            <label className="flex h-11 items-center gap-2 rounded-xl border border-[#cfddd3] px-3 text-sm font-medium text-[#52645a]">
              <input
                type="checkbox"
                checked={lowStock}
                onChange={(event) => updateLowStock(event.target.checked)}
              />
              Somente estoque baixo
            </label>
            {isAdmin && (
              <button
                type="button"
                className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#107842] px-4 font-medium text-white"
                onClick={() => setFormProduct('new')}
              >
                <Plus className="size-4" aria-hidden="true" />
                Novo produto
              </button>
            )}
          </div>
        </div>

        {notice && (
          <p className="mx-5 mb-5 rounded-xl bg-[#eef7f0] p-3 text-sm text-[#173b27]" role="status">{notice}</p>
        )}

        {!hasProducts ? (
          <EmptyState
            title={lowStock ? 'Nenhum produto com estoque baixo' : 'Nenhum produto cadastrado'}
            description={lowStock ? 'Não há produtos abaixo do estoque mínimo.' : 'O catálogo ainda não possui produtos.'}
          />
        ) : (
          <>
            <ProductTable
              products={productPage.content}
              onView={(product) => setSelectedProductId(product.id)}
              onEdit={isAdmin ? setFormProduct : undefined}
              onDelete={isAdmin ? (product) => {
                setDeleteError(null)
                setDeletingProduct(product)
              } : undefined}
            />
            <PaginationControls
              page={productPage.page}
              totalPages={productPage.totalPages}
              onPageChange={updatePage}
            />
          </>
        )}
      </section>

      {selectedProductId !== null && (
        <AppDialog
          title="Detalhes do produto"
          onClose={() => setSelectedProductId(null)}
        >
          {productQuery.isPending ? (
            <PageLoader label="Carregando produto..." />
          ) : productQuery.isError ? (
            <ErrorState
              title="Não foi possível carregar o produto"
              onRetry={() => void productQuery.refetch()}
            />
          ) : (
            <dl className="grid gap-4 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-[#718177]">Nome</dt>
                <dd className="mt-1 font-medium">{productQuery.data.name}</dd>
              </div>
              <div>
                <dt className="text-[#718177]">SKU</dt>
                <dd className="mt-1 font-medium">{productQuery.data.sku}</dd>
              </div>
              <div>
                <dt className="text-[#718177]">Categoria</dt>
                <dd className="mt-1 font-medium">{productQuery.data.categoryName}</dd>
              </div>
              <div>
                <dt className="text-[#718177]">Quantidade</dt>
                <dd className="mt-1 font-medium">{productQuery.data.totalQuantity}</dd>
              </div>
              <div>
                <dt className="text-[#718177]">Estoque mínimo</dt>
                <dd className="mt-1 font-medium">{productQuery.data.minStock}</dd>
              </div>
            </dl>
          )}
        </AppDialog>
      )}

      {formProduct && (
        <AppDialog title={formProduct === 'new' ? 'Novo produto' : 'Editar produto'} onClose={() => setFormProduct(null)} busy={isFormBusy}>
          {(close) => <ProductForm
              initialValue={formProduct === 'new' ? undefined : formProduct}
              onCancel={close}
              onBusyChange={setIsFormBusy}
              onSuccess={(message) => {
                setNotice(message)
                close()
              }}
            />}
        </AppDialog>
      )}

      {deletingProduct && (
        <AppDialog title="Excluir produto" onClose={() => setDeletingProduct(null)} busy={isDeleting}>
          {(close) => <><p className="text-sm text-[#52645a]">Excluir <strong>{deletingProduct.name}</strong>? Esta ação não pode ser desfeita.</p>
          {deleteError && <p className="mt-4 rounded-xl bg-red-50 p-3 text-sm text-red-700" role="alert">{deleteError}</p>}
          <div className="mt-6 flex justify-end gap-3">
            <button type="button" className="h-11 rounded-xl border border-[#cfddd3] px-4 font-medium" disabled={isDeleting} onClick={close}>Cancelar</button>
            <button type="button" className="h-11 rounded-xl bg-red-700 px-4 font-medium text-white disabled:opacity-60" disabled={isDeleting} onClick={() => void handleDelete(close)}>{isDeleting ? 'Excluindo...' : 'Confirmar exclusão'}</button>
          </div>
          </>}
        </AppDialog>
      )}
    </main>
  )
}
