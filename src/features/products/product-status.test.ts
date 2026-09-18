import { describe, expect, it } from 'vitest'
import { getProductStatus } from './product-status'

describe('getProductStatus', () => {
  it.each([
    [0, 10, 'OUT_OF_STOCK'],
    [8, 10, 'LOW_STOCK'],
    [10, 10, 'LOW_STOCK'],
    [11, 10, 'IN_STOCK'],
  ] as const)('maps %s/%s to %s', (quantity, minimum, expected) => {
    expect(getProductStatus(quantity, minimum)).toBe(expected)
  })
})
