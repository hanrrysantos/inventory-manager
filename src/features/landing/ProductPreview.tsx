import { AlertTriangle, PackageCheck, TrendingUp } from 'lucide-react'

const metrics = [
  { label: 'Total de itens', value: '1.284', icon: PackageCheck },
  { label: 'Estoque baixo', value: '12', icon: AlertTriangle },
  { label: 'Valor em estoque', value: 'R$ 48 mil', icon: TrendingUp },
]

export function ProductPreview() {
  return (
    <div
      className="relative mx-auto w-full max-w-[680px]"
      aria-hidden="true"
    >
      <div className="overflow-hidden rounded-[28px] border-[10px] border-[#17251d] bg-[#f4faf6] shadow-[0_32px_90px_rgba(24,88,50,0.20)]">
        <div className="flex h-11 items-center gap-2 border-b border-[#dce8df] bg-white px-5">
          <span className="size-2.5 rounded-full bg-[#ff766f]" />
          <span className="size-2.5 rounded-full bg-[#f5c95c]" />
          <span className="size-2.5 rounded-full bg-[#4dc879]" />
          <span className="ml-3 text-xs font-semibold text-[#31523d]">
            EstoqueHub
          </span>
        </div>
        <div className="p-5 sm:p-7">
          <p className="text-xs font-medium uppercase tracking-[0.18em] text-[#6b7d71]">
            Painel de estoque
          </p>
          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            {metrics.map(({ label, value, icon: Icon }) => (
              <div
                className="rounded-2xl border border-[#dce8df] bg-white p-4"
                key={label}
              >
                <Icon className="size-5 text-[#1b9a58]" />
                <p className="mt-3 text-xs text-[#6b7d71]">{label}</p>
                <p className="mt-1 text-lg font-semibold text-[#203127]">
                  {value}
                </p>
              </div>
            ))}
          </div>
          <div className="mt-4 rounded-2xl border border-[#dce8df] bg-white p-5">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold">
                Produtos que precisam de atenção
              </span>
              <span className="text-xs text-[#168f50]">Ver catálogo</span>
            </div>
            <div className="mt-5 space-y-3">
              <div className="h-3 w-full rounded-full bg-[#eef5f0]" />
              <div className="h-3 w-4/5 rounded-full bg-[#d9eee0]" />
              <div className="h-3 w-3/5 rounded-full bg-[#bce3c9]" />
            </div>
          </div>
        </div>
      </div>
      <div className="absolute -bottom-12 -left-3 w-[180px] rounded-[30px] border-[8px] border-[#17251d] bg-white p-4 shadow-[0_22px_55px_rgba(22,66,38,0.24)] sm:-left-10 sm:w-[210px]">
        <div className="mx-auto mb-4 h-1.5 w-14 rounded-full bg-[#17251d]" />
        <p className="text-xs text-[#6b7d71]">Estoque baixo</p>
        <p className="mt-1 text-3xl font-semibold text-[#203127]">12</p>
        <div className="mt-5 h-24 rounded-2xl bg-[linear-gradient(135deg,#e6f6eb,#bde7ca)] p-3">
          <div className="mt-8 flex items-end gap-2">
            <span className="h-5 w-5 rounded bg-[#74cd8f]" />
            <span className="h-9 w-5 rounded bg-[#45b96b]" />
            <span className="h-14 w-5 rounded bg-[#168f50]" />
          </div>
        </div>
      </div>
    </div>
  )
}
