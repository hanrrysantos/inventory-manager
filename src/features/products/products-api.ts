import { apiClient } from '../../services/api-client'
import type {
  PageQuery,
  PageResponse,
} from '../../services/contracts/pagination'
import type {
  Product,
  ProductInput,
  ProductUpdateInput,
} from '../../services/contracts/product'

export async function getProducts(
  query: PageQuery,
): Promise<PageResponse<Product>> {
  const { data } = await apiClient.get<PageResponse<Product>>(
    '/api/v1/products',
    { params: query },
  )
  return data
}

export async function getProduct(id: number): Promise<Product> {
  const { data } = await apiClient.get<Product>(`/api/v1/products/${id}`)
  return data
}

export async function getLowStockProducts(
  query: PageQuery,
): Promise<PageResponse<Product>> {
  const { data } = await apiClient.get<PageResponse<Product>>(
    '/api/v1/products/low-stock',
    { params: query },
  )
  return data
}

export async function createProduct(input: ProductInput): Promise<Product> {
  const { data } = await apiClient.post<Product>('/api/v1/products', input)
  return data
}

export async function updateProduct(
  id: number,
  input: ProductUpdateInput,
): Promise<Product> {
  const { data } = await apiClient.put<Product>(
    `/api/v1/products/${id}`,
    input,
  )
  return data
}

export async function deleteProduct(id: number): Promise<void> {
  await apiClient.delete(`/api/v1/products/${id}`)
}
