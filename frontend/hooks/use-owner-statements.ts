"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import {
  ownerStatementsApi,
  ownerStatementsSelfApi,
  type OwnerStatementGeneratePayload,
  type OwnerStatementListParams,
} from "@/lib/api/owner-statements"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useOwnerStatements(params: OwnerStatementListParams) {
  return useQuery({
    queryKey: ["owner-statements", params],
    queryFn: () => ownerStatementsApi.list(params),
  })
}

export function useOwnerStatement(id: string | undefined) {
  return useQuery({
    queryKey: ["owner-statement", id],
    queryFn: () => ownerStatementsApi.get(id as string),
    enabled: !!id,
  })
}

export function useGenerateOwnerStatement() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: OwnerStatementGeneratePayload) => ownerStatementsApi.generate(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["owner-statements"] })
      toast.success("Decont generat cu succes")
    },
    onError: (error) => toast.error(errorMessage(error, "Generarea decontului a eșuat")),
  })
}

export function useMarkStatementPaid() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, paymentReference }: { id: string; paymentReference?: string }) =>
      ownerStatementsApi.markPaid(id, paymentReference),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["owner-statements"] })
      queryClient.invalidateQueries({ queryKey: ["owner-statement"] })
      toast.success("Decont marcat ca plătit")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea decontului a eșuat")),
  })
}

export function useMyOwnerStatements(page = 0, size = 10) {
  return useQuery({
    queryKey: ["my-owner-statements", page, size],
    queryFn: () => ownerStatementsSelfApi.list(page, size),
  })
}

export function useMyOwnerStatement(id: string | undefined) {
  return useQuery({
    queryKey: ["my-owner-statement", id],
    queryFn: () => ownerStatementsSelfApi.get(id as string),
    enabled: !!id,
  })
}
