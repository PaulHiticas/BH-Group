"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { useCancellationQuote, useUpdateReservationStatus } from "@/hooks/use-reservations"
import { RESERVATION_STATUS_LABELS, nextReservationStatuses } from "@/lib/reservation-labels"
import type { ReservationResponse } from "@/lib/api/types"

function formatCurrency(value: number, currency: string) {
  return new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 2 }).format(value) + " " + currency
}

export function ReservationStatusActions({ reservation }: { reservation: ReservationResponse }) {
  const updateStatus = useUpdateReservationStatus()
  const nextStatuses = nextReservationStatuses(reservation.status)
  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false)

  const { data: quote } = useCancellationQuote(reservation.id, confirmCancelOpen)

  if (nextStatuses.length === 0) {
    return null
  }

  return (
    <div className="flex flex-wrap gap-2">
      {nextStatuses.map((status) =>
        status === "CANCELLED" ? (
          <AlertDialog key={status} open={confirmCancelOpen} onOpenChange={setConfirmCancelOpen}>
            <Button variant="outline" size="sm" onClick={() => setConfirmCancelOpen(true)}>
              {RESERVATION_STATUS_LABELS[status]}
            </Button>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Anulezi această rezervare?</AlertDialogTitle>
                <AlertDialogDescription>
                  {quote ? (
                    quote.estimatedRefundAmount > 0 ? (
                      <>
                        Conform politicii de anulare a proprietății, se va rambursa automat{" "}
                        <strong>
                          {formatCurrency(quote.estimatedRefundAmount, quote.currency)} ({quote.refundPercent}%)
                        </strong>{" "}
                        din plățile deja înregistrate.
                      </>
                    ) : (
                      "Conform politicii de anulare a proprietății, nu se va rambursa nimic automat."
                    )
                  ) : (
                    "Se calculează suma de rambursat..."
                  )}
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Înapoi</AlertDialogCancel>
                <AlertDialogAction
                  onClick={() => updateStatus.mutate({ id: reservation.id, status })}
                >
                  Da, anulează
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        ) : (
          <Button
            key={status}
            variant={status === "NO_SHOW" ? "outline" : "default"}
            size="sm"
            disabled={updateStatus.isPending}
            onClick={() => updateStatus.mutate({ id: reservation.id, status })}
          >
            {RESERVATION_STATUS_LABELS[status]}
          </Button>
        )
      )}
    </div>
  )
}
