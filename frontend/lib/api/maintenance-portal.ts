import { apiClient } from "@/lib/api/client"
import type { MaintenanceStatus, MaintenanceTicketResponse, PageResponse } from "@/lib/api/types"

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

export const maintenancePortalApi = {
  listMyTickets: (status?: MaintenanceStatus, page = 0, size = 10) =>
    apiClient.get<PageResponse<MaintenanceTicketResponse>>(
      `/maintenance/tickets${buildQuery({ status, page, size })}`
    ),

  getMyTicket: (id: string) => apiClient.get<MaintenanceTicketResponse>(`/maintenance/tickets/${id}`),

  updateStatus: (id: string, status: MaintenanceStatus) =>
    apiClient.patch<MaintenanceTicketResponse>(`/maintenance/tickets/${id}/status`, { status }),

  uploadPhoto: (id: string, file: File, caption?: string) => {
    const formData = new FormData()
    formData.set("file", file)
    if (caption) formData.set("caption", caption)
    return apiClient.upload<MaintenanceTicketResponse>(`/maintenance/tickets/${id}/photos`, formData)
  },
}
