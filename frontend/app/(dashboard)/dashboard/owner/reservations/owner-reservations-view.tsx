"use client"

import { useState } from "react"
import { Badge } from "@/components/ui/badge"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { useOwnerReservations } from "@/hooks/use-owner"
import {
  ALL_RESERVATION_STATUSES,
  RESERVATION_STATUS_BADGE_VARIANT,
  RESERVATION_STATUS_LABELS,
} from "@/lib/reservation-labels"
import type { ReservationStatus } from "@/lib/api/types"

function formatCurrency(value: number | null, currency: string) {
  if (value == null) return "—"
  return new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 }).format(value) + " " + currency
}

export function OwnerReservationsView() {
  const [status, setStatus] = useState<ReservationStatus | "ALL">("ALL")
  const [page, setPage] = useState(0)

  const { data, isLoading } = useOwnerReservations({
    status: status === "ALL" ? undefined : status,
    page,
  })

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Rezervările mele</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Rezervări pentru proprietățile tale administrate de BH Group.
        </p>
      </div>

      <Select
        value={status}
        onValueChange={(value) => {
          setStatus(value as ReservationStatus | "ALL")
          setPage(0)
        }}
      >
        <SelectTrigger className="w-48">
          <SelectValue placeholder="Status" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="ALL">Toate statusurile</SelectItem>
          {ALL_RESERVATION_STATUSES.map((s) => (
            <SelectItem key={s} value={s}>
              {RESERVATION_STATUS_LABELS[s]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Nicio rezervare găsită.</p>
        </div>
      ) : (
        <>
          <div className="flex flex-col divide-y divide-border/60 rounded-lg border">
            {data.content.map((reservation) => (
              <div
                key={reservation.id}
                className="flex flex-wrap items-center justify-between gap-2 p-4 text-sm"
              >
                <div>
                  <p className="font-medium">
                    {reservation.guestFirstName} {reservation.guestLastName}
                  </p>
                  <p className="text-xs text-muted-foreground">{reservation.propertyName}</p>
                </div>
                <div className="text-muted-foreground">
                  {reservation.checkInDate} → {reservation.checkOutDate}
                </div>
                <div className="font-medium">
                  {formatCurrency(reservation.totalAmount, reservation.currency)}
                </div>
                <Badge variant={RESERVATION_STATUS_BADGE_VARIANT[reservation.status]}>
                  {RESERVATION_STATUS_LABELS[reservation.status]}
                </Badge>
              </div>
            ))}
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
