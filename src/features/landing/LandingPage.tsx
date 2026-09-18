import {
  AlertTriangle,
  BarChart3,
  BellRing,
  Boxes,
  FileSpreadsheet,
  PackageSearch,
  Search,
  Sparkles,
} from 'lucide-react'
import { LandingFooter } from './LandingFooter'
import { LandingHeader } from './LandingHeader'
import { ProductPreview } from './ProductPreview'

const problems = [
  {
    title: 'Planilhas não acompanham o ritmo',
    description:
      'Entradas, saídas e correções ficam espalhadas — e o número certo nunca está à mão.',
    icon: FileSpreadsheet,
  },
  {
    title: 'Falta de visibilidade custa caro',
    description:
      'Produtos parados ocupam espaço enquanto os mais vendidos somem da prateleira.',
    icon: PackageSearch,
  },
  {
    title: 'Reposição vira urgência',
    description:
      'Sem alertas, a compra acontece tarde demais e a venda pode ir embora junto.',
    icon: AlertTriangle,
  },
] as const

const features = [
  {
    title: 'Um painel que fala a sua língua',
    description:
      'Veja produtos, quantidades e o que precisa da sua atenção sem decifrar relatórios.',
    icon: BarChart3,
    className: 'sm:col-span-2 lg:col-span-2',
    accent: true,
  },
  {
    title: 'Catálogo organizado',
    description: 'Nome, SKU e quantidade reunidos para encontrar tudo rápido.',
    icon: Boxes,
    className: '',
    accent: false,
  },
  {
    title: 'Alertas no tempo certo',
    description: 'Saiba o que está acabando antes de perder uma venda.',
    icon: BellRing,
    className: '',
    accent: false,
  },
  {
    title: 'Busca sem complicação',
    description: 'Encontre qualquer item por nome, SKU ou situação do estoque.',
    icon: Search,
    className: 'sm:col-span-2 lg:col-span-4',
    accent: false,
  },
] as const

export function LandingPage() {
  return (
    <div className="min-h-screen overflow-x-hidden bg-[#f8faf4] text-[#142c1d]">
      <LandingHeader />
      <main>
        <section id="inicio" className="relative scroll-mt-24 bg-[#f8faf4]">
          <div className="pointer-events-none absolute left-[-12rem] top-[-15rem] size-[34rem] rounded-full border-[80px] border-[#78e39a]/10" />
          <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(rgba(11,103,55,0.035)_1px,transparent_1px),linear-gradient(90deg,rgba(11,103,55,0.035)_1px,transparent_1px)] bg-[size:64px_64px] [mask-image:linear-gradient(to_right,black,transparent_78%)]" />

          <div className="relative mx-auto grid min-h-[730px] max-w-7xl items-center gap-20 px-5 pb-28 pt-20 lg:grid-cols-[0.78fr_1.22fr] lg:px-8 lg:pb-32 lg:pt-24">
            <div className="relative z-10">
              <div className="inline-flex items-center gap-2 rounded-full border border-[#b9dcc4] bg-white px-3 py-2 text-xs font-bold text-[#0e6b3b] shadow-sm">
                <Sparkles className="size-3.5" aria-hidden="true" />
                Feito para o pequeno comércio
              </div>
              <h1
                className="mt-6 max-w-xl text-[3.25rem] font-black leading-[0.98] tracking-[-0.055em] sm:text-6xl lg:text-[4.3rem]"
                aria-label="Seu estoque no ritmo do seu negócio."
              >
                Seu estoque,
                <span className="mt-1 block text-[#0e7842]">no ritmo do seu negócio.</span>
              </h1>
              <p className="mt-7 max-w-lg text-lg leading-8 text-[#5a6d60]">
                Menos tempo conferindo planilha. Mais clareza para comprar,
                vender e manter cada produto no lugar certo.
              </p>
              <div className="mt-9 flex flex-wrap items-center gap-4 text-sm font-semibold text-[#3e5948]">
                <span className="inline-flex items-center gap-2">
                  <span className="grid size-6 place-items-center rounded-full bg-[#d7f4df] text-[#0d7841]">✓</span>
                  Fácil de acompanhar
                </span>
                <span className="inline-flex items-center gap-2">
                  <span className="grid size-6 place-items-center rounded-full bg-[#d7f4df] text-[#0d7841]">✓</span>
                  Pronto para agir
                </span>
              </div>
            </div>

            <div className="relative z-10 pb-14 lg:translate-x-4">
              <ProductPreview />
            </div>
          </div>

          <div className="absolute bottom-0 left-1/2 flex w-[min(92%,1180px)] -translate-x-1/2 translate-y-1/2 items-center justify-around gap-4 rounded-[26px] border border-[#ccddcf] bg-white px-5 py-5 shadow-[0_18px_50px_rgba(30,71,44,0.10)] sm:px-8">
            <p className="text-center text-xs font-semibold text-[#53665a] sm:text-sm">
              <strong className="block text-xl text-[#113d25] sm:inline sm:text-2xl">Estoque claro</strong>{' '}
              para comprar melhor
            </p>
            <span className="h-10 w-px bg-[#dbe5dc]" />
            <p className="text-center text-xs font-semibold text-[#53665a] sm:text-sm">
              <strong className="block text-xl text-[#113d25] sm:inline sm:text-2xl">Alertas úteis</strong>{' '}
              para agir antes
            </p>
            <span className="hidden h-10 w-px bg-[#dbe5dc] sm:block" />
            <p className="hidden text-center text-sm font-semibold text-[#53665a] sm:block">
              <strong className="text-2xl text-[#113d25]">Tudo junto</strong>{' '}
              em um só lugar
            </p>
          </div>
        </section>

        <section id="problema" className="scroll-mt-20 px-5 pb-28 pt-40 md:px-8">
          <div className="mx-auto grid max-w-7xl gap-12 lg:grid-cols-[0.72fr_1.28fr] lg:gap-20">
            <div className="lg:sticky lg:top-32 lg:self-start">
              <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#0e7842]">O problema</p>
              <h2 className="mt-5 max-w-lg text-4xl font-black leading-tight tracking-[-0.045em] sm:text-5xl">
                O estoque não pode depender de adivinhação
              </h2>
              <p className="mt-6 max-w-md text-lg leading-8 text-[#637368]">
                Quando a informação chega atrasada, uma tarefa simples vira
                retrabalho — e o dia já é corrido demais para isso.
              </p>
              <div className="mt-9 inline-flex rotate-[-2deg] rounded-2xl bg-[#caff6a] px-5 py-4 text-sm font-black text-[#193b26] shadow-[6px_6px_0_#17482c]">
                Chega de “acho que ainda tem”.
              </div>
            </div>

            <div className="space-y-5">
              {problems.map(({ title, description, icon: Icon }, index) => (
                <article
                  className={`group grid gap-5 rounded-[30px] border border-[#d7e3d8] bg-white p-6 transition duration-300 hover:-translate-y-1 hover:border-[#8ac79c] hover:shadow-[0_22px_50px_rgba(28,76,44,0.10)] motion-reduce:transform-none motion-reduce:transition-none sm:grid-cols-[auto_1fr_auto] sm:items-center sm:p-8 ${
                    index === 1 ? 'lg:translate-x-8' : ''
                  }`}
                  key={title}
                >
                  <span className="grid size-14 place-items-center rounded-2xl bg-[#e2f6e8] text-[#0e7842] transition-colors group-hover:bg-[#0e7842] group-hover:text-white">
                    <Icon aria-hidden="true" />
                  </span>
                  <div>
                    <h3 className="text-xl font-bold tracking-[-0.025em]">{title}</h3>
                    <p className="mt-2 leading-7 text-[#68796e]">{description}</p>
                  </div>
                  <span className="text-4xl font-black text-[#d9e7dc]">0{index + 1}</span>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section
          id="funcionalidades"
          className="relative scroll-mt-20 overflow-hidden bg-[#0b2f1d] px-5 py-28 text-white md:px-8"
        >
          <div className="pointer-events-none absolute -right-28 -top-40 size-[34rem] rounded-full border-[82px] border-[#74e498]/8" />
          <div className="pointer-events-none absolute bottom-0 left-0 h-2/3 w-1/2 bg-[radial-gradient(circle_at_bottom_left,rgba(202,255,106,0.09),transparent_65%)]" />

          <div className="relative mx-auto max-w-7xl">
            <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr] lg:items-end">
              <div>
                <p className="text-sm font-bold uppercase tracking-[0.2em] text-[#91efa9]">Funcionalidades</p>
                <h2 className="mt-5 max-w-2xl text-4xl font-black leading-tight tracking-[-0.045em] sm:text-5xl">
                  O essencial para decidir com clareza
                </h2>
              </div>
              <p className="max-w-lg text-lg leading-8 text-white/62 lg:justify-self-end">
                A EstoqueHub organiza o que importa e deixa os próximos passos
                visíveis — sem excesso de telas ou informação.
              </p>
            </div>

            <div className="mt-14 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {features.map(({ title, description, icon: Icon, className, accent }) => (
                <article
                  className={`group rounded-[30px] border p-7 transition duration-300 hover:-translate-y-1 motion-reduce:transform-none motion-reduce:transition-none ${className} ${
                    accent
                      ? 'border-[#caff6a] bg-[#caff6a] text-[#14331f]'
                      : 'border-white/12 bg-white/[0.07] hover:border-[#84e99f]/45 hover:bg-white/[0.10]'
                  }`}
                  key={title}
                >
                  <span
                    className={`grid size-12 place-items-center rounded-2xl ${
                      accent
                        ? 'bg-[#14331f] text-[#caff6a]'
                        : 'bg-[#8cf0a7]/12 text-[#91efa9]'
                    }`}
                  >
                    <Icon aria-hidden="true" />
                  </span>
                  <h3 className="mt-8 text-2xl font-bold tracking-[-0.03em]">{title}</h3>
                  <p className={`mt-3 max-w-xl leading-7 ${accent ? 'text-[#355441]' : 'text-white/62'}`}>
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
