export type InventoryStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'

export function getProductStatus(
  totalQuantity: number,
  minStock: number,
): InventoryStatus {
  if (totalQuantity <= 0) return 'OUT_OF_STOCK'
  if (totalQuantity <= minStock) return 'LOW_STOCK'
  return 'IN_STOCK'
}
