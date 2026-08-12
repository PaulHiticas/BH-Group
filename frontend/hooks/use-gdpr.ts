"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { gdprApi } from "@/lib/api/gdpr"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useGdprSearch(email: string) {
  return useQuery({
    queryKey: ["gdpr-search", email],
    queryFn: () => gdprApi.search(email),
    enabled: false,
  })
}

export function useEraseGdprData() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (email: string) => gdprApi.erase(email),
    onSuccess: (_result, email) => {
      queryClient.invalidateQueries({ queryKey: ["gdpr-search", email] })
      toast.success("Datele au fost anonimizate")
    },
    onError: (error) => toast.error(errorMessage(error, "Anonimizarea a eșuat")),
  })
}
