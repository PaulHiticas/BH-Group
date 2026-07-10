"use client"

import { use } from "react"
import { Skeleton } from "@/components/ui/skeleton"
import { EditReservationForm } from "@/components/reservations/edit-reservation-form"
import { useReservation, useUpdateReservation } from "@/hooks/use-reservations"

export default function EditReservationPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: reservation, isLoading } = useReservation(id)
  const updateReservation = useUpdateReservation(id)

  if (isLoading || !reservation) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Editează rezervare</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {reservation.guestFirstName} {reservation.guestLastName}
        </p>
      </div>

      <EditReservationForm
        reservation={reservation}
        isSubmitting={updateReservation.isPending}
        onSubmit={(values) => updateReservation.mutate(values)}
      />
    </div>
  )
}
