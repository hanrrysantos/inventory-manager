import logoUrl from '../../assets/estoquehub-logo.png'
import { cn } from '../../lib/cn'

interface BrandMarkProps {
  inverse?: boolean
}

export function BrandMark({ inverse = false }: BrandMarkProps) {
  return (
    <div className="flex items-center gap-3">
      <img className="size-11 object-contain" src={logoUrl} alt="" />
      <div>
        <p
          className={cn('font-semibold tracking-tight text-[#173b27]', inverse && 'text-white')}
          data-testid="brand-wordmark"
        >
          EstoqueHub
        </p>
        <p className={cn('text-xs text-[#66796d]', inverse && 'text-white/65')}>
          Seu estoque, sempre sob controle
        </p>
      </div>
    </div>
  )
}
