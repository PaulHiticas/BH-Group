import Image from "next/image"
import Link from "next/link"
import { BedDouble, Building2, MapPin, Users } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import type { PropertySummaryResponse } from "@/lib/api/types"
import {
  PROPERTY_STATUS_BADGE_VARIANT,
  PROPERTY_STATUS_LABELS,
  PROPERTY_TYPE_LABELS,
} from "@/lib/property-labels"

export function PropertyCard({ property }: { property: PropertySummaryResponse }) {
  return (
    <Link href={`/dashboard/properties/${property.id}`}>
      <Card className="overflow-hidden py-0 transition-shadow hover:shadow-md">
        <div className="relative aspect-[4/3] w-full bg-muted">
          {property.coverPhotoUrl ? (
            <Image
              src={property.coverPhotoUrl}
              alt={property.name}
              fill
              sizes="(min-width: 1024px) 25vw, (min-width: 640px) 33vw, 100vw"
              className="object-cover"
            />
          ) : (
            <div className="flex size-full items-center justify-center text-muted-foreground">
              <Building2 className="size-8" />
            </div>
          )}
          <Badge
            variant={PROPERTY_STATUS_BADGE_VARIANT[property.status]}
            className="absolute right-2 top-2"
          >
            {PROPERTY_STATUS_LABELS[property.status]}
          </Badge>
        </div>
        <CardContent className="flex flex-col gap-2 pb-4">
          <div className="flex items-start justify-between gap-2">
            <h3 className="font-medium leading-tight">{property.name}</h3>
          </div>
          <p className="flex items-center gap-1 text-sm text-muted-foreground">
            <MapPin className="size-3.5" />
            {property.city}
          </p>
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            <span className="flex items-center gap-1">
              <BedDouble className="size-3.5" />
              {property.bedrooms}
            </span>
            <span className="flex items-center gap-1">
              <Users className="size-3.5" />
              {property.maxGuests}
            </span>
            <span>{PROPERTY_TYPE_LABELS[property.propertyType]}</span>
          </div>
        </CardContent>
      </Card>
    </Link>
  )
}
