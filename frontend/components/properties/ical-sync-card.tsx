"use client"

import { useState } from "react"
import { toast } from "sonner"
import { Copy, RefreshCw, Trash2 } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import {
  useAddIcalFeed,
  useDeleteIcalFeed,
  useIcalFeeds,
  useRegenerateExportToken,
  useSyncIcalFeed,
} from "@/hooks/use-ical"
import { RESERVATION_SOURCE_LABELS } from "@/lib/reservation-labels"
import type { ReservationSource } from "@/lib/api/types"

const IMPORT_SOURCES: ReservationSource[] = ["AIRBNB", "BOOKING_COM", "OTHER"]

function formatDateTime(value: string | null) {
  if (!value) return "Niciodată"
  return new Date(value).toLocaleString("ro-RO")
}

export function IcalSyncCard({
  propertyId,
  exportUrl,
}: {
  propertyId: string
  exportUrl: string | null
}) {
  const regenerateToken = useRegenerateExportToken(propertyId)
  const { data: feeds, isLoading } = useIcalFeeds(propertyId)
  const addFeed = useAddIcalFeed(propertyId)
  const syncFeed = useSyncIcalFeed(propertyId)
  const deleteFeed = useDeleteIcalFeed(propertyId)

  const [source, setSource] = useState<ReservationSource>("AIRBNB")
  const [feedUrl, setFeedUrl] = useState("")

  function handleCopy() {
    if (!exportUrl) return
    navigator.clipboard.writeText(exportUrl)
    toast.success("Link copiat")
  }

  function handleAddFeed() {
    if (!feedUrl.trim()) return
    addFeed.mutate({ source, feedUrl: feedUrl.trim() }, { onSuccess: () => setFeedUrl("") })
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <p className="mb-2 text-sm font-medium">Exportă calendarul BH Group către Airbnb/Booking.com</p>
        <p className="mb-2 text-xs text-muted-foreground">
          Adaugă acest link în setările &bdquo;Import calendar&rdquo; din Airbnb sau Booking.com, ca ei
          să nu mai arate disponibile datele deja rezervate prin BH Group.
        </p>
        {exportUrl ? (
          <div className="flex gap-2">
            <Input readOnly value={exportUrl} className="font-mono text-xs" />
            <Button type="button" variant="outline" size="icon" onClick={handleCopy} aria-label="Copiază link">
              <Copy className="size-4" />
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={regenerateToken.isPending}
              onClick={() => regenerateToken.mutate()}
            >
              Regenerează
            </Button>
          </div>
        ) : (
          <Button type="button" variant="outline" size="sm" onClick={() => regenerateToken.mutate()}>
            Generează link de export
          </Button>
        )}
      </div>

      <div>
        <p className="mb-2 text-sm font-medium">Importă calendare din Airbnb/Booking.com</p>
        <p className="mb-2 text-xs text-muted-foreground">
          Adaugă link-ul .ics de export din Airbnb/Booking.com aici — rezervările lor vor bloca automat
          calendarul BH Group (sincronizare orară, sau apasă &bdquo;Sincronizează acum&rdquo;).
        </p>

        {isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : !feeds || feeds.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nicio sursă adăugată încă.</p>
        ) : (
          <div className="mb-3 flex flex-col divide-y divide-border/60 rounded-lg border">
            {feeds.map((feed) => (
              <div key={feed.id} className="flex flex-wrap items-center justify-between gap-2 p-3 text-sm">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{RESERVATION_SOURCE_LABELS[feed.source]}</span>
                    {feed.lastSyncStatus === "SUCCESS" && !feed.lastSyncError && (
                      <Badge variant="secondary">Sincronizat</Badge>
                    )}
                    {feed.lastSyncStatus === "SUCCESS" && feed.lastSyncError && (
                      <Badge variant="outline">Conflict</Badge>
                    )}
                    {feed.lastSyncStatus === "FAILED" && <Badge variant="destructive">Eroare</Badge>}
                    {!feed.lastSyncStatus && <Badge variant="outline">Niciodată sincronizat</Badge>}
                  </div>
                  <p className="truncate text-xs text-muted-foreground">{feed.feedUrl}</p>
                  <p className="text-xs text-muted-foreground">
                    Ultima sincronizare: {formatDateTime(feed.lastSyncedAt)}
                  </p>
                  {feed.lastSyncError && (
                    <p className="text-xs text-destructive">{feed.lastSyncError}</p>
                  )}
                </div>
                <div className="flex items-center gap-1">
                  <Button
                    type="button"
                    size="icon-sm"
                    variant="ghost"
                    disabled={syncFeed.isPending}
                    onClick={() => syncFeed.mutate(feed.id)}
                    aria-label="Sincronizează acum"
                  >
                    <RefreshCw className="size-4" />
                  </Button>
                  <Button
                    type="button"
                    size="icon-sm"
                    variant="ghost"
                    onClick={() => deleteFeed.mutate(feed.id)}
                    aria-label="Elimină sursa"
                  >
                    <Trash2 className="size-4 text-destructive" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="flex flex-wrap items-center gap-2">
          <Select value={source} onValueChange={(v) => setSource(v as ReservationSource)}>
            <SelectTrigger className="w-40">
              <SelectValue>{() => RESERVATION_SOURCE_LABELS[source]}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              {IMPORT_SOURCES.map((s) => (
                <SelectItem key={s} value={s}>
                  {RESERVATION_SOURCE_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Input
            placeholder="https://www.airbnb.com/calendar/ical/....ics"
            value={feedUrl}
            onChange={(e) => setFeedUrl(e.target.value)}
            className="min-w-64 flex-1"
          />
          <Button type="button" variant="outline" size="sm" disabled={!feedUrl.trim() || addFeed.isPending} onClick={handleAddFeed}>
            Adaugă sursă
          </Button>
        </div>
      </div>
    </div>
  )
}
