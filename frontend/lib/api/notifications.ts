import { apiClient } from "@/lib/api/client"
import type { NotificationResponse, PageResponse } from "@/lib/api/types"

export const notificationsApi = {
  list: (unreadOnly = false) =>
    apiClient.get<PageResponse<NotificationResponse>>(
      `/notifications?unreadOnly=${unreadOnly}&size=20`
    ),

  unreadCount: () => apiClient.get<{ count: number }>("/notifications/unread-count"),

  markRead: (id: string) => apiClient.post<void>(`/notifications/${id}/read`),

  markAllRead: () => apiClient.post<void>("/notifications/read-all"),
}
