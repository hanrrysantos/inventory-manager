import {
  CircleX,
  PackageCheck,
  TrendingUp,
  TriangleAlert,
} from 'lucide-react'
import { ErrorState } from '../../components/feedback/ErrorState'
import { PageLoader } from '../../components/feedback/PageLoader'
import { formatCurrency } from '../../lib/formatters'
import { ProductTable } from '../products/ProductTable'
import { useProducts } from '../products/use-products'
import { AttentionPanel } from './AttentionPanel'
import { SummaryCard } from './SummaryCard'
import { useDashboardSummary } from './use-dashboard-summary'

export function DashboardPage() {
  const summaryQuery = useDashboardSummary()
  const productsQuery = useProducts()

  return (
    <main className="p-5 md:p-8">
      <section aria-label="Indicadores do estoque">
        {summaryQuery.isPending ? (
          <PageLoader label="Carregando resumo..." />
        ) : summaryQuery.isError ? (
          <div className="rounded-[24px] border border-[#d8e5dc] bg-white">
            <ErrorState
              title="Não foi possível carregar o resumo"
              onRetry={() => void summaryQuery.refetch()}
            />
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 2xl:grid-cols-4">
            <SummaryCard
              title="Total de itens"
              value={summaryQuery.data.totalQuantity}
              support={`em ${summaryQuery.data.productCount} produtos`}
              icon={PackageCheck}
              tone="success"
            />
            <SummaryCard
              title="Estoque baixo"
              value={summaryQuery.data.lowStockCount}
              support="abaixo do mínimo"
              icon={TriangleAlert}
              tone="warning"
            />
            <SummaryCard
              title="Em falta"
              value={summaryQuery.data.outOfStockCount}
              support="reposição urgente"
              icon={CircleX}
              tone="danger"
            />
            <SummaryCard
              title="Valor em estoque"
              value={formatCurrency(summaryQuery.data.inventoryValue)}
              support="valor atual estimado"
              icon={TrendingUp}
              tone="info"
            />
          </div>
        )}
      </section>

      <div className="mt-6 grid items-start gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(320px,1fr)]">
        <section
          className="overflow-hidden rounded-[24px] border border-[#d8e5dc] bg-white shadow-[0_3px_10px_rgba(45,82,56,0.08)]"
          role="region"
          aria-label="Resumo de produtos"
        >
          <div className="p-5">
            <h2 className="text-lg font-semibold">Produtos</h2>
            <p className="mt-1 text-sm text-[#718177]">Gerencie os itens do seu estoque</p>
          </div>
          {productsQuery.isPending ? (
            <PageLoader label="Carregando produtos..." />
          ) : productsQuery.isError ? (
            <ErrorState
              title="Não foi possível carregar os produtos"
              onRetry={() => void productsQuery.refetch()}
            />
          ) : productsQuery.data.length === 0 ? (
            <p className="p-8 text-center text-sm text-[#718177]">
              Nenhum produto cadastrado.
            </p>
          ) : (
            <ProductTable products={productsQuery.data} compact />
          )}
        </section>

        {summaryQuery.data ? (
          <AttentionPanel items={summaryQuery.data.attentionItems} />
        ) : (
          <section
            className="rounded-[24px] border border-[#d8e5dc] bg-white"
            role="region"
            aria-label="Produtos que precisam de atenção"
          >
            {summaryQuery.isError ? (
              <ErrorState
                title="Não foi possível carregar os itens críticos"
                onRetry={() => void summaryQuery.refetch()}
              />
            ) : (
              <PageLoader label="Carregando itens críticos..." />
            )}
          </section>
        )}
      </div>
    </main>
  )
}
