"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import {
  publicApi,
  type PublicBookingPayload,
  type PublicBookingUpdatePayload,
  type PublicPropertySearchParams,
} from "@/lib/api/public"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function usePublicProperties(params: PublicPropertySearchParams) {
  return useQuery({
    queryKey: ["public-properties", params],
    queryFn: () => publicApi.searchProperties(params),
  })
}

export function usePublicProperty(id: string) {
  return useQuery({
    queryKey: ["public-property", id],
    queryFn: () => publicApi.getProperty(id),
    enabled: !!id,
  })
}

export function usePublicAvailability(propertyId: string, checkIn: string, checkOut: string) {
  return useQuery({
    queryKey: ["public-availability", propertyId, checkIn, checkOut],
    queryFn: () => publicApi.checkAvailability(propertyId, checkIn, checkOut),
    enabled: !!propertyId && !!checkIn && !!checkOut,
  })
}

export function usePublicQuote(propertyId: string, checkIn: string, checkOut: string, guests: number) {
  return useQuery({
    queryKey: ["public-quote", propertyId, checkIn, checkOut, guests],
    queryFn: () => publicApi.getQuote(propertyId, checkIn, checkOut, guests),
    enabled: !!propertyId && !!checkIn && !!checkOut && guests > 0,
  })
}

export function usePublicCalendar(propertyId: string, from: string, to: string) {
  return useQuery({
    queryKey: ["public-calendar", propertyId, from, to],
    queryFn: () => publicApi.getCalendar(propertyId, from, to),
    enabled: !!propertyId,
  })
}

export function useCreatePublicBooking() {
  return useMutation({
    mutationFn: (payload: PublicBookingPayload) => publicApi.createBooking(payload),
    onError: (error) => {
      toast.error(errorMessage(error, "Rezervarea nu a putut fi finalizată"))
    },
  })
}

export function useBookingByToken(token: string) {
  return useQuery({
    queryKey: ["booking-manage", token],
    queryFn: () => publicApi.getBookingByToken(token),
    enabled: !!token,
    retry: false,
  })
}

export function useUpdateBookingByToken(token: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: PublicBookingUpdatePayload) => publicApi.updateBookingByToken(token, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["booking-manage", token] })
      toast.success("Rezervarea a fost actualizată")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Actualizarea rezervării a eșuat"))
    },
  })
}

export function useBookingCancellationQuote(token: string, enabled: boolean) {
  return useQuery({
    queryKey: ["booking-cancellation-quote", token],
    queryFn: () => publicApi.getCancellationQuoteByToken(token),
    enabled: enabled && !!token,
  })
}

export function useCancelBookingByToken(token: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => publicApi.cancelBookingByToken(token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["booking-manage", token] })
      toast.success("Rezervarea a fost anulată")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Anularea rezervării a eșuat"))
    },
  })
}
