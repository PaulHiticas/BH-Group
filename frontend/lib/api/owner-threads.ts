import { apiClient } from "@/lib/api/client"
import type { PageResponse } from "@/lib/api/types"

export type OwnerThreadStatus = "OPEN" | "RESOLVED"
export type OwnerThreadSenderType = "OWNER" | "STAFF"

export interface OwnerThreadSummaryResponse {
  id: string
  subject: string
  status: OwnerThreadStatus
  propertyId: string | null
  propertyName: string | null
  ownerId: string
  ownerName: string
  lastMessageAt: string
  createdAt: string
}

export interface OwnerThreadMessageResponse {
  id: string
  senderType: OwnerThreadSenderType
  senderName: string
  body: string
  readAt: string | null
  createdAt: string
}

export interface OwnerThreadDetailResponse extends OwnerThreadSummaryResponse {
  messages: OwnerThreadMessageResponse[]
}

export interface OwnerThreadCreatePayload {
  subject: string
  propertyId?: string
  body: string
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

/** Owner-facing: the current owner's own contact threads only. */
export const ownerThreadsSelfApi = {
  list: (page = 0, size = 10) =>
    apiClient.get<PageResponse<OwnerThreadSummaryResponse>>(`/owner/threads${buildQuery({ page, size })}`),

  get: (id: string) => apiClient.get<OwnerThreadDetailResponse>(`/owner/threads/${id}`),

  create: (payload: OwnerThreadCreatePayload) =>
    apiClient.post<OwnerThreadDetailResponse>("/owner/threads", payload),

  addMessage: (id: string, body: string) =>
    apiClient.post<OwnerThreadMessageResponse>(`/owner/threads/${id}/messages`, { body }),
}

/** Staff-facing: the full inbox across every owner. */
export const ownerRequestsApi = {
  list: (params: { status?: OwnerThreadStatus; page?: number; size?: number } = {}) =>
    apiClient.get<PageResponse<OwnerThreadSummaryResponse>>(
      `/owner-requests${buildQuery({ status: params.status, page: params.page, size: params.size ?? 10 })}`
    ),

  get: (id: string) => apiClient.get<OwnerThreadDetailResponse>(`/owner-requests/${id}`),

  addMessage: (id: string, body: string) =>
    apiClient.post<OwnerThreadMessageResponse>(`/owner-requests/${id}/messages`, { body }),

  resolve: (id: string) => apiClient.patch<void>(`/owner-requests/${id}/resolve`),
}
