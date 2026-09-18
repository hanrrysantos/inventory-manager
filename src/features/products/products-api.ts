import { apiClient } from '../../services/api-client'
import type { Product } from '../../services/contracts/product'

export async function getProducts(): Promise<Product[]> {
  const { data } = await apiClient.get<Product[]>('/api/v1/products')
  return data
}
