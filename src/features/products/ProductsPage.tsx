import { useSearchParams } from 'react-router-dom'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { PageLoader } from '../../components/feedback/PageLoader'
import { PaginationControls } from '../../components/navigation/PaginationControls'
import { ProductTable } from './ProductTable'
import { useProducts } from './use-products'

const PAGE_SIZE = 20

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedPage = Number(searchParams.get('page') ?? 0)
  const page = Number.isInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0
  const productsQuery = useProducts({ page, size: PAGE_SIZE, sort: 'name,asc' })

  const updatePage = (nextPage: number) => {
    const next = new URLSearchParams(searchParams)
    if (nextPage === 0) next.delete('page')
    else next.set('page', String(nextPage))
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
        <div className="flex items-center justify-between gap-4 p-5">
          <div>
            <h2 className="text-lg font-semibold">Produtos</h2>
            <p className="mt-1 text-sm text-[#718177]">
              {productPage.totalElements} produtos no catálogo
            </p>
          </div>
        </div>

        {!hasProducts ? (
          <EmptyState
            title="Nenhum produto cadastrado"
            description="O catálogo ainda não possui produtos."
          />
        ) : (
          <>
            <ProductTable products={productPage.content} />
            <PaginationControls
              page={productPage.page}
              totalPages={productPage.totalPages}
              onPageChange={updatePage}
            />
          </>
        )}
      </section>
    </main>
  )
}
