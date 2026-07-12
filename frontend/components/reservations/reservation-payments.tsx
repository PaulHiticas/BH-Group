"use client"

import { useState } from "react"
import { Undo2 } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { useRecordManualPayment, useRefundPayment, useReservationPayments } from "@/hooks/use-payments"
import { ALL_PAYMENT_METHODS, PAYMENT_METHOD_LABELS, PAYMENT_STATUS_BADGE_VARIANT, PAYMENT_STATUS_LABELS } from "@/lib/payment-labels"
import type { PaymentMethod } from "@/lib/api/types"

function formatCurrency(value: number, currency: string) {
  return new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 2 }).format(value) + " " + currency
}

export function ReservationPayments({
  reservationId,
  totalAmount,
  currency,
  canManage,
}: {
  reservationId: string
  totalAmount: number | null
  currency: string
  canManage: boolean
}) {
  const { data: payments, isLoading } = useReservationPayments(reservationId)
  const recordPayment = useRecordManualPayment(reservationId)
  const refundPayment = useRefundPayment(reservationId)

  const [amount, setAmount] = useState("")
  const [method, setMethod] = useState<PaymentMethod>("BANK_TRANSFER")
  const [notes, setNotes] = useState("")
  const [refundingId, setRefundingId] = useState<string | null>(null)
  const [refundAmount, setRefundAmount] = useState("")

  const netPaid = (payments ?? []).reduce((sum, p) => {
    if (p.status === "SUCCEEDED" || p.status === "PARTIALLY_REFUNDED") {
      return sum + (p.amount - p.refundedAmount)
    }
    return sum
  }, 0)
  const balanceDue = totalAmount != null ? Math.max(0, totalAmount - netPaid) : null

  function handleRecordPayment() {
    const parsed = Number(amount)
    if (!parsed || parsed <= 0) return
    recordPayment.mutate(
      { amount: parsed, method, notes: notes || undefined },
      { onSuccess: () => { setAmount(""); setNotes("") } }
    )
  }

  function handleRefund(paymentId: string) {
    const parsed = Number(refundAmount)
    if (!parsed || parsed <= 0) return
    refundPayment.mutate(
      { paymentId, payload: { amount: parsed } },
      { onSuccess: () => { setRefundingId(null); setRefundAmount("") } }
    )
  }

  if (isLoading) {
    return <Skeleton className="h-32 w-full" />
  }

  return (
    <div className="flex flex-col gap-4">
      {totalAmount != null && (
        <div className="grid grid-cols-3 gap-3 rounded-md bg-muted/40 p-3 text-sm">
          <div>
            <p className="text-xs text-muted-foreground">Total</p>
            <p className="font-medium">{formatCurrency(totalAmount, currency)}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Încasat</p>
            <p className="font-medium">{formatCurrency(netPaid, currency)}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Rest de plată</p>
            <p className={`font-medium ${balanceDue && balanceDue > 0 ? "text-destructive" : ""}`}>
              {formatCurrency(balanceDue ?? 0, currency)}
            </p>
          </div>
        </div>
      )}

      {!payments || payments.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nicio plată înregistrată.</p>
      ) : (
        <div className="flex flex-col divide-y divide-border/60">
          {payments.map((payment) => (
            <div key={payment.id} className="flex flex-col gap-2 py-2.5 text-sm">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <span className="font-medium">{formatCurrency(payment.amount, payment.currency)}</span>
                  <span className="ml-2 text-xs text-muted-foreground">
                    {PAYMENT_METHOD_LABELS[payment.method]} · {new Date(payment.createdAt).toLocaleDateString("ro-RO")}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  {payment.refundedAmount > 0 && (
                    <span className="text-xs text-muted-foreground">
                      -{formatCurrency(payment.refundedAmount, payment.currency)} rambursat
                    </span>
                  )}
                  <Badge variant={PAYMENT_STATUS_BADGE_VARIANT[payment.status]}>
                    {PAYMENT_STATUS_LABELS[payment.status]}
                  </Badge>
                  {canManage && (payment.status === "SUCCEEDED" || payment.status === "PARTIALLY_REFUNDED") && (
                    <Button
                      size="icon-sm"
                      variant="ghost"
                      aria-label="Rambursează"
                      onClick={() => setRefundingId(refundingId === payment.id ? null : payment.id)}
                    >
                      <Undo2 className="size-4" />
                    </Button>
                  )}
                </div>
              </div>
              {refundingId === payment.id && (
                <div className="flex gap-2">
                  <Input
                    type="number"
                    min={0.01}
                    step="0.01"
                    max={payment.amount - payment.refundedAmount}
                    placeholder={`Max ${(payment.amount - payment.refundedAmount).toFixed(2)}`}
                    value={refundAmount}
                    onChange={(e) => setRefundAmount(e.target.value)}
                    className="max-w-40"
                  />
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={refundPayment.isPending}
                    onClick={() => handleRefund(payment.id)}
                  >
                    Confirmă rambursarea
                  </Button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {canManage && (
        <div className="flex flex-wrap items-center gap-2 border-t pt-3">
          <Input
            type="number"
            min={0.01}
            step="0.01"
            placeholder="Sumă"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="w-32"
          />
          <Select value={method} onValueChange={(v) => v && setMethod(v as PaymentMethod)}>
            <SelectTrigger className="w-40">
              <SelectValue>{() => PAYMENT_METHOD_LABELS[method]}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              {ALL_PAYMENT_METHODS.map((m) => (
                <SelectItem key={m} value={m}>
                  {PAYMENT_METHOD_LABELS[m]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Input
            placeholder="Notă (opțional)"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="w-48"
          />
          <Button size="sm" disabled={!amount || recordPayment.isPending} onClick={handleRecordPayment}>
            Înregistrează plată
          </Button>
        </div>
      )}
    </div>
  )
}
