import { apiClient } from "@/lib/api/client"

export type AssistantRole = "user" | "assistant"

export interface AssistantMessage {
  role: AssistantRole
  content: string
}

export interface AssistantChatResponse {
  message: string
}

export const assistantApi = {
  chat: (messages: AssistantMessage[]) =>
    apiClient.post<AssistantChatResponse>("/assistant/chat", { messages }),
}
