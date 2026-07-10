"use client"

import { Suspense } from "react"
import dynamic from "next/dynamic"
import Image from "next/image"
import Link from "next/link"
import { useSearchParams } from "next/navigation"
import { BedDouble, Building2, Calendar, Clock, MapPin, Users } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { AvailabilityCalendar } from "@/components/booking/availability-calendar"
import { usePublicProperty } from "@/hooks/use-public-booking"
import { FACILITY_LABELS, PROPERTY_TYPE_LABELS } from "@/lib/property-labels"
import { cn } from "@/lib/utils"

const LeafletMap = dynamic(() => import("@/components/map/leaflet-map"), {
  ssr: false,
  loading: () => <Skeleton className="h-72 w-full" />,
})

function PropertyDetailInner({ id }: { id: string }) {
  const searchParams = useSearchParams()
  const { data: property, isLoading } = usePublicProperty(id)

  if (isLoading || !property) {
    return (
      <div className="mx-auto flex max-w-4xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  const bookingHref = `/book/${id}/rezerva${searchParams.toString() ? `?${searchParams.toString()}` : ""}`

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-heading text-2xl font-semibold tracking-tight">{property.name}</h1>
          <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
            <MapPin className="size-3.5" />
            {property.city}
            {property.county ? `, ${property.county}` : ""}
          </p>
        </div>
        <Link href={bookingHref} className={cn(buttonVariants({ size: "lg" }), "gap-2")}>
          <Calendar className="size-4" />
          Rezervă acum
        </Link>
      </div>

      {property.photos.length > 0 ? (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {property.photos.slice(0, 4).map((photo, index) => (
            <div
              key={photo.id}
              className={cn(
                "relative aspect-[4/3] overflow-hidden rounded-lg",
                index === 0 && "col-span-2 row-span-2 aspect-square sm:aspect-[4/3]"
              )}
            >
              <Image
                src={photo.url}
                alt={property.name}
                fill
                sizes={index === 0 ? "(min-width: 640px) 50vw, 100vw" : "(min-width: 640px) 25vw, 50vw"}
                priority={index === 0}
                className="object-cover"
              />
            </div>
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

      {property.latitude != null && property.longitude != null && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Locație</CardTitle>
          </CardHeader>
          <CardContent>
            <LeafletMap
              markers={[
                { id: property.name, lat: property.latitude, lng: property.longitude, label: property.name },
              ]}
              height={320}
            />
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Disponibilitate</CardTitle>
        </CardHeader>
        <CardContent>
          <AvailabilityCalendar propertyId={property.id} />
        </CardContent>
      </Card>

      <Link href={bookingHref} className={cn(buttonVariants({ size: "lg" }), "w-full gap-2 sm:w-auto sm:self-end")}>
        <Calendar className="size-4" />
        Rezervă acum
      </Link>
    </div>
  )
}

export function PropertyDetailContent({ id }: { id: string }) {
  return (
    <Suspense fallback={null}>
      <PropertyDetailInner id={id} />
    </Suspense>
  )
}
