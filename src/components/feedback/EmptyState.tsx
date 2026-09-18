import { PackageOpen } from 'lucide-react'

interface EmptyStateProps {
  title: string
  description: string
}

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <div className="grid min-h-64 place-items-center p-6 text-center">
      <div>
        <PackageOpen className="mx-auto size-9 text-[#8ca094]" aria-hidden="true" />
        <p className="mt-3 font-medium">{title}</p>
        <p className="mt-1 text-sm text-[#718177]">{description}</p>
      </div>
    </div>
  )
}
