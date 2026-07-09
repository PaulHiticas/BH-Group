"use client"

import { use, useState } from "react"
import Link from "next/link"
import {
  BedDouble,
  Bath,
  Clock,
  MapPin,
  Pencil,
  Trash2,
  Users,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { cn } from "@/lib/utils"
import { Skeleton } from "@/components/ui/skeleton"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { PhotoGallery } from "@/components/properties/photo-gallery"
import { DocumentList } from "@/components/properties/document-list"
import { useCurrentUser } from "@/hooks/use-current-user"
import { useDeleteProperty, useProperty } from "@/hooks/use-properties"
import {
  FACILITY_LABELS,
  PROPERTY_STATUS_BADGE_VARIANT,
  PROPERTY_STATUS_LABELS,
} from "@/lib/property-labels"

export default function PropertyDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: property, isLoading } = useProperty(id)
  const { data: user } = useCurrentUser()
  const deleteProperty = useDeleteProperty()
  const [deleteOpen, setDeleteOpen] = useState(false)

  const canManage = user?.role === "SUPER_ADMIN" || user?.role === "ADMINISTRATOR"

  if (isLoading || !property) {
    return (
      <div className="mx-auto flex max-w-5xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">{property.name}</h1>
            <Badge variant={PROPERTY_STATUS_BADGE_VARIANT[property.status]}>
              {PROPERTY_STATUS_LABELS[property.status]}
            </Badge>
          </div>
          <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
            <MapPin className="size-3.5" />
            {property.address.addressLine}, {property.address.city}
          </p>
        </div>

        {canManage && (
          <div className="flex gap-2">
            <Link
              href={`/dashboard/properties/${id}/edit`}
              className={cn(buttonVariants({ variant: "outline" }))}
            >
              <Pencil className="size-4" />
              Editează
            </Link>
            <AlertDialog open={deleteOpen} onOpenChange={setDeleteOpen}>
              <AlertDialogTrigger render={<Button variant="destructive" />}>
                <Trash2 className="size-4" />
                Șterge
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Ștergi această proprietate?</AlertDialogTitle>
                  <AlertDialogDescription>
                    Acțiunea este ireversibilă. Toate pozele și documentele asociate vor fi șterse.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Anulează</AlertDialogCancel>
                  <AlertDialogAction onClick={() => deleteProperty.mutate(id)}>
                    Șterge definitiv
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        )}
      </div>

      <div className="grid gap-4 sm:grid-cols-5">
        <Card>
          <CardContent className="flex items-center gap-2 text-sm">
            <BedDouble className="size-4 text-muted-foreground" />
            {property.bedrooms} dormitoare
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-2 text-sm">
            <Bath className="size-4 text-muted-foreground" />
            {property.bathrooms} băi
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
          <CardContent className="text-sm">
            {property.basePricePerNight != null ? (
              <span className="font-medium">{property.basePricePerNight} RON / noapte</span>
            ) : (
              <span className="text-muted-foreground">Preț nesetat</span>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Poze</CardTitle>
        </CardHeader>
        <CardContent>
          <PhotoGallery propertyId={id} photos={property.photos} canManage={!!canManage} />
        </CardContent>
      </Card>

      {property.description && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Descriere</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="whitespace-pre-line text-sm text-muted-foreground">
              {property.description}
            </p>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Facilități</CardTitle>
        </CardHeader>
        <CardContent>
          {property.facilities.length === 0 ? (
            <p className="text-sm text-muted-foreground">Nicio facilitate specificată.</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {property.facilities.map((facility) => (
                <Badge key={facility} variant="secondary">
                  {FACILITY_LABELS[facility]}
                </Badge>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Documente</CardTitle>
        </CardHeader>
        <CardContent>
          <DocumentList propertyId={id} documents={property.documents} canManage={!!canManage} />
        </CardContent>
      </Card>
    </div>
  )
}
