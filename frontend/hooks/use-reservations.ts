"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { toast } from "sonner"
import {
  reservationsApi,
  type ReservationListParams,
  type ReservationPayload,
} from "@/lib/api/reservations"
import { ApiError, type ReservationStatus } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useReservations(params: ReservationListParams) {
  return useQuery({
    queryKey: ["reservations", params],
    queryFn: () => reservationsApi.list(params),
  })
}

export function useReservation(id: string) {
  return useQuery({
    queryKey: ["reservation", id],
    queryFn: () => reservationsApi.get(id),
    enabled: !!id,
  })
}

export function useCancellationQuote(id: string, enabled: boolean) {
  return useQuery({
    queryKey: ["cancellation-quote", id],
    queryFn: () => reservationsApi.cancellationQuote(id),
    enabled: enabled && !!id,
  })
}

export function useCalendar(propertyId: string, from: string, to: string) {
  return useQuery({
    queryKey: ["calendar", propertyId, from, to],
    queryFn: () => reservationsApi.calendar(propertyId, from, to),
    enabled: !!propertyId,
  })
}

export function useAvailability(propertyId: string, checkIn: string, checkOut: string) {
  return useQuery({
    queryKey: ["availability", propertyId, checkIn, checkOut],
    queryFn: () => reservationsApi.availability(propertyId, checkIn, checkOut),
    enabled: !!propertyId && !!checkIn && !!checkOut,
  })
}

function invalidateReservationQueries(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: ["reservations"] })
  queryClient.invalidateQueries({ queryKey: ["calendar"] })
  queryClient.invalidateQueries({ queryKey: ["dashboard-summary"] })
}

export function useCreateReservation() {
  const router = useRouter()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ReservationPayload) => reservationsApi.create(payload),
    onSuccess: (reservation) => {
      invalidateReservationQueries(queryClient)
      toast.success("Rezervare creată cu succes")
      router.push(`/dashboard/reservations/${reservation.id}`)
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Crearea rezervării a eșuat"))
    },
  })
}

export function useUpdateReservation(id: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ReservationPayload) => reservationsApi.update(id, payload),
    onSuccess: () => {
      invalidateReservationQueries(queryClient)
      queryClient.invalidateQueries({ queryKey: ["reservation", id] })
      toast.success("Rezervare actualizată cu succes")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Actualizarea rezervării a eșuat"))
    },
  })
}

export function useUpdateReservationStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: ReservationStatus }) =>
      reservationsApi.updateStatus(id, status),
    onSuccess: (_, { id }) => {
      invalidateReservationQueries(queryClient)
      queryClient.invalidateQueries({ queryKey: ["reservation", id] })
      queryClient.invalidateQueries({ queryKey: ["cleaning-tasks"] })
      toast.success("Status actualizat cu succes")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Actualizarea statusului a eșuat"))
    },
  })
}

export function useUpdateAccessCode(id: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (accessCode: string) => reservationsApi.updateAccessCode(id, accessCode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reservation", id] })
      toast.success("Cod acces salvat")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Salvarea codului de acces a eșuat"))
    },
  })
}

export function useSendCheckinInstructions(id: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => reservationsApi.sendCheckinInstructions(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reservation", id] })
      toast.success("Instrucțiunile de check-in au fost trimise")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Trimiterea instrucțiunilor a eșuat"))
    },
  })
}

export function useDeleteReservation() {
  const router = useRouter()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => reservationsApi.delete(id),
    onSuccess: () => {
      invalidateReservationQueries(queryClient)
      toast.success("Rezervare ștearsă cu succes")
      router.push("/dashboard/reservations")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Ștergerea rezervării a eșuat"))
    },
  })
}
