"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { leadsApi, type LeadCreatePayload } from "@/lib/api/leads"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useCreateLead() {
  return useMutation({
    mutationFn: (payload: LeadCreatePayload) => leadsApi.create(payload),
    onSuccess: () => {
      toast.success("Mulțumim! Te vom contacta în cel mai scurt timp.")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Trimiterea a eșuat. Încearcă din nou."))
    },
  })
}

export function useLeads(page = 0, size = 10) {
  return useQuery({
    queryKey: ["leads", page, size],
    queryFn: () => leadsApi.list(page, size),
  })
}

export function useMarkLeadContacted() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, contacted }: { id: string; contacted: boolean }) =>
      leadsApi.markContacted(id, contacted),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["leads"] })
      queryClient.invalidateQueries({ queryKey: ["dashboard-summary"] })
    },
  })
}
