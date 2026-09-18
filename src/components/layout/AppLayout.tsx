import { X } from 'lucide-react'
import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type KeyboardEvent,
} from 'react'
import { Outlet } from 'react-router-dom'
import { AppHeader } from './AppHeader'
import { Sidebar } from './Sidebar'

export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const dialogRef = useRef<HTMLElement>(null)
  const menuTriggerRef = useRef<HTMLElement | null>(null)

  const openMenu = useCallback(() => {
    if (document.activeElement instanceof HTMLElement) {
      menuTriggerRef.current = document.activeElement
    }
    setMenuOpen(true)
  }, [])

  const closeMenu = useCallback(() => setMenuOpen(false), [])

  useEffect(() => {
    if (!menuOpen) return

    const dialog = dialogRef.current
    const firstControl = dialog?.querySelector<HTMLElement>('button, a[href]')
    firstControl?.focus()

    return () => menuTriggerRef.current?.focus()
  }, [menuOpen])

  const handleDialogKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    if (event.key === 'Escape') {
      event.preventDefault()
      closeMenu()
      return
    }

    if (event.key !== 'Tab') return

    const controls = dialogRef.current?.querySelectorAll<HTMLElement>(
      'button:not([disabled]), a[href]',
    )
    if (!controls?.length) return

    const firstControl = controls[0]
    const lastControl = controls[controls.length - 1]

    if (event.shiftKey && document.activeElement === firstControl) {
      event.preventDefault()
      lastControl.focus()
    } else if (!event.shiftKey && document.activeElement === lastControl) {
      event.preventDefault()
      firstControl.focus()
    }
  }

  return (
    <div className="min-h-screen bg-[#f2faf4] text-[#26382d]">
      <aside className="fixed inset-y-0 left-0 hidden w-[260px] border-r border-[#dce8df] md:block">
        <Sidebar />
      </aside>

      <div className="min-h-screen md:pl-[260px]" inert={menuOpen}>
        <AppHeader menuOpen={menuOpen} onOpenMenu={openMenu} />
        <Outlet />
      </div>

      {menuOpen && (
        <div className="fixed inset-0 z-50 md:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-[#15281b]/35"
            aria-label="Fechar menu pela sobreposição"
            onClick={closeMenu}
          />
          <aside
            ref={dialogRef}
            id="mobile-navigation"
            role="dialog"
            aria-label="Menu de navegação"
            aria-modal="true"
            className="relative h-full w-[min(82vw,300px)] border-r border-[#dce8df] bg-[#f7fcf8] shadow-2xl"
            onKeyDown={handleDialogKeyDown}
          >
            <button
              type="button"
              className="absolute right-4 top-4 z-10 grid size-9 place-items-center rounded-full border border-[#d4e2d7] bg-white text-[#52645a]"
              aria-label="Fechar menu"
              onClick={closeMenu}
            >
              <X className="size-5" aria-hidden="true" />
            </button>
            <Sidebar onNavigate={closeMenu} />
          </aside>
        </div>
      )}
    </div>
  )
}
