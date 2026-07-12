"use client"

import { use, useRef } from "react"
import Link from "next/link"
import { ArrowLeft, Upload } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  useMyMaintenanceTicket,
  useUpdateMyTicketStatus,
  useUploadMyTicketPhoto,
} from "@/hooks/use-maintenance-portal"
import {
  MAINTENANCE_CATEGORY_LABELS,
  MAINTENANCE_PRIORITY_BADGE_VARIANT,
  MAINTENANCE_PRIORITY_LABELS,
  MAINTENANCE_STATUS_BADGE_VARIANT,
  MAINTENANCE_STATUS_LABELS,
  nextMaintenanceStatuses,
} from "@/lib/cleaning-labels"

export default function MyMaintenanceTicketDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: ticket, isLoading } = useMyMaintenanceTicket(id)
  const updateStatus = useUpdateMyTicketStatus(id)
  const uploadPhoto = useUploadMyTicketPhoto(id)
  const fileInputRef = useRef<HTMLInputElement>(null)

  if (isLoading || !ticket) {
    return (
      <div className="mx-auto flex max-w-2xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  const nextStatuses = nextMaintenanceStatuses(ticket.status)

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <Link
        href="/dashboard/maintenance-portal/tickets"
        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-3.5" />
        Înapoi la tichetele mele
      </Link>

      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{ticket.title}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {ticket.propertyName} · {MAINTENANCE_CATEGORY_LABELS[ticket.category]}
          </p>
        </div>
        <div className="flex flex-col items-end gap-1.5">
          <Badge variant={MAINTENANCE_PRIORITY_BADGE_VARIANT[ticket.priority]}>
            {MAINTENANCE_PRIORITY_LABELS[ticket.priority]}
          </Badge>
          <Badge variant={MAINTENANCE_STATUS_BADGE_VARIANT[ticket.status]}>
            {MAINTENANCE_STATUS_LABELS[ticket.status]}
          </Badge>
        </div>
      </div>

      {nextStatuses.length > 0 && (
        <div className="flex gap-2">
          {nextStatuses.map((next) => (
            <Button key={next} onClick={() => updateStatus.mutate(next)} disabled={updateStatus.isPending}>
              {MAINTENANCE_STATUS_LABELS[next]}
            </Button>
          ))}
        </div>
      )}

      {ticket.description && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Descriere</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">{ticket.description}</CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Poze</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          {ticket.photos.length > 0 && (
            <div className="grid grid-cols-3 gap-2">
              {ticket.photos.map((photo) => (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  key={photo.id}
                  src={photo.url}
                  alt="poză tichet mentenanță"
                  className="aspect-square w-full rounded-md object-cover"
                />
              ))}
            </div>
          )}
          <Button
            variant="outline"
            size="sm"
            className="self-start"
            disabled={uploadPhoto.isPending}
            onClick={() => fileInputRef.current?.click()}
          >
            <Upload className="size-4" />
            {uploadPhoto.isPending ? "Se încarcă..." : "Adaugă poză"}
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp,image/avif"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0]
              if (file) uploadPhoto.mutate(file)
              e.target.value = ""
            }}
          />
        </CardContent>
      </Card>
    </div>
  )
}
