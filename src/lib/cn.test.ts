import { expect, it } from 'vitest'
import { cn } from './cn'

it('merges conditional and conflicting Tailwind classes', () => {
  expect(cn('px-2 text-red-500', { hidden: false }, 'px-4')).toBe(
    'text-red-500 px-4',
  )
})
