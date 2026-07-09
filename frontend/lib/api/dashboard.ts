import { apiClient } from "@/lib/api/client"
import type { DashboardSummaryResponse } from "@/lib/api/types"

export const dashboardApi = {
  summary: () => apiClient.get<DashboardSummaryResponse>("/dashboard/summary"),
}
