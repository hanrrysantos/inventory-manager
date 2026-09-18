import { BrandMark } from '../../components/ui/BrandMark'

export function LandingFooter() {
  return (
    <footer className="bg-[#0e1b13] text-white">
      <div className="mx-auto grid max-w-7xl gap-10 px-5 py-14 md:grid-cols-[1.5fr_1fr] md:px-8">
        <div className="max-w-sm">
          <BrandMark inverse />
          <p className="mt-5 text-sm leading-7 text-white/60">
            Controle simples para pequenos comércios acompanharem produtos,
            quantidades e alertas em um só lugar.
          </p>
        </div>
        <nav className="md:justify-self-end" aria-label="Atalhos do rodapé">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-white/70">
            Navegação
          </p>
          <div className="mt-4 flex flex-col gap-3 text-sm text-white/70">
            <a className="inline-flex min-h-11 items-center" href="#problema">
              Problema
            </a>
            <a
              className="inline-flex min-h-11 items-center"
              href="#funcionalidades"
            >
              Funcionalidades
            </a>
          </div>
        </nav>
      </div>
      <div className="mx-auto max-w-7xl border-t border-white/10 px-5 py-6 text-xs text-white/70 md:px-8">
        © 2026 EstoqueHub. Todos os direitos reservados.
      </div>
    </footer>
  )
}
