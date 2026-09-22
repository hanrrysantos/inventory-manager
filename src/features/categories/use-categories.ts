import { useQuery } from '@tanstack/react-query'
import type { PageQuery } from '../../services/contracts/pagination'
import { getCategories } from './categories-api'

export function useCategories(query: PageQuery) {
  return useQuery({
    queryKey: ['categories', query],
    queryFn: () => getCategories(query),
  })
}
