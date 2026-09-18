import {
  AlertTriangle,
  BarChart3,
  CircleCheckBig,
  PackageCheck,
  Search,
} from 'lucide-react'

const products = [
  {
    name: 'Café especial 500g',
    sku: 'CAF-042',
    quantity: '4 un.',
    status: 'Repor hoje',
    critical: true,
  },
  {
    name: 'Leite integral',
    sku: 'LEI-018',
    quantity: '38 un.',
    status: 'Em dia',
    critical: false,
  },
  {
    name: 'Copo térmico',
    sku: 'COP-107',
    quantity: '21 un.',
    status: 'Em dia',
    critical: false,
  },
]

export function ProductPreview() {
  return (
    <div className="relative isolate mx-auto w-full max-w-[720px]" aria-hidden="true">
      <div className="absolute -inset-10 -z-10 rounded-[50%_50%_18%_18%] border-[42px] border-[#75e49a]/18" />
      <div className="absolute -right-3 top-4 -z-10 size-24 rotate-12 rounded-[28px] bg-[#caff6a] sm:-right-7" />

      <div className="overflow-hidden rounded-[26px] border-[8px] border-[#102d1e] bg-[#f5f8f2] shadow-[0_38px_100px_rgba(5,50,28,0.28)] sm:rounded-[34px] sm:border-[10px]">
        <div className="flex h-11 items-center gap-2 border-b border-[#dce7dc] bg-white px-4 sm:px-5">
          <span className="size-2.5 rounded-full bg-[#ff7a6b]" />
          <span className="size-2.5 rounded-full bg-[#f3ca58]" />
          <span className="size-2.5 rounded-full bg-[#62cb82]" />
          <span className="ml-3 text-[11px] font-bold tracking-[-0.02em] text-[#1a3826]">
            EstoqueHub
          </span>
        </div>

        <div className="grid min-h-[390px] grid-cols-[68px_1fr] sm:grid-cols-[128px_1fr]">
          <aside className="bg-[#0d5531] p-3 text-white sm:p-4">
            <div className="grid size-10 place-items-center rounded-xl bg-[#caff6a] text-[#103320]">
              <PackageCheck className="size-5" />
            </div>
            <div className="mt-8 space-y-3">
              <div className="flex items-center gap-2 rounded-xl bg-white/12 p-2 text-[10px] font-semibold">
                <BarChart3 className="size-4 shrink-0" />
                <span className="hidden sm:inline">Visão geral</span>
              </div>
              <div className="flex items-center gap-2 p-2 text-[10px] text-white/60">
                <PackageCheck className="size-4 shrink-0" />
                <span className="hidden sm:inline">Produtos</span>
              </div>
              <div className="flex items-center gap-2 p-2 text-[10px] text-white/60">
                <AlertTriangle className="size-4 shrink-0" />
                <span className="hidden sm:inline">Alertas</span>
              </div>
            </div>
          </aside>

          <div className="min-w-0 p-4 sm:p-6">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.18em] text-[#11834b]">
                  Visão geral
                </p>
                <p className="mt-1 text-lg font-bold tracking-[-0.03em] text-[#142e1e] sm:text-2xl">
                  Bom dia, Ana.
                </p>
              </div>
              <span className="grid size-9 place-items-center rounded-full border border-[#d9e6db] bg-white text-[#52705d]">
                <Search className="size-4" />
              </span>
            </div>

            <div className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-3">
              <div className="rounded-2xl bg-[#103d27] p-3 text-white sm:p-4">
                <p className="text-[9px] text-white/60 sm:text-[10px]">Produtos</p>
                <p className="mt-1 text-xl font-bold sm:text-2xl">128</p>
              </div>
              <div className="rounded-2xl bg-[#caff6a] p-3 text-[#173322] sm:p-4">
                <p className="text-[9px] text-[#34573f] sm:text-[10px]">Estoque baixo</p>
                <p className="mt-1 text-xl font-bold sm:text-2xl">12</p>
              </div>
              <div className="hidden rounded-2xl border border-[#dce7dc] bg-white p-4 sm:block">
                <p className="text-[10px] text-[#65776b]">Valor em estoque</p>
                <p className="mt-1 text-2xl font-bold text-[#173322]">R$ 48 mil</p>
              </div>
            </div>

            <div className="mt-4 overflow-hidden rounded-2xl border border-[#dce7dc] bg-white">
              <div className="flex items-center justify-between border-b border-[#e6eee7] px-3 py-3 sm:px-4">
                <p className="text-[11px] font-bold text-[#173322] sm:text-xs">Produtos em destaque</p>
                <span className="text-[9px] font-semibold text-[#11834b] sm:text-[10px]">Ver todos</span>
              </div>
              {products.map((product) => (
                <div
                  className="grid grid-cols-[1fr_auto] items-center gap-3 border-b border-[#edf2ed] px-3 py-2.5 last:border-0 sm:px-4"
                  key={product.sku}
                >
                  <div className="min-w-0">
                    <p className="truncate text-[10px] font-semibold text-[#203528] sm:text-[11px]">{product.name}</p>
                    <p className="mt-0.5 text-[8px] text-[#829087] sm:text-[9px]">
                      {product.sku} · {product.quantity}
                    </p>
                  </div>
                  <span
                    className={`rounded-full px-2 py-1 text-[8px] font-bold sm:text-[9px] ${
                      product.critical
                        ? 'bg-[#fff0d8] text-[#a85a09]'
                        : 'bg-[#e8f7ed] text-[#137542]'
                    }`}
                  >
                    {product.status}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="absolute -bottom-12 -left-2 w-[148px] rounded-[28px] border-[7px] border-[#102d1e] bg-[#f8fbf5] p-3 shadow-[0_22px_55px_rgba(7,53,30,0.28)] sm:-left-10 sm:w-[188px] sm:border-[8px] sm:p-4">
        <div className="mx-auto mb-4 h-1.5 w-12 rounded-full bg-[#102d1e]" />
        <div className="rounded-2xl bg-[#0e6538] p-3 text-white">
          <CircleCheckBig className="size-5 text-[#caff6a]" />
          <p className="mt-7 text-[9px] text-white/65 sm:text-[10px]">Status do estoque</p>
          <p className="mt-1 text-xs font-bold sm:text-sm">Tudo sob controle</p>
        </div>
        <div className="mt-3 flex items-center gap-2 rounded-xl bg-[#fff0d8] p-2 text-[#8c4e0d]">
          <AlertTriangle className="size-3.5 shrink-0" />
          <span className="text-[8px] font-semibold sm:text-[9px]">2 alertas novos</span>
        </div>
      </div>
    </div>
  )
}
