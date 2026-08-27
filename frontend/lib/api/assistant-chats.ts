import { apiClient } from "@/lib/api/client"
import type { PageResponse } from "@/lib/api/types"

export type AssistantChatStatus = "OPEN" | "RESOLVED"
export type AssistantChatSenderType = "GUEST" | "AI" | "STAFF"

export interface AssistantChatSummaryResponse {
  id: string
  guestName: string | null
  guestEmail: string | null
  status: AssistantChatStatus
  lastMessageAt: string
  createdAt: string
}

export interface AssistantChatMessageResponse {
  id: string
  senderType: AssistantChatSenderType
  body: string
  createdAt: string
}

export interface AssistantChatDetailResponse extends AssistantChatSummaryResponse {
  messages: AssistantChatMessageResponse[]
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

/** Staff-facing: the full inbox of AI-assistant human handoff chats. */
export const assistantChatsApi = {
  list: (params: { status?: AssistantChatStatus; page?: number; size?: number } = {}) =>
    apiClient.get<PageResponse<AssistantChatSummaryResponse>>(
      `/assistant-chats${buildQuery({ status: params.status, page: params.page, size: params.size ?? 10 })}`
    ),

  get: (id: string) => apiClient.get<AssistantChatDetailResponse>(`/assistant-chats/${id}`),

  addMessage: (id: string, body: string) =>
    apiClient.post<AssistantChatMessageResponse>(`/assistant-chats/${id}/messages`, { body }),

  resolve: (id: string) => apiClient.patch<void>(`/assistant-chats/${id}/resolve`),
}
