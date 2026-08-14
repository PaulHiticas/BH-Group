import dynamic from "next/dynamic"
import { Skeleton } from "@/components/ui/skeleton"
import { StreetViewEmbed } from "@/components/booking/street-view-embed"
import type { PublicPropertyResponse } from "@/lib/api/types"

const LeafletMap = dynamic(() => import("@/components/map/leaflet-map"), {
  ssr: false,
  loading: () => <Skeleton className="h-[420px] w-full rounded-2xl" />,
})

interface PropertyLocationProps {
  property: PublicPropertyResponse
}

/**
 * Respects the property's location privacy exactly as configured server-side:
 * exactLocation=false means only approximate coordinates are ever sent by
 * the API in the first place (rounded server-side), so there is nothing to
 * reconstruct or guess here - this component just renders what it's given
 * and never infers an address from coordinates.
 */
export function PropertyLocation({ property }: PropertyLocationProps) {
  if (property.latitude == null || property.longitude == null) return null

  return (
    <section className="flex flex-col gap-4">
      <h2 className="font-heading text-lg font-semibold tracking-tight">Unde vei sta</h2>

      {property.exactLocation && property.addressLine ? (
        <p className="text-sm text-muted-foreground">
          {property.addressLine}, {property.city}
          {property.county ? `, ${property.county}` : ""}
          {property.country ? `, ${property.country}` : ""}
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
        height={420}
      />

      {property.exactLocation && (
        <StreetViewEmbed
          latitude={property.latitude}
          longitude={property.longitude}
          label={property.name}
        />
      )}
    </section>
  )
}
