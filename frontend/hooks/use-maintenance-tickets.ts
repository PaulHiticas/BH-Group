"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import {
  maintenanceTicketsApi,
  type MaintenanceTicketCreatePayload,
  type MaintenanceTicketListParams,
  type MaintenanceTicketUpdatePayload,
} from "@/lib/api/maintenance"
import { ApiError, type MaintenanceStatus } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useMaintenanceTickets(params: MaintenanceTicketListParams) {
  return useQuery({
    queryKey: ["maintenance-tickets", params],
    queryFn: () => maintenanceTicketsApi.list(params),
  })
}

export function useMaintenanceTicket(id: string) {
  return useQuery({
    queryKey: ["maintenance-ticket", id],
    queryFn: () => maintenanceTicketsApi.get(id),
    enabled: !!id,
  })
}

export function useCreateMaintenanceTicket() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: MaintenanceTicketCreatePayload) => maintenanceTicketsApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["maintenance-tickets"] })
      toast.success("Tichet de mentenanță creat")
    },
    onError: (error) => toast.error(errorMessage(error, "Crearea tichetului a eșuat")),
  })
}

export function useUpdateMaintenanceTicket(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: MaintenanceTicketUpdatePayload) => maintenanceTicketsApi.update(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["maintenance-tickets"] })
      queryClient.invalidateQueries({ queryKey: ["maintenance-ticket", id] })
      toast.success("Tichet actualizat")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea a eșuat")),
  })
}

export function useAssignMaintenanceTicket() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, assignedToId }: { id: string; assignedToId: string }) =>
      maintenanceTicketsApi.assign(id, assignedToId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["maintenance-tickets"] })
      toast.success("Tehnician alocat")
    },
    onError: (error) => toast.error(errorMessage(error, "Alocarea a eșuat")),
  })
}

export function useUpdateMaintenanceTicketStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: MaintenanceStatus }) =>
      maintenanceTicketsApi.updateStatus(id, status),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["maintenance-tickets"] })
      queryClient.invalidateQueries({ queryKey: ["maintenance-ticket", variables.id] })
      toast.success("Status actualizat")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea statusului a eșuat")),
  })
}
