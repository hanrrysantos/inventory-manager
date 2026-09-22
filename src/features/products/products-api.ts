import { apiClient } from '../../services/api-client'
import type {
  PageQuery,
  PageResponse,
} from '../../services/contracts/pagination'
import type { Product } from '../../services/contracts/product'

export async function getProducts(
  query: PageQuery,
): Promise<PageResponse<Product>> {
  const { data } = await apiClient.get<PageResponse<Product>>(
    '/api/v1/products',
    { params: query },
  )
  return data
}
