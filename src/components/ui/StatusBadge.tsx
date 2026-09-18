import { cn } from '../../lib/cn'
import type { InventoryStatus } from '../../features/products/product-status'

const statusStyles: Record<InventoryStatus, string> = {
  IN_STOCK: 'bg-[#e4f4e8] text-[#48a866]',
  LOW_STOCK: 'bg-[#f8f1d8] text-[#d39b50]',
  OUT_OF_STOCK: 'bg-[#fbe3e3] text-[#e55c5c]',
}

const statusLabels: Record<InventoryStatus, string> = {
  IN_STOCK: 'Em estoque',
  LOW_STOCK: 'Estoque baixo',
  OUT_OF_STOCK: 'Em falta',
}

export function StatusBadge({ status }: { status: InventoryStatus }) {
  return (
    <span
      className={cn(
        'inline-flex rounded-full px-3 py-1 text-xs font-medium',
        statusStyles[status],
      )}
    >
      {statusLabels[status]}
    </span>
  )
}
