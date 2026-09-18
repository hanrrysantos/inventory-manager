import { AlertTriangle } from 'lucide-react'

interface ErrorStateProps {
  title: string
  onRetry: () => void
}

export function ErrorState({ title, onRetry }: ErrorStateProps) {
  return (
    <div className="grid min-h-64 place-items-center p-6 text-center" role="alert">
      <div>
        <AlertTriangle className="mx-auto size-8 text-[#d39b50]" aria-hidden="true" />
        <p className="mt-3 font-medium">{title}</p>
        <button
          type="button"
          className="mt-4 rounded-xl bg-[#58b978] px-4 py-2 text-sm font-medium text-white"
          onClick={onRetry}
        >
          Tentar novamente
        </button>
      </div>
    </div>
  )
}
