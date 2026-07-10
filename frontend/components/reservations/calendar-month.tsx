import Link from "next/link"
import { cn } from "@/lib/utils"
import type { CalendarEntryResponse } from "@/lib/api/types"

const WEEKDAY_LABELS = ["Lun", "Mar", "Mie", "Joi", "Vin", "Sâm", "Dum"]

function toDateOnly(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function isWithinStay(day: Date, entry: CalendarEntryResponse) {
  const checkIn = toDateOnly(new Date(entry.checkInDate))
  const checkOut = toDateOnly(new Date(entry.checkOutDate))
  return day >= checkIn && day < checkOut
}

export function CalendarMonth({
  month,
  entries,
}: {
  month: Date
  entries: CalendarEntryResponse[]
}) {
  const year = month.getFullYear()
  const monthIndex = month.getMonth()

  const firstOfMonth = new Date(year, monthIndex, 1)
  const startOffset = (firstOfMonth.getDay() + 6) % 7 // Monday-first offset
  const gridStart = new Date(year, monthIndex, 1 - startOffset)

  const days = Array.from({ length: 42 }, (_, i) => {
    const d = new Date(gridStart)
    d.setDate(gridStart.getDate() + i)
    return d
  })

  const today = toDateOnly(new Date())

  return (
    <div className="overflow-hidden rounded-lg border border-border/60">
      <div className="grid grid-cols-7 border-b border-border/60 bg-muted/40 text-xs font-medium text-muted-foreground">
        {WEEKDAY_LABELS.map((label) => (
          <div key={label} className="px-2 py-2 text-center">
            {label}
          </div>
        ))}
      </div>
      <div className="grid grid-cols-7">
        {days.map((day) => {
          const isCurrentMonth = day.getMonth() === monthIndex
          const isToday = day.getTime() === today.getTime()
          const dayEntries = entries.filter((entry) => isWithinStay(day, entry))

          return (
            <div
              key={day.toISOString()}
              className={cn(
                "flex min-h-24 flex-col gap-1 border-b border-r border-border/60 p-1.5 last:border-r-0",
                !isCurrentMonth && "bg-muted/20 text-muted-foreground"
              )}
            >
              <span
                className={cn(
                  "text-xs",
                  isToday &&
                    "flex size-5 items-center justify-center rounded-full bg-primary font-medium text-primary-foreground"
                )}
              >
                {day.getDate()}
              </span>
              <div className="flex flex-col gap-1">
                {dayEntries.slice(0, 3).map((entry) =>
                  entry.source === "MAINTENANCE" ? (
                    <span
                      key={entry.reservationId}
                      className="truncate rounded bg-muted px-1.5 py-0.5 text-[11px] font-medium text-muted-foreground"
                      title="Mentenanță"
                    >
                      🔧 Mentenanță
                    </span>
                  ) : (
                    <Link
                      key={entry.reservationId}
                      href={`/dashboard/reservations/${entry.reservationId}`}
                      className={cn(
                        "truncate rounded px-1.5 py-0.5 text-[11px] font-medium",
                        entry.status === "CANCELLED" || entry.status === "NO_SHOW"
                          ? "bg-muted text-muted-foreground line-through"
                          : "bg-primary/10 text-primary"
                      )}
                      title={entry.guestFullName}
                    >
                      {entry.guestFullName}
                    </Link>
                  )
                )}
                {dayEntries.length > 3 && (
                  <span className="text-[11px] text-muted-foreground">
                    +{dayEntries.length - 3} altele
                  </span>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
