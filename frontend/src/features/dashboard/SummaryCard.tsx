import type { LucideIcon } from 'lucide-react'
import { cn } from '../../lib/cn'

interface SummaryCardProps {
  title: string
  value: string | number
  support: string
  icon: LucideIcon
  tone: 'success' | 'warning' | 'danger' | 'info'
}

const toneStyles = {
  success: 'bg-[#e5f4e9] text-[#54b873]',
  warning: 'bg-[#f8f2da] text-[#e7a45d]',
  danger: 'bg-[#fae3e3] text-[#e85d5d]',
  info: 'bg-[#def3f5] text-[#2cabb7]',
}

export function SummaryCard({
  title,
  value,
  support,
  icon: Icon,
  tone,
}: SummaryCardProps) {
  return (
    <article className="flex min-h-36 justify-between rounded-[24px] border border-[#d8e5dc] bg-white p-5 shadow-[0_3px_10px_rgba(45,82,56,0.08)]">
      <div>
        <p className="text-sm text-[#6c7d72]">{title}</p>
        <p className="mt-2 text-3xl font-semibold tracking-tight text-[#23352a]">
          {value}
        </p>
        <p className="mt-4 text-xs text-[#708177]">{support}</p>
      </div>
      <span
        className={cn('grid size-11 shrink-0 place-items-center rounded-2xl', toneStyles[tone])}
      >
        <Icon className="size-5" aria-hidden="true" />
      </span>
    </article>
  )
}
