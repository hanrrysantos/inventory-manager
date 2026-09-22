import { useQuery } from '@tanstack/react-query'
import type { PageQuery } from '../../services/contracts/pagination'
import { getProducts } from './products-api'

export function useProducts(query: PageQuery) {
  return useQuery({
    queryKey: ['products', query],
    queryFn: () => getProducts(query),
  })
}
