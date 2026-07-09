"use client"

import { Suspense, use, useState } from "react"
import Link from "next/link"
import { useSearchParams } from "next/navigation"
import { BedDouble, Building2, CheckCircle2, Clock, MapPin, Users } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { BookingForm } from "@/components/booking/booking-form"
import { usePublicProperty } from "@/hooks/use-public-booking"
import { FACILITY_LABELS, PROPERTY_TYPE_LABELS } from "@/lib/property-labels"
import { cn } from "@/lib/utils"
import type { PublicReservationResponse } from "@/lib/api/types"

function PropertyDetailContent({ id }: { id: string }) {
  const searchParams = useSearchParams()
  const { data: property, isLoading } = usePublicProperty(id)
  const [confirmedReservation, setConfirmedReservation] = useState<PublicReservationResponse | null>(null)

  if (isLoading || !property) {
    return (
      <div className="mx-auto flex max-w-4xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  if (confirmedReservation) {
    return (
      <div className="mx-auto flex max-w-lg flex-col items-center gap-4 text-center">
        <CheckCircle2 className="size-12 text-emerald-500" />
        <h1 className="text-2xl font-semibold tracking-tight">Cerere de rezervare trimisă!</h1>
        <p className="text-sm text-muted-foreground">
          Am trimis un email de confirmare la <strong>{confirmedReservation.guestEmail}</strong> cu
          toate detaliile și un link pentru a-ți gestiona rezervarea.
        </p>
        <Card className="w-full text-left">
          <CardContent className="flex flex-col gap-2 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Proprietate</span>
              <span className="font-medium">{confirmedReservation.propertyName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Perioadă</span>
              <span className="font-medium">
                {confirmedReservation.checkInDate} → {confirmedReservation.checkOutDate}
              </span>
            </div>
            {confirmedReservation.totalAmount != null && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Sumă totală</span>
                <span className="font-medium">
                  {confirmedReservation.totalAmount} {confirmedReservation.currency}
                </span>
              </div>
            )}
          </CardContent>
        </Card>
        <Link
          href={`/manage-booking/${confirmedReservation.managementToken}`}
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
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div>
        <h1 className="font-heading text-2xl font-semibold tracking-tight">{property.name}</h1>
        <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
          <MapPin className="size-3.5" />
          {property.city}
          {property.county ? `, ${property.county}` : ""}
        </p>
      </div>

      {property.photos.length > 0 ? (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {property.photos.slice(0, 4).map((photo, index) => (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              key={photo.id}
              src={photo.url}
              alt={property.name}
              className={cn(
                "aspect-[4/3] rounded-lg object-cover",
                index === 0 && "col-span-2 row-span-2 aspect-square sm:aspect-[4/3]"
              )}
            />
          ))}
        </div>
      ) : (
        <div className="flex aspect-[16/9] items-center justify-center rounded-lg bg-muted text-muted-foreground">
          <Building2 className="size-10" />
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-4">
        <Card>
          <CardContent className="flex items-center gap-2 text-sm">
            <BedDouble className="size-4 text-muted-foreground" />
            {property.bedrooms} dormitoare
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-2 text-sm">
            <Users className="size-4 text-muted-foreground" />
            max {property.maxGuests} oaspeți
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-2 text-sm">
            <Clock className="size-4 text-muted-foreground" />
            {property.checkInTime.slice(0, 5)} - {property.checkOutTime.slice(0, 5)}
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-sm">{PROPERTY_TYPE_LABELS[property.propertyType]}</CardContent>
        </Card>
      </div>

      {property.description && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Descriere</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="whitespace-pre-line text-sm text-muted-foreground">{property.description}</p>
          </CardContent>
        </Card>
      )}

      {property.facilities.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Facilități</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            {property.facilities.map((facility) => (
              <Badge key={facility} variant="secondary">
                {FACILITY_LABELS[facility]}
              </Badge>
            ))}
          </CardContent>
        </Card>
      )}

      <div>
        <h2 className="mb-4 text-xl font-semibold tracking-tight">Rezervă acum</h2>
        <BookingForm
          property={property}
          defaultCheckIn={searchParams.get("checkIn") ?? undefined}
          defaultCheckOut={searchParams.get("checkOut") ?? undefined}
          defaultGuests={searchParams.get("guests") ? Number(searchParams.get("guests")) : undefined}
          onSuccess={setConfirmedReservation}
        />
      </div>
    </div>
  )
}

export default function PublicPropertyDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)

  return (
    <Suspense fallback={null}>
      <PropertyDetailContent id={id} />
    </Suspense>
  )
}
