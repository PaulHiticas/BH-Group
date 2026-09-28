"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { motion } from "motion/react"
import { MapPin, Search } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { DateRangePicker } from "@/components/booking/date-range-picker"
import { GuestPicker } from "@/components/booking/guest-picker"

export function HomeSearchBar() {
  const router = useRouter()
  const [destination, setDestination] = useState("")
  const [checkIn, setCheckIn] = useState("")
  const [checkOut, setCheckOut] = useState("")
  const [adults, setAdults] = useState(2)
  const [children, setChildren] = useState(0)

  function handleSearch() {
    const params = new URLSearchParams()
    if (destination) params.set("search", destination)
    if (checkIn) params.set("checkIn", checkIn)
    if (checkOut) params.set("checkOut", checkOut)
    // `guests` stays the single total the booking API filters on; adults and
    // children ride along only so the picker can restore itself.
    params.set("guests", String(adults + children))
    params.set("adults", String(adults))
    if (children > 0) params.set("children", String(children))
    router.push(`/book${params.toString() ? `?${params.toString()}` : ""}`)
  }

  return (
    <section className="border-b border-border/60 bg-card py-10">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
        className="mx-auto w-full max-w-4xl px-6 sm:px-10"
      >
        {/* Segments sit in one rounded bar on desktop and stack full-width on
            phones. Dates and guests open as popovers, so neither one reflows
            the page when it opens. */}
        <div className="flex flex-col gap-3 rounded-2xl border border-border/60 bg-background p-3 shadow-sm sm:flex-row sm:items-end sm:gap-2 sm:p-3">
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <span className="px-1 text-xs font-medium text-muted-foreground">
              Destinație
            </span>
            <div className="relative">
              <MapPin className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Oraș sau nume proprietate"
                className="h-11 rounded-xl pl-9"
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
              />
            </div>
          </div>

          <DateRangePicker
            label="Perioadă"
            checkIn={checkIn}
            checkOut={checkOut}
            onChange={(from, to) => {
              setCheckIn(from)
              setCheckOut(to)
            }}
            className="sm:w-56"
          />

          <GuestPicker
            label="Oaspeți"
            adults={adults}
            childrenCount={children}
            onChange={(nextAdults, nextChildren) => {
              setAdults(nextAdults)
              setChildren(nextChildren)
            }}
            className="sm:w-48"
          />

          <Button
            size="lg"
            className="h-11 gap-2 rounded-xl px-6 sm:w-auto"
            onClick={handleSearch}
          >
            <Search className="size-4" />
            Caută
          </Button>
        </div>
      </motion.div>
    </section>
  )
}
