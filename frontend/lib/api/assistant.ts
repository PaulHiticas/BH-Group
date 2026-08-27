import { apiClient } from "@/lib/api/client"

export type AssistantRole = "user" | "assistant"

export interface AssistantMessage {
  role: AssistantRole
  content: string
}

export interface AssistantChatResponse {
  message: string
  needsHuman: boolean
}

export interface AssistantHandoffPayload {
  messages: AssistantMessage[]
  guestName?: string
  guestEmail?: string
}

export interface AssistantHandoffResponse {
  publicToken: string
}

export type AssistantHandoffSenderType = "GUEST" | "AI" | "STAFF"

export interface AssistantHandoffMessage {
  id: string
  senderType: AssistantHandoffSenderType
  body: string
  createdAt: string
}

export const assistantApi = {
  chat: (messages: AssistantMessage[]) =>
    apiClient.post<AssistantChatResponse>("/assistant/chat", { messages }),

  handoff: (payload: AssistantHandoffPayload) =>
    apiClient.post<AssistantHandoffResponse>("/assistant/handoff", payload),

  getHandoffMessages: (publicToken: string) =>
    apiClient.get<AssistantHandoffMessage[]>(`/assistant/chat/${publicToken}/messages`),
}
