import { describe, expect, it } from 'vitest'
import { formatCurrency, formatRole } from './formatters'

describe('formatCurrency', () => {
  it('formats inventory values as Brazilian currency', () => {
    expect(formatCurrency(21480)).toMatch(/R\$\s21\.480,00/)
  })
})

describe('formatRole', () => {
  it.each([
    ['ADMIN', 'Administradora'],
    ['USER', 'Usuária'],
    ['AUDITOR', 'AUDITOR'],
  ])('formats %s as %s', (role, expected) => {
    expect(formatRole(role)).toBe(expected)
  })
})
