"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { assistantChatsApi, type AssistantChatStatus } from "@/lib/api/assistant-chats"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useAssistantChats(params: { status?: AssistantChatStatus; page?: number; size?: number }) {
  return useQuery({
    queryKey: ["assistant-chats", params],
    queryFn: () => assistantChatsApi.list(params),
  })
}

export function useAssistantChatDetail(id: string | undefined) {
  return useQuery({
    queryKey: ["assistant-chat", id],
    queryFn: () => assistantChatsApi.get(id as string),
    enabled: !!id,
  })
}

export function useAddAssistantChatReply(chatId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: string) => assistantChatsApi.addMessage(chatId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assistant-chat", chatId] })
      queryClient.invalidateQueries({ queryKey: ["assistant-chats"] })
    },
    onError: (error) => toast.error(errorMessage(error, "Răspunsul nu a putut fi trimis")),
  })
}

export function useResolveAssistantChat(chatId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => assistantChatsApi.resolve(chatId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assistant-chat", chatId] })
      queryClient.invalidateQueries({ queryKey: ["assistant-chats"] })
      toast.success("Conversația a fost marcată ca rezolvată")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea statusului a eșuat")),
  })
}
