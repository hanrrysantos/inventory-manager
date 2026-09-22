import { X } from 'lucide-react'
import { useEffect, useId, useRef, type ReactNode } from 'react'

interface AppDialogProps {
  title: string
  onClose: () => void
  children: ReactNode
}

export function AppDialog({ title, onClose, children }: AppDialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    ref.current?.showModal()
  }, [])

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault()
        ref.current?.close()
      }}
      onClose={onClose}
      className="m-auto w-[min(92vw,34rem)] rounded-3xl border border-[#d8e5dc] bg-white p-0 text-[#26382d] shadow-2xl backdrop:bg-[#15281b]/35"
    >
      <section className="p-6">
        <div className="flex items-center justify-between gap-4">
          <h2 id={titleId} className="text-xl font-semibold">
            {title}
          </h2>
          <button
            type="button"
            className="grid size-10 shrink-0 place-items-center rounded-full border border-[#d4e2d7] text-[#52645a] hover:bg-[#eef7f0]"
            aria-label="Fechar"
            onClick={() => ref.current?.close()}
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>
        <div className="mt-5">{children}</div>
      </section>
    </dialog>
  )
}
