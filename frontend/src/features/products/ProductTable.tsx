import type { Product } from '../../services/contracts/product'
import { StatusBadge } from '../../components/ui/StatusBadge'
import { getProductStatus } from './product-status'
import { Eye, Pencil, Trash2 } from 'lucide-react'

interface ProductTableProps {
  products: Product[]
  compact?: boolean
  onView?: (product: Product) => void
  onEdit?: (product: Product) => void
  onDelete?: (product: Product) => void
}

export function ProductTable({
  products,
  compact = false,
  onView,
  onEdit,
  onDelete,
}: ProductTableProps) {
  const rows = compact ? products.slice(0, 6) : products
  const hasActions = Boolean(onView || onEdit || onDelete)

  return (
    <div className={compact ? 'min-h-[480px] overflow-x-auto' : 'overflow-x-auto'}>
      <table className="w-full min-w-[720px] border-collapse text-left">
        <thead className="bg-[#f4f9f5] text-xs uppercase text-[#68796e]">
          <tr>
            <th className="px-5 py-3 font-medium">Produto</th>
            <th className="px-5 py-3 font-medium">Categoria</th>
            <th className="px-5 py-3 font-medium">Quantidade</th>
            <th className="px-5 py-3 font-medium">Mínimo</th>
            <th className="px-5 py-3 font-medium">Status</th>
            {hasActions && (
              <th className="px-5 py-3 text-right font-medium">Ações</th>
            )}
          </tr>
        </thead>
        <tbody className="divide-y divide-[#e1ebe4] bg-white">
          {rows.map((product) => (
            <tr key={product.id} className="hover:bg-[#fbfdfb]">
              <td className="px-5 py-4">
                <p className="text-sm font-medium text-[#293a30]">{product.name}</p>
                <p className="mt-0.5 text-xs text-[#718177]">{product.sku}</p>
              </td>
              <td className="px-5 py-4 text-sm text-[#65766b]">{product.categoryName}</td>
              <td className="px-5 py-4 text-sm font-medium">{product.totalQuantity}</td>
              <td className="px-5 py-4 text-sm text-[#65766b]">{product.minStock}</td>
              <td className="px-5 py-4">
                <StatusBadge status={getProductStatus(product.totalQuantity, product.minStock)} />
              </td>
              {hasActions && (
                <td className="whitespace-nowrap px-5 py-4 text-right">
                  {onView && (
                    <button
                      type="button"
                      className="inline-flex min-h-10 items-center gap-2 rounded-xl px-3 text-sm font-medium text-[#107842] hover:bg-[#eef7f0]"
                      aria-label={`Ver ${product.name}`}
                      onClick={() => onView(product)}
                    >
                      <Eye className="size-4" aria-hidden="true" />
                      Ver
                    </button>
                  )}
                  {onEdit && (
                    <button
                      type="button"
                      className="inline-flex min-h-10 items-center gap-2 rounded-xl px-3 text-sm font-medium text-[#52645a] hover:bg-[#eef7f0]"
                      aria-label={`Editar ${product.name}`}
                      onClick={() => onEdit(product)}
                    >
                      <Pencil className="size-4" aria-hidden="true" />
                      Editar
                    </button>
                  )}
                  {onDelete && (
                    <button
                      type="button"
                      className="inline-flex min-h-10 items-center gap-2 rounded-xl px-3 text-sm font-medium text-red-700 hover:bg-red-50"
                      aria-label={`Excluir ${product.name}`}
                      onClick={() => onDelete(product)}
                    >
                      <Trash2 className="size-4" aria-hidden="true" />
                      Excluir
                    </button>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
