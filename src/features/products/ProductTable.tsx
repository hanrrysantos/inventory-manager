import type { Product } from '../../services/contracts/product'
import { StatusBadge } from '../../components/ui/StatusBadge'
import { getProductStatus } from './product-status'

interface ProductTableProps {
  products: Product[]
  compact?: boolean
}

export function ProductTable({ products, compact = false }: ProductTableProps) {
  const rows = compact ? products.slice(0, 6) : products

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[720px] border-collapse text-left">
        <thead className="bg-[#f4f9f5] text-xs uppercase text-[#68796e]">
          <tr>
            <th className="px-5 py-3 font-medium">Produto</th>
            <th className="px-5 py-3 font-medium">Categoria</th>
            <th className="px-5 py-3 font-medium">Quantidade</th>
            <th className="px-5 py-3 font-medium">Mínimo</th>
            <th className="px-5 py-3 font-medium">Status</th>
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
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
