import { ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '../../lib/cn'

interface PaginationControlsProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  compact?: boolean
}

export function PaginationControls({
  page,
  totalPages,
  onPageChange,
  compact = false,
}: PaginationControlsProps) {
  if (totalPages <= 1) return null

  return (
    <nav
      aria-label="Paginação de produtos"
      className={cn(
        'flex items-center justify-center border-t border-[#e1ebe4]',
        compact ? 'gap-2 px-4 py-3' : 'gap-4 px-5 py-4',
      )}
    >
      <button
        type="button"
        aria-label="Página anterior"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
        className={cn(
          'inline-flex items-center justify-center rounded-full border border-[#d0dfd4] text-[#506158] transition hover:bg-[#edf5ef] disabled:cursor-not-allowed disabled:opacity-40',
          compact ? 'size-8' : 'h-9 gap-1 px-3 text-sm',
        )}
      >
        <ChevronLeft className="size-4" aria-hidden="true" />
        {!compact && <span>Anterior</span>}
      </button>

      <span className={cn('text-[#65766b]', compact ? 'text-xs' : 'text-sm')}>
        {compact ? `${page + 1} / ${totalPages}` : `Página ${page + 1} de ${totalPages}`}
      </span>

      <button
        type="button"
        aria-label="Próxima página"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className={cn(
          'inline-flex items-center justify-center rounded-full border border-[#d0dfd4] text-[#506158] transition hover:bg-[#edf5ef] disabled:cursor-not-allowed disabled:opacity-40',
          compact ? 'size-8' : 'h-9 gap-1 px-3 text-sm',
        )}
      >
        {!compact && <span>Próxima</span>}
        <ChevronRight className="size-4" aria-hidden="true" />
      </button>
    </nav>
  )
}
