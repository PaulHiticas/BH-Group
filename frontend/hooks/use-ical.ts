"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { icalApi } from "@/lib/api/ical"
import { ApiError, type ReservationSource } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useIcalFeeds(propertyId: string) {
  return useQuery({
    queryKey: ["ical-feeds", propertyId],
    queryFn: () => icalApi.listFeeds(propertyId),
    enabled: !!propertyId,
  })
}

export function useRegenerateExportToken(propertyId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => icalApi.regenerateExportToken(propertyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["property", propertyId] })
      toast.success("Link de export regenerat")
    },
    onError: (error) => toast.error(errorMessage(error, "Regenerarea link-ului a eșuat")),
  })
}

export function useAddIcalFeed(propertyId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ source, feedUrl }: { source: ReservationSource; feedUrl: string }) =>
      icalApi.addFeed(propertyId, source, feedUrl),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ical-feeds", propertyId] })
      toast.success("Sursă de sincronizare adăugată")
    },
    onError: (error) => toast.error(errorMessage(error, "Adăugarea sursei a eșuat")),
  })
}

export function useSyncIcalFeed(propertyId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (feedId: string) => icalApi.syncFeed(propertyId, feedId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ical-feeds", propertyId] })
      queryClient.invalidateQueries({ queryKey: ["reservations"] })
      toast.success("Sincronizare finalizată")
    },
    onError: (error) => toast.error(errorMessage(error, "Sincronizarea a eșuat")),
  })
}

export function useDeleteIcalFeed(propertyId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (feedId: string) => icalApi.deleteFeed(propertyId, feedId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ical-feeds", propertyId] })
      toast.success("Sursă eliminată")
    },
    onError: (error) => toast.error(errorMessage(error, "Eliminarea sursei a eșuat")),
  })
}
