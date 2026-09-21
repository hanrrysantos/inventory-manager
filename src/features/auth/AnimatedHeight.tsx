import { useLayoutEffect, useRef, useState, type ReactNode } from 'react'

interface AnimatedHeightProps {
  children: ReactNode
  dependency: unknown
}

const TRANSITION_MS = 260

export function AnimatedHeight({ children, dependency }: AnimatedHeightProps) {
  const contentRef = useRef<HTMLDivElement>(null)
  const previousDependency = useRef(dependency)
  const [height, setHeight] = useState<number>()
  const [animate, setAnimate] = useState(false)

  useLayoutEffect(() => {
    const content = contentRef.current
    if (!content) return

    const measure = () =>
      setHeight((current) => {
        const next = content.scrollHeight
        return current === next ? current : next
      })

    // Animate the height only when the user deliberately switches panels
    // (login <-> register). The initial mount and async content resizes
    // (e.g. the Google button finishing loading) must settle instantly so
    // the card does not flash or shake.
    const isSwitch = previousDependency.current !== dependency
    previousDependency.current = dependency

    let timeout: ReturnType<typeof setTimeout> | undefined
    if (isSwitch) {
      setAnimate(true)
      timeout = setTimeout(() => setAnimate(false), TRANSITION_MS)
    }

    measure()

    if (typeof ResizeObserver === 'undefined') {
      return () => {
        if (timeout) clearTimeout(timeout)
      }
    }

    const observer = new ResizeObserver(measure)
    observer.observe(content)

    return () => {
      observer.disconnect()
      if (timeout) clearTimeout(timeout)
    }
  }, [dependency])

  return (
    <div
      className={
        animate
          ? 'overflow-hidden transition-[height] duration-[260ms] ease-[cubic-bezier(0.22,0.61,0.36,1)] motion-reduce:transition-none'
          : 'overflow-hidden'
      }
      data-testid="access-panel-height"
      style={{ height }}
    >
      <div ref={contentRef} style={{ display: 'flow-root' }}>{children}</div>
    </div>
  )
}
