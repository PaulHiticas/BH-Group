import { apiClient } from "@/lib/api/client"
import type {
  MaintenanceCategory,
  MaintenancePriority,
  MaintenanceStatus,
  MaintenanceTicketResponse,
  PageResponse,
} from "@/lib/api/types"

export interface MaintenanceTicketCreatePayload {
  propertyId: string
  title: string
  description?: string
  category?: MaintenanceCategory
  priority?: MaintenancePriority
  vendor?: string
  estimatedCost?: number
}

export interface MaintenanceTicketUpdatePayload {
  title: string
  description?: string
  category?: MaintenanceCategory
  priority?: MaintenancePriority
  vendor?: string
  estimatedCost?: number | null
  actualCost?: number | null
}

export interface MaintenanceTicketListParams {
  propertyId?: string
  status?: MaintenanceStatus
  priority?: MaintenancePriority
  category?: MaintenanceCategory
  assigneeId?: string
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

export const maintenanceTicketsApi = {
  list: (params: MaintenanceTicketListParams = {}) =>
    apiClient.get<PageResponse<MaintenanceTicketResponse>>(
      `/maintenance-tickets${buildQuery({
        propertyId: params.propertyId,
        status: params.status,
        priority: params.priority,
        category: params.category,
        assigneeId: params.assigneeId,
        page: params.page,
        size: params.size ?? 10,
      })}`
    ),

  get: (id: string) => apiClient.get<MaintenanceTicketResponse>(`/maintenance-tickets/${id}`),

  create: (payload: MaintenanceTicketCreatePayload) =>
    apiClient.post<MaintenanceTicketResponse>("/maintenance-tickets", payload),

  update: (id: string, payload: MaintenanceTicketUpdatePayload) =>
    apiClient.put<MaintenanceTicketResponse>(`/maintenance-tickets/${id}`, payload),

  assign: (id: string, assignedToId: string) =>
    apiClient.patch<MaintenanceTicketResponse>(`/maintenance-tickets/${id}/assign`, { assignedToId }),

  updateStatus: (id: string, status: MaintenanceStatus) =>
    apiClient.patch<MaintenanceTicketResponse>(`/maintenance-tickets/${id}/status`, { status }),

  uploadPhoto: (id: string, file: File, caption?: string) => {
    const formData = new FormData()
    formData.set("file", file)
    if (caption) formData.set("caption", caption)
    return apiClient.upload<MaintenanceTicketResponse>(`/maintenance-tickets/${id}/photos`, formData)
  },
}
