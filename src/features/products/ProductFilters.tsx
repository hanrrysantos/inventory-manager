import { Search, SlidersHorizontal } from 'lucide-react'
import { cn } from '../../lib/cn'
import type { InventoryStatus } from './product-status'

export type ProductStatusFilter = 'ALL' | InventoryStatus

interface ProductFiltersProps {
  search: string
  status: ProductStatusFilter
  onSearchChange: (value: string) => void
  onStatusChange: (value: ProductStatusFilter) => void
}

const filters: Array<{ value: ProductStatusFilter; label: string }> = [
  { value: 'ALL', label: 'Todos' },
  { value: 'IN_STOCK', label: 'Em estoque' },
  { value: 'LOW_STOCK', label: 'Estoque baixo' },
  { value: 'OUT_OF_STOCK', label: 'Em falta' },
]

export function ProductFilters({
  search,
  status,
  onSearchChange,
  onStatusChange,
}: ProductFiltersProps) {
  return (
    <div className="border-b border-[#dce8df]">
      <div className="p-5">
        <label className="relative block max-w-sm">
          <span className="sr-only">Buscar produto ou SKU</span>
          <Search className="absolute left-3 top-3 size-5 text-[#7a8b80]" aria-hidden="true" />
          <input
            type="search"
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Buscar produto ou SKU..."
            className="h-11 w-full rounded-full border border-[#d0dfd4] bg-[#f7fcf8] pl-10 pr-4 text-sm focus:border-[#58b978] focus:outline-none focus:ring-2 focus:ring-[#58b978]/20"
          />
        </label>
      </div>

      <div className="flex items-center gap-2 overflow-x-auto px-5 pb-4">
        <SlidersHorizontal className="mr-1 size-5 shrink-0 text-[#728279]" aria-hidden="true" />
        {filters.map((filter) => (
          <button
            key={filter.value}
            type="button"
            onClick={() => onStatusChange(filter.value)}
            className={cn(
              'shrink-0 rounded-full px-4 py-2 text-xs font-medium transition',
              status === filter.value
                ? 'bg-[#58b978] text-white'
                : 'bg-[#edf5ef] text-[#64746a] hover:bg-[#e1eee4]',
            )}
          >
            {filter.label}
          </button>
        ))}
      </div>
    </div>
  )
}
