import type { AnchorHTMLAttributes, MouseEvent } from 'react'

interface SmoothAnchorProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  href: `#${string}`
}

export function SmoothAnchor({ href, onClick, ...props }: SmoothAnchorProps) {
  function handleClick(event: MouseEvent<HTMLAnchorElement>) {
    onClick?.(event)

    if (
      event.defaultPrevented ||
      event.button !== 0 ||
      event.metaKey ||
      event.ctrlKey ||
      event.shiftKey ||
      event.altKey
    ) {
      return
    }

    const target = document.querySelector<HTMLElement>(href)

    if (!target) {
      return
    }

    event.preventDefault()
    const previousUrl = window.location.href

    if (window.location.hash !== href) {
      window.history.pushState(null, '', href)
      window.dispatchEvent(
        new HashChangeEvent('hashchange', {
          oldURL: previousUrl,
          newURL: window.location.href,
        }),
      )
    }
    const reduceMotion = window.matchMedia?.(
      '(prefers-reduced-motion: reduce)',
    ).matches

    target.scrollIntoView({
      behavior: reduceMotion ? 'auto' : 'smooth',
      block: 'start',
    })
    target.tabIndex = -1
    target.focus({ preventScroll: true })
  }

  return <a href={href} onClick={handleClick} {...props} />
}
