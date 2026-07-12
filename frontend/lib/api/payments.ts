import { apiClient } from "@/lib/api/client"
import type { PaymentMethod, PaymentResponse } from "@/lib/api/types"

export interface ManualPaymentPayload {
  reservationId: string
  amount: number
  method: PaymentMethod
  notes?: string
}

export interface RefundPayload {
  amount: number
  reason?: string
}

export const paymentsApi = {
  listForReservation: (reservationId: string) =>
    apiClient.get<PaymentResponse[]>(`/payments?reservationId=${reservationId}`),

  recordManualPayment: (payload: ManualPaymentPayload) =>
    apiClient.post<PaymentResponse>("/payments", payload),

  refund: (paymentId: string, payload: RefundPayload) =>
    apiClient.post<PaymentResponse>(`/payments/${paymentId}/refund`, payload),
}
