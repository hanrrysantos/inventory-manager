import {
  CircleX,
  LayoutDashboard,
  LogOut,
  Package,
  PackageCheck,
  TrendingUp,
  TriangleAlert,
} from 'lucide-react'

const metrics = [
  {
    label: 'Total de itens',
    value: '462',
    support: 'em 13 produtos',
    icon: PackageCheck,
    iconStyle: 'bg-[#e5f4e9] text-[#54b873]',
  },
  {
    label: 'Estoque baixo',
    value: '4',
    support: 'abaixo do mínimo',
    icon: TriangleAlert,
    iconStyle: 'bg-[#f8f2da] text-[#e7a45d]',
  },
  {
    label: 'Em falta',
    value: '0',
    support: 'reposição urgente',
    icon: CircleX,
    iconStyle: 'bg-[#fae3e3] text-[#e85d5d]',
  },
  {
    label: 'Valor em estoque',
    value: 'R$ 3.534',
    support: 'valor atual estimado',
    icon: TrendingUp,
    iconStyle: 'bg-[#def3f5] text-[#2cabb7]',
  },
] as const

const products = [
  {
    name: 'Arroz Agulhinha 5kg',
    sku: 'ARR-001',
    category: 'Mercearia Seca',
    quantity: 40,
    status: 'Em estoque',
    lowStock: false,
  },
  {
    name: 'Feijão Carioca 1kg',
    sku: 'FEI-001',
    category: 'Mercearia Seca',
    quantity: 12,
    status: 'Estoque baixo',
    lowStock: true,
  },
  {
    name: 'Macarrão Espaguete 500g',
    sku: 'MAC-001',
    category: 'Mercearia Seca',
    quantity: 21,
    status: 'Em estoque',
    lowStock: false,
  },
  {
    name: 'Leite Integral UHT 1L',
    sku: 'LEI-001',
    category: 'Laticínios',
    quantity: 150,
    status: 'Em estoque',
    lowStock: false,
  },
] as const

const attentionItems = [
  { name: 'Detergente Neutro 500ml', sku: 'DET-001', stock: '15/20', width: '75%' },
  { name: 'Feijão Carioca 1kg', sku: 'FEI-001', stock: '12/15', width: '80%' },
  { name: 'Suco de Laranja 1L', sku: 'SUC-001', stock: '2/10', width: '20%' },
] as const

export function ProductPreview() {
  return (
    <div
      className="relative isolate mx-auto w-full max-w-[800px]"
      aria-hidden="true"
      data-testid="dashboard-preview"
    >
      <div className="absolute -inset-x-10 -inset-y-8 -z-10 rounded-[46%_54%_24%_30%] bg-[#d9f3df]" />
      <div className="absolute -right-5 top-4 -z-10 size-24 rotate-12 rounded-[28px] bg-[#caff6a]" />

      <div className="overflow-hidden rounded-[24px] border-[7px] border-[#143424] bg-[#f2faf4] shadow-[0_38px_100px_rgba(5,50,28,0.25)] sm:rounded-[30px] sm:border-[9px]">
        <div className="flex h-10 items-center gap-2 border-b border-[#dce8df] bg-white px-4">
          <span className="size-2.5 rounded-full bg-[#ff7a6b]" />
          <span className="size-2.5 rounded-full bg-[#f3ca58]" />
          <span className="size-2.5 rounded-full bg-[#62cb82]" />
          <span className="ml-3 text-[10px] font-bold text-[#23402e]">EstoqueHub</span>
        </div>

        <div className="grid min-h-[420px] grid-cols-[58px_1fr] sm:grid-cols-[116px_1fr]">
          <aside className="border-r border-[#dce8df] bg-[#f7fcf8] p-2.5 sm:p-3">
            <div className="flex items-center gap-2 px-1 sm:px-1.5">
              <span className="grid size-8 shrink-0 place-items-center rounded-xl bg-[#d9f4e1] text-[#168d4f]">
                <PackageCheck className="size-4" />
              </span>
              <span className="hidden text-[9px] font-bold text-[#22412d] sm:inline">EstoqueHub</span>
            </div>

            <div className="mt-7 space-y-2">
              <div className="flex items-center gap-2 rounded-xl bg-[#5cbd79] p-2 text-[9px] font-semibold text-white">
                <LayoutDashboard className="size-3.5 shrink-0" />
                <span className="hidden sm:inline">Painel</span>
              </div>
              <div className="flex items-center gap-2 rounded-xl p-2 text-[9px] text-[#65776c]">
                <Package className="size-3.5 shrink-0" />
                <span className="hidden sm:inline">Produtos</span>
              </div>
            </div>
          </aside>

          <div className="min-w-0">
            <header className="flex h-[58px] items-center justify-between border-b border-[#dce8df] bg-white px-3 sm:px-5">
              <div>
                <p className="text-[11px] font-bold text-[#263a2d] sm:text-sm">Painel de estoque</p>
                <p className="mt-0.5 hidden text-[8px] text-[#718177] sm:block">Bom dia, user1! Aqui está o resumo de hoje.</p>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="grid size-7 place-items-center rounded-full bg-[#d7f0df] text-[9px] font-bold text-[#287044]">U</span>
                <LogOut className="size-3.5 text-[#718177]" />
              </div>
            </header>

            <div className="p-3 sm:p-4">
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                {metrics.map(({ label, value, support, icon: Icon, iconStyle }) => (
                  <div className="rounded-xl border border-[#d9e5dc] bg-white p-2.5 shadow-sm" key={label}>
                    <div className="flex items-start justify-between gap-1">
                      <div className="min-w-0">
                        <p className="truncate text-[8px] text-[#718177]">{label}</p>
                        <p className="mt-1 truncate text-sm font-bold text-[#2b3e31] sm:text-base">{value}</p>
                      </div>
                      <span className={`hidden size-7 shrink-0 place-items-center rounded-lg sm:grid ${iconStyle}`}>
                        <Icon className="size-3.5" />
                      </span>
                    </div>
                    <p className="mt-2 hidden truncate text-[7px] text-[#7a8980] sm:block">{support}</p>
                  </div>
                ))}
              </div>

              <div className="mt-3 grid gap-3 min-[560px]:grid-cols-[1.65fr_0.85fr]">
                <section className="overflow-hidden rounded-xl border border-[#d9e5dc] bg-white shadow-sm">
                  <div className="border-b border-[#e1ebe4] px-3 py-2.5">
                    <p className="text-[10px] font-bold text-[#2d4033]">Produtos</p>
                    <p className="mt-0.5 text-[7px] text-[#77877c]">Gerencie os itens do seu estoque</p>
                  </div>
                  <table className="w-full table-fixed text-left">
                    <thead className="bg-[#f4f9f5] text-[6px] uppercase text-[#6d7d73]">
                      <tr>
                        <th className="w-[44%] px-3 py-2 font-medium">Produto</th>
                        <th className="hidden w-[28%] px-2 py-2 font-medium sm:table-cell">Categoria</th>
                        <th className="w-[16%] px-2 py-2 font-medium">Qtd.</th>
                        <th className="px-2 py-2 font-medium">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#e7eee8]">
                      {products.map((product) => (
                        <tr key={product.sku}>
                          <td className="px-3 py-2">
                            <p className="truncate text-[8px] font-semibold text-[#314337]">{product.name}</p>
                            <p className="text-[6px] text-[#7c8b81]">{product.sku}</p>
                          </td>
                          <td className="hidden truncate px-2 py-2 text-[7px] text-[#6d7d73] sm:table-cell">{product.category}</td>
                          <td className="px-2 py-2 text-[8px] font-semibold text-[#314337]">{product.quantity}</td>
                          <td className="px-2 py-2">
                            <span
                              className={`inline-block whitespace-nowrap rounded-full px-1.5 py-1 text-[6px] font-semibold ${
                                product.lowStock
                                  ? 'bg-[#fbf0d5] text-[#bd7a20]'
                                  : 'bg-[#e3f5e8] text-[#279557]'
                              }`}
                            >
                              {product.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </section>

                <section className="hidden rounded-xl border border-[#d9e5dc] bg-white p-3 shadow-sm min-[560px]:block">
                  <div className="flex items-center gap-2">
                    <span className="grid size-7 place-items-center rounded-full bg-[#fbf1d8] text-[#e5a146]">
                      <TriangleAlert className="size-3.5" />
                    </span>
                    <div>
                      <p className="text-[9px] font-bold text-[#304236]">Precisam de atenção</p>
                      <p className="text-[7px] text-[#7b8a80]">Itens para repor em breve</p>
                    </div>
                  </div>

                  <div className="mt-3 space-y-2">
                    {attentionItems.map((item) => (
                      <div className="rounded-lg border border-[#dfe9e1] bg-[#fbfdfb] p-2" key={item.sku}>
                        <div className="flex items-start justify-between gap-2">
                          <div className="min-w-0">
                            <p className="truncate text-[7px] font-semibold text-[#304236]">{item.name}</p>
                            <p className="text-[6px] text-[#819087]">{item.sku}</p>
                          </div>
                          <span className="text-[6px] text-[#6e7e74]">{item.stock}</span>
                        </div>
                        <div className="mt-2 h-1 overflow-hidden rounded-full bg-[#e5eee7]">
                          <div className="h-full rounded-full bg-[#dfc553]" style={{ width: item.width }} />
                        </div>
                      </div>
                    ))}
                  </div>
                </section>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
