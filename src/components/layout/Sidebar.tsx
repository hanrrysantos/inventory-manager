import { LayoutDashboard, Package } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '../../lib/cn'
import { BrandMark } from '../ui/BrandMark'

interface SidebarProps {
  onNavigate?: () => void
}

const links = [
  { to: '/dashboard', label: 'Painel', icon: LayoutDashboard },
  { to: '/products', label: 'Produtos', icon: Package },
]

export function Sidebar({ onNavigate }: SidebarProps) {
  return (
    <div className="flex h-full flex-col bg-[#f7fcf8] px-5 py-7">
      <div className="px-2">
        <BrandMark />
      </div>

      <nav className="mt-10" aria-label="Navegação principal">
        <ul className="space-y-2">
          {links.map(({ to, label, icon: Icon }) => (
            <li key={to}>
              <NavLink
                to={to}
                onClick={onNavigate}
                className={({ isActive }) =>
                  cn(
                    'flex h-11 items-center gap-3 rounded-2xl px-4 text-sm font-medium transition',
                    isActive
                      ? 'bg-[#5cbd79] text-white shadow-sm'
                      : 'text-[#627268] hover:bg-[#eaf5ed] hover:text-[#26382d]',
                  )
                }
              >
                <Icon className="size-5" aria-hidden="true" />
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}
