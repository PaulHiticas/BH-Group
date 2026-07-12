"use client"

import { useQuery } from "@tanstack/react-query"
import { ownerApi, type OwnerReservationListParams } from "@/lib/api/owner"

export function useOwnerDashboardSummary() {
  return useQuery({
    queryKey: ["owner-dashboard-summary"],
    queryFn: () => ownerApi.dashboardSummary(),
  })
}

export function useOwnerProperties(page = 0, size = 12) {
  return useQuery({
    queryKey: ["owner-properties", page, size],
    queryFn: () => ownerApi.listProperties(page, size),
  })
}

export function useOwnerProperty(id: string) {
  return useQuery({
    queryKey: ["owner-property", id],
    queryFn: () => ownerApi.getProperty(id),
    enabled: !!id,
  })
}

export function useOwnerReservations(params: OwnerReservationListParams) {
  return useQuery({
    queryKey: ["owner-reservations", params],
    queryFn: () => ownerApi.listReservations(params),
  })
}

export function useOwnerMaintenanceTickets(page = 0, size = 10) {
  return useQuery({
    queryKey: ["owner-maintenance-tickets", page, size],
    queryFn: () => ownerApi.listMaintenanceTickets(page, size),
  })
}

export function useOwnerExpenses(page = 0, size = 10) {
  return useQuery({
    queryKey: ["owner-expenses", page, size],
    queryFn: () => ownerApi.listExpenses(page, size),
  })
}
