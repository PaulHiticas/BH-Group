"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { paymentsApi, type ManualPaymentPayload, type RefundPayload } from "@/lib/api/payments"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useReservationPayments(reservationId: string) {
  return useQuery({
    queryKey: ["payments", reservationId],
    queryFn: () => paymentsApi.listForReservation(reservationId),
    enabled: !!reservationId,
  })
}

export function useRecordManualPayment(reservationId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: Omit<ManualPaymentPayload, "reservationId">) =>
      paymentsApi.recordManualPayment({ ...payload, reservationId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["payments", reservationId] })
      queryClient.invalidateQueries({ queryKey: ["reservation", reservationId] })
      toast.success("Plată înregistrată")
    },
    onError: (error) => toast.error(errorMessage(error, "Înregistrarea plății a eșuat")),
  })
}

export function useRefundPayment(reservationId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ paymentId, payload }: { paymentId: string; payload: RefundPayload }) =>
      paymentsApi.refund(paymentId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["payments", reservationId] })
      toast.success("Rambursare procesată")
    },
    onError: (error) => toast.error(errorMessage(error, "Rambursarea a eșuat")),
  })
}
