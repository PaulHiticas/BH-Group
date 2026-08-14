"use client"

import { Suspense, useState } from "react"
import dynamic from "next/dynamic"
import Image from "next/image"
import Link from "next/link"
import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { BedDouble, Building2, Clock, MapPin, Users } from "lucide-react"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { AvailabilityCalendar } from "@/components/booking/availability-calendar"
import { PhotoLightbox } from "@/components/booking/photo-lightbox"
import { StreetViewEmbed } from "@/components/booking/street-view-embed"
import { usePublicProperty } from "@/hooks/use-public-booking"
import { FACILITY_ICONS, FACILITY_LABELS, PROPERTY_TYPE_LABELS } from "@/lib/property-labels"
import { cn } from "@/lib/utils"

const LeafletMap = dynamic(() => import("@/components/map/leaflet-map"), {
  ssr: false,
  loading: () => <Skeleton className="h-72 w-full" />,
})

function PropertyDetailInner({ id }: { id: string }) {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()
  const { data: property, isLoading } = usePublicProperty(id)
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null)

  const checkIn = searchParams.get("checkIn")
  const checkOut = searchParams.get("checkOut")
  const rawGuests = Number(searchParams.get("guests"))
  const guests = property
    ? Math.min(Math.max(rawGuests || 1, 1), property.maxGuests)
    : rawGuests || 1

  function updateSelection(next: { checkIn?: string | null; checkOut?: string | null; guests?: number }) {
    const params = new URLSearchParams(searchParams.toString())
    const merged = { checkIn, checkOut, guests, ...next }

    if (merged.checkIn) params.set("checkIn", merged.checkIn)
    else params.delete("checkIn")

    if (merged.checkOut) params.set("checkOut", merged.checkOut)
    else params.delete("checkOut")

    if (merged.guests) params.set("guests", String(merged.guests))
    else params.delete("guests")

    router.replace(`${pathname}?${params.toString()}`, { scroll: false })
  }

  if (isLoading || !property) {
    return (
      <div className="mx-auto flex max-w-4xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  const bookingHref = `/book/${id}/rezerva${searchParams.toString() ? `?${searchParams.toString()}` : ""}`
  const canContinue = !!checkIn && !!checkOut

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
          {property.photos.slice(0, 4).map((photo, index) => {
            const isLast = index === 3 && property.photos.length > 4
            return (
              <button
                key={photo.id}
                type="button"
                onClick={() => setLightboxIndex(index)}
                aria-label={`Vezi galeria foto — fotografia ${index + 1}`}
                className={cn(
                  "group relative aspect-[4/3] overflow-hidden rounded-lg",
                  index === 0 && "col-span-2 row-span-2 aspect-square sm:aspect-[4/3]"
                )}
              >
                <Image
                  src={photo.url}
                  alt={photo.caption || property.name}
                  fill
                  sizes={index === 0 ? "(min-width: 640px) 50vw, 100vw" : "(min-width: 640px) 25vw, 50vw"}
                  priority={index === 0}
                  className="object-cover transition-transform duration-300 group-hover:scale-105"
                />
                {isLast && (
                  <div className="absolute inset-0 flex items-center justify-center bg-black/50 text-sm font-medium text-white">
                    +{property.photos.length - 4} foto
                  </div>
                )}
              </button>
            )
          })}
        </div>
      ) : (
        <div className="flex aspect-[16/9] items-center justify-center rounded-lg bg-muted text-muted-foreground">
          <Building2 className="size-10" />
        </div>
      )}

      {lightboxIndex !== null && (
        <PhotoLightbox
          photos={property.photos}
          index={lightboxIndex}
          onIndexChange={setLightboxIndex}
          onClose={() => setLightboxIndex(null)}
          propertyName={property.name}
        />
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
          <CardContent className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {property.facilities.map((facility) => {
              const Icon = FACILITY_ICONS[facility]
              return (
                <div key={facility} className="flex items-center gap-2.5 text-sm">
                  <Icon className="size-4 shrink-0 text-muted-foreground" />
                  {FACILITY_LABELS[facility]}
                </div>
              )
            })}
          </CardContent>
        </Card>
      )}

      {property.latitude != null && property.longitude != null && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Locație</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {property.exactLocation && property.addressLine ? (
              <p className="text-sm">
                {property.addressLine}, {property.city}
                {property.county ? `, ${property.county}` : ""}
              </p>
            ) : (
              <p className="text-sm text-muted-foreground">
                Locație aproximativă — adresa exactă e trimisă după confirmarea rezervării.
              </p>
            )}
            <LeafletMap
              markers={[
                { id: property.name, lat: property.latitude, lng: property.longitude, label: property.name },
              ]}
              height={320}
            />
            {property.exactLocation && (
              <StreetViewEmbed
                latitude={property.latitude}
                longitude={property.longitude}
                label={property.name}
              />
            )}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Disponibilitate</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-5">
          <div className="flex flex-col gap-1.5 sm:w-56">
            <label className="text-sm font-medium">Oaspeți</label>
            <Select
              value={String(guests)}
              onValueChange={(value) => value && updateSelection({ guests: Number(value) })}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Array.from({ length: property.maxGuests }, (_, i) => i + 1).map((n) => (
                  <SelectItem key={n} value={String(n)}>
                    {n} {n === 1 ? "oaspete" : "oaspeți"}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">Maximum {property.maxGuests} oaspeți pentru această proprietate.</p>
          </div>

          <AvailabilityCalendar
            propertyId={property.id}
            minStayNights={property.minStayNights}
            maxStayNights={property.maxStayNights}
            checkIn={checkIn}
            checkOut={checkOut}
            onSelect={(range) => updateSelection(range)}
          />
        </CardContent>
      </Card>

      {canContinue ? (
        <Link href={bookingHref} className={cn(buttonVariants({ size: "lg" }), "w-full sm:w-auto sm:self-end")}>
          Continuă către cererea de rezervare
        </Link>
      ) : (
        <Button size="lg" disabled className="w-full sm:w-auto sm:self-end">
          Selectează perioada
        </Button>
      )}
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
