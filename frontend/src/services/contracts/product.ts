export interface Product {
  id: number
  name: string
  sku: string
  totalQuantity: number
  categoryName: string
  minStock: number
}

export interface ProductInput {
  name: string
  sku: string
  minStock: number
  categoryId: number
}

export type ProductUpdateInput = Pick<ProductInput, 'name' | 'minStock'>
