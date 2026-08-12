import { apiClient } from "@/lib/api/client"
import type {
  OwnerStatementResponse,
  OwnerStatementStatus,
  OwnerStatementSummaryResponse,
  PageResponse,
} from "@/lib/api/types"

export interface OwnerStatementListParams {
  ownerId?: string
  status?: OwnerStatementStatus
  page?: number
  size?: number
}

export interface OwnerStatementGeneratePayload {
  ownerId: string
  periodStart: string
  periodEnd: string
}

function buildQuery(params: Record<string, string | number | undefined>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      query.set(key, String(value))
    }
  })
  const qs = query.toString()
  return qs ? `?${qs}` : ""
}

export const ownerStatementsApi = {
  list: (params: OwnerStatementListParams = {}) =>
    apiClient.get<PageResponse<OwnerStatementSummaryResponse>>(
      `/owner-statements${buildQuery({
        ownerId: params.ownerId,
        status: params.status,
        page: params.page,
        size: params.size ?? 10,
      })}`
    ),

  get: (id: string) => apiClient.get<OwnerStatementResponse>(`/owner-statements/${id}`),

  generate: (payload: OwnerStatementGeneratePayload) =>
    apiClient.post<OwnerStatementResponse[]>("/owner-statements/generate", payload),

  markPaid: (id: string, paymentReference?: string) =>
    apiClient.patch<OwnerStatementResponse>(`/owner-statements/${id}/mark-paid`, { paymentReference }),
}

export const ownerStatementsSelfApi = {
  list: (page = 0, size = 10) =>
    apiClient.get<PageResponse<OwnerStatementSummaryResponse>>(
      `/owner/statements${buildQuery({ page, size })}`
    ),

  get: (id: string) => apiClient.get<OwnerStatementResponse>(`/owner/statements/${id}`),
}
