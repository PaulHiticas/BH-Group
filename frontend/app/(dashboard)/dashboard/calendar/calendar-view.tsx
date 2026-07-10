"use client"

import { useMemo, useState } from "react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { ChevronLeft, ChevronRight, Lock } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { CalendarMonth } from "@/components/reservations/calendar-month"
import { reservationsApi } from "@/lib/api/reservations"
import { useProperties } from "@/hooks/use-properties"
import { useCalendar } from "@/hooks/use-reservations"
import { ApiError } from "@/lib/api/types"

const MONTH_LABELS = [
  "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
  "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie",
]

function toIsoDate(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).toISOString().slice(0, 10)
}

function BlockDatesDialog({ propertyId }: { propertyId: string }) {
  const [open, setOpen] = useState(false)
  const [checkInDate, setCheckInDate] = useState("")
  const [checkOutDate, setCheckOutDate] = useState("")
  const [notes, setNotes] = useState("")
  const queryClient = useQueryClient()

  const blockDates = useMutation({
    mutationFn: () =>
      reservationsApi.create({
        propertyId,
        guestFirstName: "Blocat",
        guestLastName: "(mentenanță)",
        checkInDate,
        checkOutDate,
        numberOfGuests: 1,
        source: "MAINTENANCE",
        currency: "RON",
        notes: notes || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["calendar"] })
      toast.success("Perioadă blocată cu succes")
      setOpen(false)
      setCheckInDate("")
      setCheckOutDate("")
      setNotes("")
    },
    onError: (error) => {
      toast.error(error instanceof ApiError ? error.message : "Blocarea perioadei a eșuat")
    },
  })

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={
        <Button type="button" variant="outline" className="gap-2" disabled={!propertyId}>
          <Lock className="size-4" />
          Blochează perioadă
        </Button>
      } />
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Blochează perioadă (mentenanță)</DialogTitle>
          <DialogDescription>
            Marchează zilele ca indisponibile pentru rezervare, fără să creezi o rezervare de
            oaspete.
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1.5">
              <Label>Start</Label>
              <Input type="date" value={checkInDate} onChange={(e) => setCheckInDate(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label>Sfârșit</Label>
              <Input type="date" value={checkOutDate} onChange={(e) => setCheckOutDate(e.target.value)} />
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label>Motiv (opțional)</Label>
            <Textarea rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Curățenie, reparații..." />
          </div>
        </div>
        <DialogFooter>
          <Button
            type="button"
            disabled={!checkInDate || !checkOutDate || blockDates.isPending}
            onClick={() => blockDates.mutate()}
          >
            {blockDates.isPending ? "Se blochează..." : "Blochează"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export function CalendarView() {
  const { data: properties, isLoading: isLoadingProperties } = useProperties({ size: 200 })
  const [propertyId, setPropertyId] = useState("")
  const [month, setMonth] = useState(() => {
    const now = new Date()
    return new Date(now.getFullYear(), now.getMonth(), 1)
  })

  const activePropertyId = propertyId || properties?.content[0]?.id || ""

  const rangeFrom = toIsoDate(new Date(month.getFullYear(), month.getMonth(), 1))
  const rangeTo = toIsoDate(new Date(month.getFullYear(), month.getMonth() + 1, 0))
  const { data: entries, isLoading: isLoadingEntries } = useCalendar(activePropertyId, rangeFrom, rangeTo)

  const monthLabel = useMemo(
    () => `${MONTH_LABELS[month.getMonth()]} ${month.getFullYear()}`,
    [month]
  )

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Calendar rezervări</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Vezi rezervările pe proprietate și blochează perioade pentru mentenanță.
          </p>
        </div>
        <BlockDatesDialog propertyId={activePropertyId} />
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Select value={activePropertyId} onValueChange={(value) => setPropertyId(value ?? "")}>
          <SelectTrigger className="w-72">
            <SelectValue placeholder="Selectează proprietatea" />
          </SelectTrigger>
          <SelectContent>
            {properties?.content.map((property) => (
              <SelectItem key={property.id} value={property.id}>
                {property.name} — {property.city}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex items-center gap-2">
          <Button
            type="button"
            size="icon"
            variant="outline"
            onClick={() => setMonth((m) => new Date(m.getFullYear(), m.getMonth() - 1, 1))}
          >
            <ChevronLeft className="size-4" />
          </Button>
          <span className="min-w-32 text-center text-sm font-medium">{monthLabel}</span>
          <Button
            type="button"
            size="icon"
            variant="outline"
            onClick={() => setMonth((m) => new Date(m.getFullYear(), m.getMonth() + 1, 1))}
          >
            <ChevronRight className="size-4" />
          </Button>
        </div>
      </div>

      {isLoadingProperties || (activePropertyId && isLoadingEntries) ? (
        <Skeleton className="h-96 w-full" />
      ) : !activePropertyId ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Nu există proprietăți create încă.</p>
        </div>
      ) : (
        <CalendarMonth month={month} entries={entries ?? []} />
      )}
    </div>
  )
}
