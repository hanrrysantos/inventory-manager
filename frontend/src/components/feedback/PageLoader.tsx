export function PageLoader({ label = 'Carregando dados...' }: { label?: string }) {
  return (
    <div className="grid min-h-64 place-items-center" role="status">
      <div className="flex items-center gap-3 text-sm text-[#6e7f73]">
        <span className="size-5 animate-spin rounded-full border-2 border-[#cfe3d5] border-t-[#58b978]" />
        {label}
      </div>
    </div>
  )
}
