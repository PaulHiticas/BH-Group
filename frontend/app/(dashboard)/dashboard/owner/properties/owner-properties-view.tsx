"use client"

import { useState } from "react"
import Link from "next/link"
import { MapPin } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { useOwnerProperties } from "@/hooks/use-owner"
import { PROPERTY_STATUS_BADGE_VARIANT, PROPERTY_STATUS_LABELS } from "@/lib/property-labels"

function formatCurrency(value: number, currency: string) {
  return new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 }).format(value) + " " + currency
}

export function OwnerPropertiesView() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useOwnerProperties(page)

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Proprietățile mele</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Proprietățile administrate de BH Group în numele tău.
        </p>
      </div>

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-48 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">
            Nu ai încă nicio proprietate administrată de BH Group.
          </p>
        </div>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {data.content.map((property) => (
              <Link key={property.id} href={`/dashboard/owner/properties/${property.id}`}>
                <Card className="h-full overflow-hidden transition-colors hover:border-primary/40">
                  {property.coverPhotoUrl && (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={property.coverPhotoUrl}
                      alt={property.name}
                      className="h-36 w-full object-cover"
                    />
                  )}
                  <CardContent className="flex flex-col gap-2 pt-4">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-medium">{property.name}</span>
                      <Badge variant={PROPERTY_STATUS_BADGE_VARIANT[property.status]}>
                        {PROPERTY_STATUS_LABELS[property.status]}
                      </Badge>
                    </div>
                    <p className="flex items-center gap-1 text-xs text-muted-foreground">
                      <MapPin className="size-3" />
                      {property.address.city}
                    </p>
                    <div className="mt-2 flex items-center justify-between border-t pt-2 text-sm">
                      <span className="text-muted-foreground">Venit net</span>
                      <span className="font-medium">
                        {formatCurrency(property.netRevenue, property.currency)}
                      </span>
                    </div>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
