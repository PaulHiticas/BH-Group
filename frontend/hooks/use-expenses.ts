"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import {
  expensesApi,
  type ExpenseCreatePayload,
  type ExpenseListParams,
  type ExpenseUpdatePayload,
} from "@/lib/api/expenses"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useExpenses(params: ExpenseListParams) {
  return useQuery({
    queryKey: ["expenses", params],
    queryFn: () => expensesApi.list(params),
  })
}

export function useCreateExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: ExpenseCreatePayload) => expensesApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] })
      queryClient.invalidateQueries({ queryKey: ["financial-report"] })
      toast.success("Cheltuială înregistrată")
    },
    onError: (error) => toast.error(errorMessage(error, "Înregistrarea cheltuielii a eșuat")),
  })
}

export function useUpdateExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: ExpenseUpdatePayload }) =>
      expensesApi.update(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] })
      queryClient.invalidateQueries({ queryKey: ["financial-report"] })
      toast.success("Cheltuială actualizată")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea cheltuielii a eșuat")),
  })
}

export function useDeleteExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => expensesApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] })
      queryClient.invalidateQueries({ queryKey: ["financial-report"] })
      toast.success("Cheltuială ștearsă")
    },
    onError: (error) => toast.error(errorMessage(error, "Ștergerea cheltuielii a eșuat")),
  })
}

export function useUploadExpenseReceipt() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, file }: { id: string; file: File }) => expensesApi.uploadReceipt(id, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["expenses"] })
      toast.success("Bon/factură încărcat(ă)")
    },
    onError: (error) => toast.error(errorMessage(error, "Încărcarea a eșuat")),
  })
}
