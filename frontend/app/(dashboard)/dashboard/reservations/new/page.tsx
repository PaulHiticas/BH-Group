"use client"

import { Suspense } from "react"
import { useSearchParams } from "next/navigation"
import { CreateReservationForm } from "@/components/reservations/create-reservation-form"
import { useCreateReservation } from "@/hooks/use-reservations"
import type { ReservationPayload } from "@/lib/api/reservations"

function NewReservationForm() {
  const searchParams = useSearchParams()
  const defaultPropertyId = searchParams.get("propertyId") ?? undefined
  const createReservation = useCreateReservation()

  return (
    <CreateReservationForm
      defaultPropertyId={defaultPropertyId}
      isSubmitting={createReservation.isPending}
      onSubmit={(values: ReservationPayload) => createReservation.mutate(values)}
    />
  )
}

export default function NewReservationPage() {
  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Adaugă rezervare</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Rezervarea va fi creată cu statusul &quot;Confirmată&quot;.
        </p>
      </div>

      <Suspense fallback={null}>
        <NewReservationForm />
      </Suspense>
    </div>
  )
}
