import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { BrandMark } from '../../components/ui/BrandMark'
import { SmoothAnchor } from './SmoothAnchor'

export function LandingHeader() {
  return (
    <header className="sticky top-0 z-50 border-b border-[#d9e5db] bg-[#fbfcf7]/90 backdrop-blur-xl">
      <div className="mx-auto flex min-h-20 max-w-7xl items-center justify-between gap-3 px-4 sm:gap-5 sm:px-5 md:px-8">
        <SmoothAnchor
          className="inline-flex min-h-11 items-center [&_p]:hidden min-[420px]:[&_p:first-child]:block lg:[&_p]:block"
          href="#inicio"
          aria-label="EstoqueHub — início"
        >
          <BrandMark />
        </SmoothAnchor>
        <nav
          className="hidden items-center gap-8 text-sm font-medium text-[#52655a] md:flex"
          aria-label="Navegação da página inicial"
        >
          <SmoothAnchor
            className="inline-flex min-h-11 items-center transition-colors hover:text-[#08743d]"
            href="#problema"
          >
            Problema
          </SmoothAnchor>
          <SmoothAnchor
            className="inline-flex min-h-11 items-center transition-colors hover:text-[#08743d]"
            href="#funcionalidades"
          >
            Funcionalidades
          </SmoothAnchor>
        </nav>
        <Link
          className="group inline-flex min-h-11 items-center gap-2 rounded-full bg-[#0e6538] px-4 text-xs font-semibold text-white shadow-[0_10px_28px_rgba(14,101,56,0.22)] transition duration-300 ease-out hover:-translate-y-0.5 hover:translate-x-0.5 hover:bg-[#084b2a] hover:shadow-[0_14px_34px_rgba(14,101,56,0.28)] active:translate-y-0 motion-reduce:transform-none motion-reduce:transition-none sm:px-5 sm:text-sm"
          to="/login"
        >
          Acesse a plataforma
          <ArrowRight
            className="size-4 transition-transform duration-300 group-hover:translate-x-1 motion-reduce:transform-none motion-reduce:transition-none"
            aria-hidden="true"
          />
        </Link>
      </div>
    </header>
  )
}
