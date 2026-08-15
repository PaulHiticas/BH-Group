"use client"

import { useMemo } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import {
  pricingApi,
  type BreakdownParams,
  type DynamicPricingConfigPayload,
  type LocalEventPayload,
  type LocalEventResponse,
} from "@/lib/api/pricing"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function usePricingConfig(propertyId: string) {
  return useQuery({
    queryKey: ["pricing-config", propertyId],
    queryFn: () => pricingApi.getConfig(propertyId),
    enabled: !!propertyId,
  })
}

export function useUpdatePricingConfig(propertyId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: DynamicPricingConfigPayload) => pricingApi.updateConfig(propertyId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["pricing-config", propertyId] })
      toast.success("Configurația de preț dinamic a fost salvată")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Salvarea configurației a eșuat"))
    },
  })
}

export function usePricingBreakdown(propertyId: string, params: BreakdownParams) {
  const enabled =
    !!propertyId &&
    !!params.checkIn &&
    !!params.checkOut &&
    params.checkOut > params.checkIn &&
    params.guests >= 1

  return useQuery({
    queryKey: ["pricing-breakdown", propertyId, params],
    queryFn: () => pricingApi.getBreakdown(propertyId, params),
    enabled,
  })
}

export function useLocalEventsForProperty(propertyId: string) {
  return useQuery({
    queryKey: ["local-events-property", propertyId],
    queryFn: () => pricingApi.listEventsForProperty(propertyId),
    enabled: !!propertyId,
  })
}

export function useLocalEventsForCity(city: string) {
  return useQuery({
    queryKey: ["local-events-city", city],
    queryFn: () => pricingApi.listEventsForCity(city),
    enabled: !!city,
  })
}

/** Merges the property-scoped and city-scoped event lists, deduplicated by id (an event can only ever appear in one of the two, but query timing could otherwise double-render it). */
export function useMergedLocalEvents(propertyId: string, city: string) {
  const propertyEvents = useLocalEventsForProperty(propertyId)
  const cityEvents = useLocalEventsForCity(city)

  const events = useMemo(() => {
    const byId = new Map<string, LocalEventResponse>()
    for (const event of propertyEvents.data ?? []) byId.set(event.id, event)
    for (const event of cityEvents.data ?? []) byId.set(event.id, event)
    return Array.from(byId.values()).sort((a, b) => a.startDate.localeCompare(b.startDate))
  }, [propertyEvents.data, cityEvents.data])

  return {
    events,
    isLoading: propertyEvents.isLoading || cityEvents.isLoading,
    isError: propertyEvents.isError || cityEvents.isError,
  }
}

export function useCreateLocalEvent(propertyId: string, city: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: LocalEventPayload) => pricingApi.createEvent(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["local-events-property", propertyId] })
      if (city) queryClient.invalidateQueries({ queryKey: ["local-events-city", city] })
      toast.success("Eveniment adăugat")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Adăugarea evenimentului a eșuat"))
    },
  })
}

export function useDeleteLocalEvent(propertyId: string, city: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => pricingApi.deleteEvent(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["local-events-property", propertyId] })
      if (city) queryClient.invalidateQueries({ queryKey: ["local-events-city", city] })
      toast.success("Eveniment șters")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Ștergerea evenimentului a eșuat"))
    },
  })
}
