import { apiClient } from "@/lib/api/client"
import type { CleaningTaskResponse, CleaningTaskStatus, PageResponse } from "@/lib/api/types"

export interface CleaningTaskCreatePayload {
  propertyId: string
  scheduledDate: string
  assignedCleanerId?: string
  notes?: string
}

export interface CleaningTaskUpdatePayload {
  scheduledDate?: string
  notes?: string
  cost?: number | null
  estimatedMinutes?: number | null
}

export interface CleaningTaskListParams {
  propertyId?: string
  status?: CleaningTaskStatus
  cleanerId?: string
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

export const cleaningTasksApi = {
  list: (params: CleaningTaskListParams = {}) =>
    apiClient.get<PageResponse<CleaningTaskResponse>>(
      `/cleaning-tasks${buildQuery({
        propertyId: params.propertyId,
        status: params.status,
        cleanerId: params.cleanerId,
        page: params.page,
        size: params.size ?? 10,
      })}`
    ),

  get: (id: string) => apiClient.get<CleaningTaskResponse>(`/cleaning-tasks/${id}`),

  create: (payload: CleaningTaskCreatePayload) =>
    apiClient.post<CleaningTaskResponse>("/cleaning-tasks", payload),

  update: (id: string, payload: CleaningTaskUpdatePayload) =>
    apiClient.put<CleaningTaskResponse>(`/cleaning-tasks/${id}`, payload),

  assign: (id: string, cleanerId: string) =>
    apiClient.patch<CleaningTaskResponse>(`/cleaning-tasks/${id}/assign`, { cleanerId }),

  updateStatus: (id: string, status: CleaningTaskStatus) =>
    apiClient.patch<CleaningTaskResponse>(`/cleaning-tasks/${id}/status`, { status }),

  updateChecklistItem: (id: string, label: string, checked: boolean) =>
    apiClient.patch<CleaningTaskResponse>(`/cleaning-tasks/${id}/checklist`, { label, checked }),

  uploadPhoto: (id: string, file: File, caption?: string) => {
    const formData = new FormData()
    formData.set("file", file)
    if (caption) formData.set("caption", caption)
    return apiClient.upload<CleaningTaskResponse>(`/cleaning-tasks/${id}/photos`, formData)
  },
}
