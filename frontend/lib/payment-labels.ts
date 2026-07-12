import type { PaymentMethod, PaymentStatus, RefundStatus } from "@/lib/api/types"

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: "Numerar",
  BANK_TRANSFER: "Transfer bancar",
  CARD_TERMINAL: "Card (POS)",
  ONLINE_CARD: "Card online",
  OTHER: "Altă metodă",
}

export const ALL_PAYMENT_METHODS: PaymentMethod[] = [
  "CASH",
  "BANK_TRANSFER",
  "CARD_TERMINAL",
  "ONLINE_CARD",
  "OTHER",
]

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: "În așteptare",
  PROCESSING: "În procesare",
  SUCCEEDED: "Reușită",
  FAILED: "Eșuată",
  CANCELLED: "Anulată",
  PARTIALLY_REFUNDED: "Rambursată parțial",
  REFUNDED: "Rambursată",
}

export const PAYMENT_STATUS_BADGE_VARIANT: Record<
  PaymentStatus,
  "default" | "secondary" | "outline" | "destructive"
> = {
  PENDING: "outline",
  PROCESSING: "outline",
  SUCCEEDED: "default",
  FAILED: "destructive",
  CANCELLED: "destructive",
  PARTIALLY_REFUNDED: "secondary",
  REFUNDED: "secondary",
}

export const REFUND_STATUS_LABELS: Record<RefundStatus, string> = {
  REQUESTED: "Solicitată",
  SUCCEEDED: "Reușită",
  FAILED: "Eșuată",
}
