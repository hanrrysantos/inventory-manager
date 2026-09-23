import { describe, expect, it } from 'vitest'
import { productCreateSchema, productEditSchema } from './product-schema'

describe('product schemas', () => {
  it('rejects missing and non-positive identifiers', () => {
    expect(
      productCreateSchema.safeParse({
        name: '',
        sku: '',
        minStock: -1,
        categoryId: 0,
      }).success,
    ).toBe(false)
  })

  it('enforces the product name limit', () => {
    expect(
      productCreateSchema.safeParse({
        name: 'a'.repeat(256),
        sku: 'SKU',
        minStock: 1,
        categoryId: 1,
      }).success,
    ).toBe(false)
  })

  it('enforces the SKU limit', () => {
    expect(
      productCreateSchema.safeParse({
        name: 'Café',
        sku: 'a'.repeat(51),
        minStock: 1,
        categoryId: 1,
      }).success,
    ).toBe(false)
  })

  it('accepts only whole minimum stock quantities', () => {
    expect(
      productCreateSchema.safeParse({
        name: 'Café',
        sku: 'CAF-1',
        minStock: 1.5,
        categoryId: 1,
      }).success,
    ).toBe(false)
    expect(
      productEditSchema.safeParse({ name: 'Café', minStock: 0 }).success,
    ).toBe(true)
  })

  it('rejects an empty minimum stock quantity', () => {
    expect(
      productCreateSchema.safeParse({
        name: 'Café',
        sku: 'CAF-1',
        minStock: '',
        categoryId: 1,
      }).success,
    ).toBe(false)
    expect(
      productEditSchema.safeParse({ name: 'Café', minStock: '' }).success,
    ).toBe(false)
  })
})
