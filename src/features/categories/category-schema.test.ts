import { describe, expect, it } from 'vitest'
import { categorySchema } from './category-schema'

describe('categorySchema', () => {
  it('rejects an empty category name', () => {
    expect(
      categorySchema.safeParse({ name: ' ', description: '' }).success,
    ).toBe(false)
  })

  it('rejects category names longer than the API limit', () => {
    expect(
      categorySchema.safeParse({ name: 'a'.repeat(101), description: '' })
        .success,
    ).toBe(false)
  })

  it('trims a valid category name', () => {
    expect(
      categorySchema.parse({ name: ' Bebidas ', description: '' }),
    ).toEqual({ name: 'Bebidas', description: '' })
  })
})
