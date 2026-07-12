import { apiClient } from "@/lib/api/client"
import type { CleaningTaskResponse, CleaningTaskStatus, PageResponse } from "@/lib/api/types"

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

export const cleanerApi = {
  listMyTasks: (status?: CleaningTaskStatus, page = 0, size = 10) =>
    apiClient.get<PageResponse<CleaningTaskResponse>>(
      `/cleaner/tasks${buildQuery({ status, page, size })}`
    ),

  getMyTask: (id: string) => apiClient.get<CleaningTaskResponse>(`/cleaner/tasks/${id}`),

  updateStatus: (id: string, status: CleaningTaskStatus) =>
    apiClient.patch<CleaningTaskResponse>(`/cleaner/tasks/${id}/status`, { status }),

  updateChecklistItem: (id: string, label: string, checked: boolean) =>
    apiClient.patch<CleaningTaskResponse>(`/cleaner/tasks/${id}/checklist`, { label, checked }),

  uploadPhoto: (id: string, file: File, caption?: string) => {
    const formData = new FormData()
    formData.set("file", file)
    if (caption) formData.set("caption", caption)
    return apiClient.upload<CleaningTaskResponse>(`/cleaner/tasks/${id}/photos`, formData)
  },
}
