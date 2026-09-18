import { Leaf } from 'lucide-react'

export function BrandMark() {
  return (
    <div className="flex items-center gap-3">
      <span className="grid size-10 place-items-center rounded-full bg-[#58b978] text-white">
        <Leaf className="size-5" aria-hidden="true" />
      </span>
      <div>
        <p className="font-semibold text-[#26382d]">Verdejar</p>
        <p className="text-xs text-[#6e7f73]">Controle de estoque</p>
      </div>
    </div>
  )
}
