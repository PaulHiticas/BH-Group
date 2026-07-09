"use client"

import { Button } from "@/components/ui/button"
import { useUpdateReservationStatus } from "@/hooks/use-reservations"
import { RESERVATION_STATUS_LABELS, nextReservationStatuses } from "@/lib/reservation-labels"
import type { ReservationResponse } from "@/lib/api/types"

export function ReservationStatusActions({ reservation }: { reservation: ReservationResponse }) {
  const updateStatus = useUpdateReservationStatus()
  const nextStatuses = nextReservationStatuses(reservation.status)

  if (nextStatuses.length === 0) {
    return null
  }

  return (
    <div className="flex flex-wrap gap-2">
      {nextStatuses.map((status) => (
        <Button
          key={status}
          variant={status === "CANCELLED" || status === "NO_SHOW" ? "outline" : "default"}
          size="sm"
          disabled={updateStatus.isPending}
          onClick={() => updateStatus.mutate({ id: reservation.id, status })}
        >
          {RESERVATION_STATUS_LABELS[status]}
        </Button>
      ))}
    </div>
  )
}
