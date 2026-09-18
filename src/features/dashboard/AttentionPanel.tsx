import { TriangleAlert } from 'lucide-react'
import type { AttentionItem } from '../../services/contracts/dashboard'
import { cn } from '../../lib/cn'

const barStyles = {
  IN_STOCK: 'bg-[#58b978]',
  LOW_STOCK: 'bg-[#ddc85f]',
  OUT_OF_STOCK: 'bg-[#e45d5d]',
}

function getProgress(item: AttentionItem): number {
  if (item.minStock <= 0) return 100
  return Math.min(100, Math.max(0, (item.quantity / item.minStock) * 100))
}

export function AttentionPanel({ items }: { items: AttentionItem[] }) {
  return (
    <section
      className="rounded-[24px] border border-[#d8e5dc] bg-white p-5 shadow-[0_3px_10px_rgba(45,82,56,0.08)]"
      role="region"
      aria-label="Produtos que precisam de atenção"
    >
      <div className="flex items-center gap-3">
        <span className="grid size-10 place-items-center rounded-full bg-[#fbf4df] text-[#e7a45d]">
          <TriangleAlert className="size-5" aria-hidden="true" />
        </span>
        <div>
          <h2 className="font-semibold">Precisam de atenção</h2>
          <p className="text-sm text-[#718177]">Itens para repor em breve</p>
        </div>
      </div>

      <div className="mt-5 space-y-3">
        {items.length === 0 ? (
          <p className="rounded-2xl bg-[#f5faf6] p-4 text-sm text-[#65766b]">
            Nenhum item crítico no momento.
          </p>
        ) : (
          items.map((item) => {
            const progress = getProgress(item)
            return (
              <article
                key={item.productId}
                className="rounded-2xl border border-[#dbe8de] bg-[#fbfdfb] p-4"
              >
                <div className="flex items-start justify-between gap-3 text-sm">
                  <div>
                    <p className="font-medium text-[#293a30]">{item.productName}</p>
                    <p className="mt-0.5 text-xs text-[#78887e]">{item.sku}</p>
                  </div>
                  <span className="shrink-0 text-xs text-[#65766b]">
                    {item.quantity}/{item.minStock}
                  </span>
                </div>
                <div
                  className="mt-3 h-1.5 overflow-hidden rounded-full bg-[#e7f0e9]"
                  role="progressbar"
                  aria-label={`Nível de estoque de ${item.productName}`}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={Math.round(progress)}
                >
                  <div
                    className={cn('h-full rounded-full', barStyles[item.status])}
                    style={{ width: `${progress}%` }}
                  />
                </div>
              </article>
            )
          })
        )}
      </div>
    </section>
  )
}
