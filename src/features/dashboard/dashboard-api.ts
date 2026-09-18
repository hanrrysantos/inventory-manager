import { apiClient } from '../../services/api-client'
import type { DashboardSummary } from '../../services/contracts/dashboard'

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const { data } = await apiClient.get<DashboardSummary>(
    '/api/v1/dashboard/summary',
  )
  return data
}
