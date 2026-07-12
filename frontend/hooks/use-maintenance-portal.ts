"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { maintenancePortalApi } from "@/lib/api/maintenance-portal"
import { ApiError, type MaintenanceStatus } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useMyMaintenanceTickets(status: MaintenanceStatus | undefined, page = 0) {
  return useQuery({
    queryKey: ["my-maintenance-tickets", status, page],
    queryFn: () => maintenancePortalApi.listMyTickets(status, page),
  })
}

export function useMyMaintenanceTicket(id: string) {
  return useQuery({
    queryKey: ["my-maintenance-ticket", id],
    queryFn: () => maintenancePortalApi.getMyTicket(id),
    enabled: !!id,
  })
}

export function useUpdateMyTicketStatus(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (status: MaintenanceStatus) => maintenancePortalApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-maintenance-tickets"] })
      queryClient.invalidateQueries({ queryKey: ["my-maintenance-ticket", id] })
      toast.success("Status actualizat")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea statusului a eșuat")),
  })
}

export function useUploadMyTicketPhoto(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => maintenancePortalApi.uploadPhoto(id, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-maintenance-ticket", id] })
      toast.success("Poză adăugată")
    },
    onError: (error) => toast.error(errorMessage(error, "Încărcarea pozei a eșuat")),
  })
}
