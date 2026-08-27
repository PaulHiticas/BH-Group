"use client"

import { useMutation, useQuery } from "@tanstack/react-query"
import { assistantApi, type AssistantHandoffPayload, type AssistantMessage } from "@/lib/api/assistant"

export function useAssistantChat() {
  return useMutation({
    mutationFn: (messages: AssistantMessage[]) => assistantApi.chat(messages),
  })
}

export function useAssistantHandoff() {
  return useMutation({
    mutationFn: (payload: AssistantHandoffPayload) => assistantApi.handoff(payload),
  })
}

/** Polls a visitor's own handoff chat for staff replies. */
export function useAssistantHandoffMessages(publicToken: string | null) {
  return useQuery({
    queryKey: ["assistant-handoff-messages", publicToken],
    queryFn: () => assistantApi.getHandoffMessages(publicToken as string),
    enabled: !!publicToken,
    refetchInterval: 5000,
  })
}
