import type { InventoryStatus } from '../../features/products/product-status'

export interface AttentionItem {
  productId: number
  productName: string
  sku: string
  quantity: number
  minStock: number
  status: InventoryStatus
}

export interface DashboardSummary {
  totalQuantity: number
  productCount: number
  lowStockCount: number
  outOfStockCount: number
  expiredBatchCount: number
  inventoryValue: number
  attentionItems: AttentionItem[]
}
