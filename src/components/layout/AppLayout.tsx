import { X } from 'lucide-react'
import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { AppHeader } from './AppHeader'
import { Sidebar } from './Sidebar'

export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className="min-h-screen bg-[#f2faf4] text-[#26382d]">
      <aside className="fixed inset-y-0 left-0 hidden w-[260px] border-r border-[#dce8df] md:block">
        <Sidebar />
      </aside>

      <div className="min-h-screen md:pl-[260px]">
        <AppHeader menuOpen={menuOpen} onOpenMenu={() => setMenuOpen(true)} />
        <Outlet />
      </div>

      {menuOpen && (
        <div className="fixed inset-0 z-50 md:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-[#15281b]/35"
            aria-label="Fechar menu pela sobreposição"
            onClick={() => setMenuOpen(false)}
          />
          <aside
            id="mobile-navigation"
            role="dialog"
            aria-label="Menu de navegação"
            aria-modal="true"
            className="relative h-full w-[min(82vw,300px)] border-r border-[#dce8df] bg-[#f7fcf8] shadow-2xl"
          >
            <button
              type="button"
              className="absolute right-4 top-4 z-10 grid size-9 place-items-center rounded-full border border-[#d4e2d7] bg-white text-[#52645a]"
              aria-label="Fechar menu"
              onClick={() => setMenuOpen(false)}
            >
              <X className="size-5" aria-hidden="true" />
            </button>
            <Sidebar onNavigate={() => setMenuOpen(false)} />
          </aside>
        </div>
      )}
    </div>
  )
}
