"use client"

import { Minus, Plus, Users } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { cn } from "@/lib/utils"

interface GuestPickerProps {
  adults: number
  /** Not named `children`: that prop name is reserved by React. */
  childrenCount: number
  onChange: (adults: number, childrenCount: number) => void
  className?: string
  label?: string
  /** Upper bound for adults + children, when a property caps occupancy. */
  maxGuests?: number
}

function plural(count: number, one: string, many: string) {
  return `${count} ${count === 1 ? one : many}`
}

function Stepper({
  title,
  hint,
  value,
  min,
  onChange,
  canIncrement,
}: {
  title: string
  hint: string
  value: number
  min: number
  onChange: (next: number) => void
  canIncrement: boolean
}) {
  return (
    <div className="flex items-center justify-between gap-6 py-2">
      <div className="min-w-0">
        <p className="text-sm font-medium">{title}</p>
        <p className="text-xs text-muted-foreground">{hint}</p>
      </div>
      <div className="flex shrink-0 items-center gap-1">
        <Button
          type="button"
          variant="outline"
          size="icon-sm"
          aria-label={`Scade ${title.toLowerCase()}`}
          disabled={value <= min}
          onClick={() => onChange(value - 1)}
        >
          <Minus className="size-3.5" />
        </Button>
        <span
          aria-live="polite"
          className="w-6 text-center text-sm font-medium tabular-nums"
        >
          {value}
        </span>
        <Button
          type="button"
          variant="outline"
          size="icon-sm"
          aria-label={`Crește ${title.toLowerCase()}`}
          disabled={!canIncrement}
          onClick={() => onChange(value + 1)}
        >
          <Plus className="size-3.5" />
        </Button>
      </div>
    </div>
  )
}

/**
 * Adults and children are picked separately, but the total is what the
 * search actually filters on - children occupy capacity just like adults, so
 * `guests` stays a single number end to end.
 */
export function GuestPicker({
  adults,
  childrenCount,
  onChange,
  className,
  label,
  maxGuests,
}: GuestPickerProps) {
  const total = adults + childrenCount
  const roomLeft = maxGuests === undefined || total < maxGuests

  const summary =
    childrenCount > 0
      ? `${plural(adults, "adult", "adulți")}, ${plural(childrenCount, "copil", "copii")}`
      : plural(adults, "oaspete", "oaspeți")

  return (
    <div className={cn("flex min-w-0 flex-col gap-1.5", className)}>
      {label && (
        <span className="px-1 text-xs font-medium text-muted-foreground">{label}</span>
      )}
      <Popover>
        <PopoverTrigger className="flex h-11 w-full items-center gap-2 rounded-xl border border-input bg-background px-3 text-left text-sm transition-colors outline-none hover:bg-muted/50 focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 data-popup-open:bg-muted/50">
          <Users className="size-4 shrink-0 text-muted-foreground" />
          <span className="truncate">{summary}</span>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-72 p-4">
          <Stepper
            title="Adulți"
            hint="13 ani sau peste"
            value={adults}
            min={1}
            canIncrement={roomLeft}
            onChange={(next) => onChange(next, childrenCount)}
          />
          <div className="h-px bg-border" />
          <Stepper
            title="Copii"
            hint="0–12 ani"
            value={childrenCount}
            min={0}
            canIncrement={roomLeft}
            onChange={(next) => onChange(adults, next)}
          />
          <p className="mt-3 border-t pt-3 text-xs text-muted-foreground">
            {maxGuests !== undefined && !roomLeft
              ? `Capacitatea maximă este de ${maxGuests} oaspeți.`
              : "Copiii se numără la capacitatea proprietății."}
          </p>
        </PopoverContent>
      </Popover>
    </div>
  )
}
