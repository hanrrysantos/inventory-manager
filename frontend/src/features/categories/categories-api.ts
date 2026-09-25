import { apiClient } from '../../services/api-client'
import type {
  Category,
  CategoryInput,
} from '../../services/contracts/category'
import type {
  PageQuery,
  PageResponse,
} from '../../services/contracts/pagination'

export async function getCategories(
  query: PageQuery,
): Promise<PageResponse<Category>> {
  const { data } = await apiClient.get<PageResponse<Category>>(
    '/api/v1/categories',
    { params: query },
  )
  return data
}

export async function getCategory(id: number): Promise<Category> {
  const { data } = await apiClient.get<Category>(`/api/v1/categories/${id}`)
  return data
}

export async function createCategory(
  input: CategoryInput,
): Promise<Category> {
  const { data } = await apiClient.post<Category>('/api/v1/categories', input)
  return data
}

export async function updateCategory(
  id: number,
  input: CategoryInput,
): Promise<Category> {
  const { data } = await apiClient.put<Category>(
    `/api/v1/categories/${id}`,
    input,
  )
  return data
}

export async function deleteCategory(id: number): Promise<void> {
  await apiClient.delete(`/api/v1/categories/${id}`)
}
