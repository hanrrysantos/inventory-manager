import { Link } from 'react-router-dom'
import { BrandMark } from '../../components/ui/BrandMark'

export function LandingHeader() {
  return (
    <header className="sticky top-0 z-50 border-b border-[#dfeae2] bg-white/90 backdrop-blur-xl">
      <div className="mx-auto flex min-h-20 max-w-7xl items-center justify-between gap-5 px-5 md:px-8">
        <a
          className="inline-flex min-h-11 items-center"
          href="#inicio"
          aria-label="EstoqueHub — início"
        >
          <BrandMark />
        </a>
        <nav
          className="hidden items-center gap-8 text-sm font-medium text-[#52655a] md:flex"
          aria-label="Navegação da página inicial"
        >
          <a
            className="inline-flex min-h-11 items-center transition hover:text-[#107842]"
            href="#problema"
          >
            Problema
          </a>
          <a
            className="inline-flex min-h-11 items-center transition hover:text-[#107842]"
            href="#funcionalidades"
          >
            Funcionalidades
          </a>
        </nav>
        <Link
          className="inline-flex min-h-11 items-center rounded-full bg-[#107842] px-5 text-sm font-semibold text-white shadow-lg shadow-green-900/10 transition hover:bg-[#0b6336]"
          to="/login"
        >
          Acesse a plataforma
        </Link>
      </div>
    </header>
  )
}
