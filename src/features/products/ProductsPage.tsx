import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { PageLoader } from '../../components/feedback/PageLoader'
import { ProductFilters, type ProductStatusFilter } from './ProductFilters'
import { ProductTable } from './ProductTable'
import { getProductStatus } from './product-status'
import { useProducts } from './use-products'

const validStatuses = new Set<ProductStatusFilter>([
  'ALL',
  'IN_STOCK',
  'LOW_STOCK',
  'OUT_OF_STOCK',
])

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const productsQuery = useProducts()
  const search = searchParams.get('q') ?? ''
  const rawStatus = searchParams.get('status') ?? 'ALL'
  const status = validStatuses.has(rawStatus as ProductStatusFilter)
    ? (rawStatus as ProductStatusFilter)
    : 'ALL'

  const filteredProducts = useMemo(() => {
    const normalizedSearch = search.trim().toLocaleLowerCase('pt-BR')
    return (productsQuery.data ?? []).filter((product) => {
      const matchesSearch =
        !normalizedSearch ||
        product.name.toLocaleLowerCase('pt-BR').includes(normalizedSearch) ||
        product.sku.toLocaleLowerCase('pt-BR').includes(normalizedSearch)
      const matchesStatus =
        status === 'ALL' ||
        getProductStatus(product.totalQuantity, product.minStock) === status
      return matchesSearch && matchesStatus
    })
  }, [productsQuery.data, search, status])

  const updateParam = (key: 'q' | 'status', value: string, emptyValue: string) => {
    const next = new URLSearchParams(searchParams)
    if (!value || value === emptyValue) next.delete(key)
    else next.set(key, value)
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

  const hasProducts = productsQuery.data.length > 0

  return (
    <main className="p-5 md:p-8">
      <section className="overflow-hidden rounded-[24px] border border-[#d8e5dc] bg-white shadow-[0_3px_10px_rgba(45,82,56,0.08)]">
        <div className="flex items-center justify-between gap-4 p-5">
          <div>
            <h2 className="text-lg font-semibold">Produtos</h2>
            <p className="mt-1 text-sm text-[#718177]">
              {productsQuery.data.length} produtos no catálogo
            </p>
          </div>
        </div>

        {hasProducts && (
          <ProductFilters
            search={search}
            status={status}
            onSearchChange={(value) => updateParam('q', value, '')}
            onStatusChange={(value) => updateParam('status', value, 'ALL')}
          />
        )}

        {!hasProducts ? (
          <EmptyState
            title="Nenhum produto cadastrado"
            description="O catálogo ainda não possui produtos."
          />
        ) : filteredProducts.length === 0 ? (
          <EmptyState
            title="Nenhum produto encontrado"
            description="Altere a busca ou os filtros para ver outros resultados."
          />
        ) : (
          <ProductTable products={filteredProducts} />
        )}
      </section>
    </main>
  )
}
