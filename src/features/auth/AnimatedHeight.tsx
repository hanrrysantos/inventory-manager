import { useLayoutEffect, useRef, useState, type ReactNode } from 'react'

interface AnimatedHeightProps {
  children: ReactNode
  dependency: unknown
}

export function AnimatedHeight({ children, dependency }: AnimatedHeightProps) {
  const contentRef = useRef<HTMLDivElement>(null)
  const [height, setHeight] = useState<number>()

  useLayoutEffect(() => {
    const content = contentRef.current
    if (!content) return

    const measure = () => setHeight(content.scrollHeight)
    measure()

    if (typeof ResizeObserver === 'undefined') return

    const observer = new ResizeObserver(measure)
    observer.observe(content)

    return () => observer.disconnect()
  }, [dependency])

  return (
    <div
      className="overflow-hidden transition-[height] duration-300 ease-out motion-reduce:transition-none"
      data-testid="access-panel-height"
      style={{ height }}
    >
      <div ref={contentRef} style={{ display: 'flow-root' }}>{children}</div>
    </div>
  )
}
