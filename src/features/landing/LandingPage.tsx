import { AlertTriangle, BarChart3, PackageSearch, Search } from 'lucide-react'
import { LandingFooter } from './LandingFooter'
import { LandingHeader } from './LandingHeader'
import { ProductPreview } from './ProductPreview'

const problems = [
  [
    'Planilhas não acompanham o ritmo',
    'Atualizações manuais deixam informações importantes espalhadas e atrasadas.',
  ],
  [
    'Falta de visibilidade custa caro',
    'Sem números claros, faltas e excessos de estoque só aparecem quando já viraram problema.',
  ],
  [
    'Reposição no improviso',
    'A ausência de alertas transforma decisões simples em urgências recorrentes.',
  ],
] as const

const features = [
  {
    title: 'Visão geral',
    description:
      'Indicadores de quantidade, produtos críticos e valor estimado em um painel direto.',
    icon: BarChart3,
  },
  {
    title: 'Catálogo organizado',
    description: 'Produtos, SKUs e quantidades reunidos para consulta rápida.',
    icon: PackageSearch,
  },
  {
    title: 'Alertas para agir',
    description:
      'Destaques para estoque baixo e produtos em falta antes que afetem a operação.',
    icon: AlertTriangle,
  },
  {
    title: 'Busca e filtros',
    description:
      'Encontre itens por nome ou SKU e filtre pela situação do estoque.',
    icon: Search,
  },
]

export function LandingPage() {
  return (
    <div className="min-h-screen bg-[#fbfdfb] text-[#18281e]">
      <LandingHeader />
      <main>
        <section
          id="inicio"
          className="overflow-hidden bg-[linear-gradient(rgba(34,120,65,0.045)_1px,transparent_1px),linear-gradient(90deg,rgba(34,120,65,0.045)_1px,transparent_1px)] bg-[size:56px_56px]"
        >
          <div className="mx-auto grid min-h-[720px] max-w-7xl items-center gap-16 px-5 py-20 lg:grid-cols-[0.82fr_1.18fr] lg:px-8">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#168f50]">
                Controle de estoque para pequenos comércios
              </p>
              <h1 className="mt-5 max-w-xl text-5xl font-bold leading-[1.04] tracking-[-0.045em] sm:text-6xl">
                Controle seu estoque sem perder tempo.
              </h1>
              <p className="mt-6 max-w-lg text-lg leading-8 text-[#617168]">
                Acompanhe produtos, quantidades e alertas em um painel simples
                para decidir com clareza e manter sua operação em movimento.
              </p>
              <div className="mt-8 flex flex-wrap gap-x-7 gap-y-3 text-sm text-[#52655a]">
                <span>✓ Visão clara do estoque</span>
                <span>✓ Alertas para agir antes</span>
              </div>
            </div>
            <div className="pb-14">
              <ProductPreview />
            </div>
          </div>
        </section>
        <section
          id="problema"
          className="scroll-mt-24 px-5 py-24 md:px-8"
        >
          <div className="mx-auto max-w-7xl">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#168f50]">
              Problema
            </p>
            <h2 className="mt-4 max-w-2xl text-4xl font-bold tracking-[-0.035em]">
              O estoque não pode depender de adivinhação
            </h2>
            <div className="mt-12 grid gap-5 md:grid-cols-3">
              {problems.map(([title, description], index) => (
                <article
                  className="rounded-[28px] border border-[#dce8df] bg-white p-7 shadow-sm"
                  key={title}
                >
                  <span className="text-sm font-semibold text-[#168f50]">
                    0{index + 1}
                  </span>
                  <h3 className="mt-8 text-xl font-semibold">{title}</h3>
                  <p className="mt-3 leading-7 text-[#68786f]">
                    {description}
                  </p>
                </article>
              ))}
            </div>
          </div>
        </section>
        <section
          id="funcionalidades"
          className="scroll-mt-24 bg-[#eef7f1] px-5 py-24 md:px-8"
        >
          <div className="mx-auto max-w-7xl">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#168f50]">
              Funcionalidades
            </p>
            <h2 className="mt-4 max-w-2xl text-4xl font-bold tracking-[-0.035em]">
              O essencial para decidir com clareza
            </h2>
            <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {features.map(({ title, description, icon: Icon }) => (
                <article className="rounded-[28px] bg-white p-7" key={title}>
                  <span className="grid size-12 place-items-center rounded-2xl bg-[#dcf2e3] text-[#168f50]">
                    <Icon aria-hidden="true" />
                  </span>
                  <h3 className="mt-6 text-xl font-semibold">{title}</h3>
                  <p className="mt-3 leading-7 text-[#68786f]">
                    {description}
                  </p>
                </article>
              ))}
            </div>
          </div>
        </section>
      </main>
      <LandingFooter />
    </div>
  )
}
