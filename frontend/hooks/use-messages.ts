"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { messagingApi } from "@/lib/api/messaging"
import { publicApi } from "@/lib/api/public"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useReservationMessages(reservationId: string, enabled = true) {
  return useQuery({
    queryKey: ["messages", reservationId],
    queryFn: () => messagingApi.listForReservation(reservationId),
    enabled: !!reservationId && enabled,
    refetchInterval: 20_000,
  })
}

export function useSendStaffMessage(reservationId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: string) => messagingApi.sendStaffMessage(reservationId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["messages", reservationId] })
    },
    onError: (error) => toast.error(errorMessage(error, "Mesajul nu a putut fi trimis")),
  })
}

export function useBookingMessages(token: string) {
  return useQuery({
    queryKey: ["booking-messages", token],
    queryFn: () => publicApi.getMessagesByToken(token),
    enabled: !!token,
    refetchInterval: 20_000,
  })
}

export function useSendGuestMessage(token: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: string) => publicApi.sendMessageByToken(token, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["booking-messages", token] })
    },
    onError: (error) => toast.error(errorMessage(error, "Mesajul nu a putut fi trimis")),
  })
}
