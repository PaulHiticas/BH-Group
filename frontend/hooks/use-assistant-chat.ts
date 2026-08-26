"use client"

import { useMutation } from "@tanstack/react-query"
import { assistantApi, type AssistantMessage } from "@/lib/api/assistant"

export function useAssistantChat() {
  return useMutation({
    mutationFn: (messages: AssistantMessage[]) => assistantApi.chat(messages),
  })
}
