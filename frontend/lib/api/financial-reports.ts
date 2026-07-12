import { apiClient } from "@/lib/api/client"
import type { FinancialReportSummaryResponse } from "@/lib/api/types"

export interface FinancialReportParams {
  propertyId?: string
  from?: string
  to?: string
}

function buildQuery(params: Record<string, string | undefined>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  const qs = query.toString()
  return qs ? `?${qs}` : ""
}

export const financialReportsApi = {
  summary: (params: FinancialReportParams = {}) =>
    apiClient.get<FinancialReportSummaryResponse>(
      `/reports/financial/summary${buildQuery({ propertyId: params.propertyId, from: params.from, to: params.to })}`
    ),
}
