"use client"

import { useState } from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { usePublicCalendar } from "@/hooks/use-public-booking"
import { cn } from "@/lib/utils"

const WEEKDAY_LABELS = ["Lun", "Mar", "Mie", "Joi", "Vin", "Sâm", "Dum"]
const MONTH_LABELS = [
  "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
  "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie",
]

function toDateOnly(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function toIsoDate(date: Date) {
  return toDateOnly(date).toISOString().slice(0, 10)
}

function isBooked(day: Date, ranges: { checkInDate: string; checkOutDate: string }[]) {
  return ranges.some((range) => {
    const checkIn = toDateOnly(new Date(range.checkInDate))
    const checkOut = toDateOnly(new Date(range.checkOutDate))
    return day >= checkIn && day < checkOut
  })
}

export function AvailabilityCalendar({ propertyId }: { propertyId: string }) {
  const [month, setMonth] = useState(() => {
    const now = new Date()
    return new Date(now.getFullYear(), now.getMonth(), 1)
  })

  const rangeFrom = toIsoDate(month)
  const rangeTo = toIsoDate(new Date(month.getFullYear(), month.getMonth() + 1, 0))
  const { data: bookedRanges, isLoading } = usePublicCalendar(propertyId, rangeFrom, rangeTo)

  const year = month.getFullYear()
  const monthIndex = month.getMonth()
  const firstOfMonth = new Date(year, monthIndex, 1)
  const startOffset = (firstOfMonth.getDay() + 6) % 7
  const gridStart = new Date(year, monthIndex, 1 - startOffset)
  const days = Array.from({ length: 42 }, (_, i) => {
    const d = new Date(gridStart)
    d.setDate(gridStart.getDate() + i)
    return d
  })

  const today = toDateOnly(new Date())
  const canGoBack = new Date(year, monthIndex, 1) > new Date(today.getFullYear(), today.getMonth(), 1)

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium">
          {MONTH_LABELS[monthIndex]} {year}
        </p>
        <div className="flex gap-1">
          <Button
            type="button"
            size="icon"
            variant="outline"
            className="size-7"
            disabled={!canGoBack}
            onClick={() => setMonth(new Date(year, monthIndex - 1, 1))}
          >
            <ChevronLeft className="size-3.5" />
          </Button>
          <Button
            type="button"
            size="icon"
            variant="outline"
            className="size-7"
            onClick={() => setMonth(new Date(year, monthIndex + 1, 1))}
          >
            <ChevronRight className="size-3.5" />
          </Button>
        </div>
      </div>

      {isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : (
        <div className="overflow-hidden rounded-lg border border-border/60">
          <div className="grid grid-cols-7 border-b border-border/60 bg-muted/40 text-[11px] font-medium text-muted-foreground">
            {WEEKDAY_LABELS.map((label) => (
              <div key={label} className="px-1 py-1.5 text-center">
                {label}
              </div>
            ))}
          </div>
          <div className="grid grid-cols-7">
            {days.map((day) => {
              const isCurrentMonth = day.getMonth() === monthIndex
              const isPast = day < today
              const unavailable = isBooked(day, bookedRanges ?? [])
              return (
                <div
                  key={day.toISOString()}
                  className={cn(
                    "flex h-10 items-center justify-center border-b border-r border-border/60 text-xs last:border-r-0",
                    !isCurrentMonth && "text-muted-foreground/40",
                    isPast && "text-muted-foreground/40",
                    unavailable && isCurrentMonth && !isPast && "bg-muted text-muted-foreground line-through"
                  )}
                >
                  {day.getDate()}
                </div>
              )
            })}
          </div>
        </div>
      )}
      <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <span className="inline-block size-2.5 rounded-sm bg-muted" />
        Zile indisponibile
      </p>
    </div>
  )
}
