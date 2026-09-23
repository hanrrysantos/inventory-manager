import { useQuery } from '@tanstack/react-query'
import type { PageQuery } from '../../services/contracts/pagination'
import { getLowStockProducts, getProducts } from './products-api'

export function useProducts(query: PageQuery, lowStock = false) {
  return useQuery({
    queryKey: ['products', lowStock ? 'low-stock' : 'all', query],
    queryFn: () =>
      lowStock ? getLowStockProducts(query) : getProducts(query),
  })
}
