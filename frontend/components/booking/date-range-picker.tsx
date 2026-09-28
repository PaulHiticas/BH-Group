"use client"

import { useEffect, useState } from "react"
import { format, isValid, parse, startOfToday } from "date-fns"
import { ro } from "date-fns/locale"
import { CalendarDays } from "lucide-react"
import type { DateRange } from "react-day-picker"

import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { cn } from "@/lib/utils"

const ISO = "yyyy-MM-dd"

/** The wire format stays exactly what the URL and API already use. */
function toISO(date: Date | undefined) {
  return date ? format(date, ISO) : ""
}

function fromISO(value: string | undefined) {
  if (!value) return undefined
  const parsed = parse(value, ISO, new Date())
  return isValid(parsed) ? parsed : undefined
}

interface DateRangePickerProps {
  /** ISO yyyy-MM-dd, same string the URL carries. */
  checkIn: string
  checkOut: string
  onChange: (checkIn: string, checkOut: string) => void
  className?: string
  /** Renders the label above the trigger, matching the other search segments. */
  label?: string
}

export function DateRangePicker({
  checkIn,
  checkOut,
  onChange,
  className,
  label,
}: DateRangePickerProps) {
  const [open, setOpen] = useState(false)
  const [twoMonths, setTwoMonths] = useState(false)

  // One month on a phone, two side by side once there is room - matching how
  // booking sites show a range.
  useEffect(() => {
    const query = window.matchMedia("(min-width: 640px)")
    const sync = () => setTwoMonths(query.matches)
    sync()
    query.addEventListener("change", sync)
    return () => query.removeEventListener("change", sync)
  }, [])

  // react-day-picker answers the first click with {from, to} both on that day
  // and only widens `to` on the second. Feeding it back a `to: undefined`
  // makes it forget it is mid-range and start over, so the picker keeps the
  // library's own range object and syncs outward from it.
  const [range, setRange] = useState<DateRange | undefined>(() =>
    fromISO(checkIn) ? { from: fromISO(checkIn), to: fromISO(checkOut) } : undefined
  )

  // Re-seed from the props whenever the panel opens, so a range cleared or
  // restored elsewhere (URL, reset) shows up here.
  function handleOpenChange(next: boolean) {
    if (next) {
      setRange(
        fromISO(checkIn) ? { from: fromISO(checkIn), to: fromISO(checkOut) } : undefined
      )
    }
    setOpen(next)
  }

  function handleSelect(next: DateRange | undefined) {
    setRange(next)

    const from = toISO(next?.from)
    const to = toISO(next?.to)

    // A stay is at least one night, so check-out is never the check-in day:
    // until a later day is picked, only check-in is committed.
    if (!from || !to || from === to) {
      onChange(from, "")
      return
    }

    onChange(from, to)
    setOpen(false)
  }

  const summary = (() => {
    const from = fromISO(checkIn)
    const to = fromISO(checkOut)
    if (!from) return "Selectează datele"
    const fmt = (d: Date) => format(d, "d MMM", { locale: ro })
    return to ? `${fmt(from)} – ${fmt(to)}` : `${fmt(from)} – ...`
  })()

  return (
    <div className={cn("flex min-w-0 flex-col gap-1.5", className)}>
      {label && (
        <span className="px-1 text-xs font-medium text-muted-foreground">{label}</span>
      )}
      <Popover open={open} onOpenChange={handleOpenChange}>
        <PopoverTrigger
          className={cn(
            "flex h-11 w-full items-center gap-2 rounded-xl border border-input bg-background px-3 text-left text-sm transition-colors outline-none hover:bg-muted/50 focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 data-popup-open:bg-muted/50"
          )}
        >
          <CalendarDays className="size-4 shrink-0 text-muted-foreground" />
          <span
            className={cn(
              "truncate",
              !fromISO(checkIn) && "text-muted-foreground"
            )}
          >
            {summary}
          </span>
        </PopoverTrigger>
        <PopoverContent align="start" className="p-3">
          <Calendar
            mode="range"
            selected={range}
            onSelect={handleSelect}
            numberOfMonths={twoMonths ? 2 : 1}
            defaultMonth={fromISO(checkIn) ?? startOfToday()}
            // Yesterday and earlier can never be booked.
            disabled={{ before: startOfToday() }}
            // A range spans both endpoints, so 2 days is the shortest real
            // stay: one night.
            min={2}
            autoFocus
          />
          <p className="mt-2 px-1 text-xs text-muted-foreground">
            Alege întâi data de check-in, apoi check-out.
          </p>
        </PopoverContent>
      </Popover>
    </div>
  )
}
