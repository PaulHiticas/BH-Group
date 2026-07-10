import { apiClient } from "@/lib/api/client"
import type { AuditLogResponse, PageResponse } from "@/lib/api/types"

export interface AuditLogSearchParams {
  entityName?: string
  action?: string
  actorEmail?: string
  from?: string
  to?: string
  page?: number
  size?: number
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

export const auditApi = {
  search: (params: AuditLogSearchParams = {}) =>
    apiClient.get<PageResponse<AuditLogResponse>>(
      `/admin/audit-logs${buildQuery({
        entityName: params.entityName,
        action: params.action,
        actorEmail: params.actorEmail,
        from: params.from,
        to: params.to,
        page: params.page,
        size: params.size ?? 20,
      })}`
    ),
}
