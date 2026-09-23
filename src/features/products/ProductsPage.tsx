import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { PageLoader } from '../../components/feedback/PageLoader'
import { PaginationControls } from '../../components/navigation/PaginationControls'
import { AppDialog } from '../../components/ui/AppDialog'
import { ProductTable } from './ProductTable'
import { getProduct } from './products-api'
import { useProducts } from './use-products'

const PAGE_SIZE = 20
const SORT_PROPERTIES = ['id', 'name', 'sku'] as const

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null)
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
                <option value="id,asc">Mais antigos</option>
                <option value="id,desc">Mais recentes</option>
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
          </div>
        </div>

        {!hasProducts ? (
          <EmptyState
            title="Nenhum produto cadastrado"
            description="O catálogo ainda não possui produtos."
          />
        ) : (
          <>
            <ProductTable
              products={productPage.content}
              onView={(product) => setSelectedProductId(product.id)}
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
    </main>
  )
}
