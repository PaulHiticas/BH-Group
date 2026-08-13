"use client"

import { useMutation, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { gdprApi, type GdprVerification } from "@/lib/api/gdpr"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

/**
 * A mutation, not a query - this is an on-demand, admin-triggered lookup,
 * not something that should stay reactively in sync with component state.
 * (An earlier version used a disabled useQuery + refetch() + a setTimeout
 * to dodge a stale-closure race between the query key and the debounced
 * input; a mutation just doesn't have that problem.)
 */
export function useGdprSearch() {
  return useMutation({
    mutationFn: (email: string) => gdprApi.search(email),
    onError: (error) => toast.error(errorMessage(error, "Căutarea a eșuat")),
  })
}

export function useExportGdprData() {
  return useMutation({
    mutationFn: ({ email, verification }: { email: string; verification: GdprVerification }) =>
      gdprApi.export(email, verification),
    onError: (error) => toast.error(errorMessage(error, "Exportul a eșuat")),
  })
}

export function useEraseGdprData() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      email,
      confirmationEmail,
      verification,
    }: {
      email: string
      confirmationEmail: string
      verification: GdprVerification
    }) => gdprApi.erase(email, confirmationEmail, verification),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["gdpr-search"] })
      toast.success("Datele au fost anonimizate")
    },
    onError: (error) => toast.error(errorMessage(error, "Anonimizarea a eșuat")),
  })
}
