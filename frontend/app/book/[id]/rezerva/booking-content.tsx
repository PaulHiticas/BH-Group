"use client"

import { Suspense, useState } from "react"
import Link from "next/link"
import { useSearchParams } from "next/navigation"
import { AlertCircle, ArrowLeft, CheckCircle2, CreditCard, Loader2 } from "lucide-react"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { BookingForm } from "@/components/booking/booking-form"
import {
  usePaymentConfig,
  usePublicProperty,
  useStartCardCheckout,
} from "@/hooks/use-public-booking"
import { cn } from "@/lib/utils"
import type { PublicReservationResponse } from "@/lib/api/types"

function ReservationSummary({ reservation }: { reservation: PublicReservationResponse }) {
  return (
    <Card className="w-full text-left">
      <CardContent className="flex flex-col gap-2 text-sm">
        <div className="flex justify-between">
          <span className="text-muted-foreground">Proprietate</span>
          <span className="font-medium">{reservation.propertyName}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">Perioadă</span>
          <span className="font-medium">
            {reservation.checkInDate} → {reservation.checkOutDate}
          </span>
        </div>
        {reservation.totalAmount != null && (
          <div className="flex justify-between">
            <span className="text-muted-foreground">Sumă totală</span>
            <span className="font-medium">
              {reservation.totalAmount} {reservation.currency}
            </span>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function BookingInner({ id }: { id: string }) {
  const searchParams = useSearchParams()
  const { data: property, isLoading } = usePublicProperty(id)
  const { data: paymentConfig, isLoading: paymentConfigLoading } = usePaymentConfig()
  const startCheckout = useStartCardCheckout()
  const [reservation, setReservation] = useState<PublicReservationResponse | null>(null)
  const cardPaymentsEnabled = paymentConfig?.cardPaymentsEnabled ?? false
  // Once checkout has been requested (or the browser is already on its way
  // to Stripe) every pay button stays disabled - no second session.
  const checkoutBusy = startCheckout.isPending || startCheckout.isSuccess

  function handleBookingCreated(created: PublicReservationResponse) {
    setReservation(created)
    if (cardPaymentsEnabled) {
      // Straight to Stripe: the booking is held, and it is confirmed
      // automatically once Stripe reports the payment - no manual approval.
      startCheckout.mutate(created.managementToken)
    }
  }

  if (isLoading || paymentConfigLoading || !property) {
    return (
      <div className="mx-auto flex max-w-2xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  if (reservation && cardPaymentsEnabled) {
    if (startCheckout.isError) {
      return (
        <div className="mx-auto flex max-w-lg flex-col items-center gap-4 text-center">
          <AlertCircle className="size-12 text-amber-500" />
          <h1 className="text-2xl font-semibold tracking-tight">Nu am putut deschide plata</h1>
          <p className="text-sm text-muted-foreground">
            Perioada rămâne reținută pentru tine încă puțin. Reîncearcă plata cu cardul — rezervarea se
            confirmă automat imediat ce plata este confirmată.
          </p>
          <ReservationSummary reservation={reservation} />
          <Button
            className="w-full"
            size="lg"
            disabled={checkoutBusy}
            onClick={() => startCheckout.mutate(reservation.managementToken)}
          >
            <CreditCard className="size-4" />
            {checkoutBusy ? "Se deschide plata..." : "Reîncearcă plata cu cardul"}
          </Button>
          <Link
            href={`/manage-booking/${reservation.managementToken}`}
            className={cn(buttonVariants({ variant: "outline" }), "w-full")}
          >
            Vezi rezervarea
          </Link>
        </div>
      )
    }

    return (
      <div
        className="mx-auto flex max-w-lg flex-col items-center gap-4 text-center"
        role="status"
        aria-live="polite"
      >
        <Loader2 className="size-12 animate-spin text-muted-foreground" />
        <h1 className="text-2xl font-semibold tracking-tight">Te redirecționăm către plata securizată</h1>
        <p className="text-sm text-muted-foreground">
          Plata e procesată de Stripe. Datele cardului nu ajung pe serverele noastre.
        </p>
      </div>
    )
  }

  if (reservation) {
    return (
      <div className="mx-auto flex max-w-lg flex-col items-center gap-4 text-center">
        <CheckCircle2 className="size-12 text-emerald-500" />
        <h1 className="text-2xl font-semibold tracking-tight">Cererea a fost primită și așteaptă confirmarea</h1>
        <p className="text-sm text-muted-foreground">
          Am trimis un email la <strong>{reservation.guestEmail}</strong> cu detaliile cererii și un link
          pentru a o gestiona. Echipa noastră o confirmă manual, iar tu primești un nou email imediat ce e
          aprobată.
        </p>
        <ReservationSummary reservation={reservation} />
        <Link
          href={`/manage-booking/${reservation.managementToken}`}
          className={cn(buttonVariants(), "w-full")}
        >
          Gestionează rezervarea
        </Link>
        <Link href="/book" className="text-sm text-muted-foreground hover:text-foreground">
          Înapoi la căutare
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <div>
        <Link
          href={`/book/${id}`}
          className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="size-3.5" />
          Înapoi la {property.name}
        </Link>
        <h1 className="text-xl font-semibold tracking-tight">Rezervă — {property.name}</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {cardPaymentsEnabled
            ? "După ce completezi datele, reținem perioada pentru tine și te trimitem la plata securizată cu cardul. Rezervarea se confirmă automat imediat ce plata este confirmată."
            : "Plata online cu cardul nu este disponibilă momentan, așa că trimiți o cerere de rezervare — nu e o confirmare instant. Echipa noastră o confirmă manual și primești un email imediat ce e aprobată."}
        </p>
      </div>
      <BookingForm
        property={property}
        defaultCheckIn={searchParams.get("checkIn") ?? undefined}
        defaultCheckOut={searchParams.get("checkOut") ?? undefined}
        defaultGuests={searchParams.get("guests") ? Number(searchParams.get("guests")) : undefined}
        submitLabel={cardPaymentsEnabled ? "Continuă către plată" : "Trimite cererea de rezervare"}
        busy={checkoutBusy}
        onSuccess={handleBookingCreated}
      />
    </div>
  )
}

export function BookingContent({ id }: { id: string }) {
  return (
    <Suspense fallback={null}>
      <BookingInner id={id} />
    </Suspense>
  )
}
