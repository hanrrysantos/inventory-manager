import { LogOut, Menu } from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { formatRole } from '../../lib/formatters'
import { useAuth } from '../../features/auth/use-auth'

interface AppHeaderProps {
  menuOpen: boolean
  onOpenMenu: () => void
}

const pageTitles: Record<string, string> = {
  '/dashboard': 'Painel de estoque',
  '/products': 'Produtos',
  '/categories': 'Categorias',
}

function getInitials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()
}

export function AppHeader({ menuOpen, onOpenMenu }: AppHeaderProps) {
  const { pathname } = useLocation()
  const { user, logout } = useAuth()
  const title = pageTitles[pathname] ?? 'EstoqueHub'
  const firstName = user?.name.split(' ')[0] ?? ''

  return (
    <header className="flex min-h-24 items-center justify-between gap-4 border-b border-[#dce8df] bg-white/90 px-5 py-4 backdrop-blur md:px-8">
      <div className="flex min-w-0 items-center gap-3">
        <button
          type="button"
          className="grid size-10 shrink-0 place-items-center rounded-xl border border-[#d4e2d7] text-[#52645a] md:hidden"
          aria-label="Abrir menu"
          aria-expanded={menuOpen}
          aria-controls="mobile-navigation"
          onClick={onOpenMenu}
        >
          <Menu className="size-5" aria-hidden="true" />
        </button>
        <div className="min-w-0">
          <h1 className="truncate text-xl font-semibold text-[#26382d] md:text-2xl">
            {title}
          </h1>
          {pathname === '/dashboard' && (
            <p className="mt-1 hidden text-sm text-[#6e7f73] sm:block">
              Bom dia, {firstName}! Aqui está o resumo de hoje.
            </p>
          )}
        </div>
      </div>

      <div className="flex items-center gap-2">
        <div className="flex items-center gap-3 rounded-full border border-[#d4e2d7] bg-white py-1.5 pl-1.5 pr-3">
          <span className="grid size-9 place-items-center rounded-full bg-[#ccebd6] text-sm font-medium text-[#275d3a]">
            {getInitials(user?.name ?? '')}
          </span>
          <div className="hidden sm:block">
            <p className="text-sm font-medium text-[#26382d]">{user?.name}</p>
            <p className="text-xs text-[#738278]">{formatRole(user?.role ?? '')}</p>
          </div>
        </div>
        <button
          type="button"
          className="grid size-10 place-items-center rounded-full border border-[#d4e2d7] text-[#65766b] transition hover:bg-[#eef7f0] hover:text-[#26382d]"
          aria-label="Sair"
          onClick={logout}
        >
          <LogOut className="size-4" aria-hidden="true" />
        </button>
      </div>
    </header>
  )
}
