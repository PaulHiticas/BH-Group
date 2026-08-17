"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { motion } from "motion/react"
import { Calendar, MapPin, Search, Users } from "lucide-react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"

export function HomeSearchBar() {
  const router = useRouter()
  const [destination, setDestination] = useState("")
  const [checkIn, setCheckIn] = useState("")
  const [checkOut, setCheckOut] = useState("")
  const [guests, setGuests] = useState("")

  function handleSearch() {
    const params = new URLSearchParams()
    if (destination) params.set("search", destination)
    if (checkIn) params.set("checkIn", checkIn)
    if (checkOut) params.set("checkOut", checkOut)
    if (guests) params.set("guests", guests)
    router.push(`/book${params.toString() ? `?${params.toString()}` : ""}`)
  }

  return (
    <section className="border-b border-border/60 bg-card py-10">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
        className="mx-auto w-full max-w-5xl px-6 sm:px-10"
      >
        <div className="flex flex-col gap-4 rounded-2xl border border-border/60 bg-background p-5 shadow-sm sm:flex-row sm:items-end sm:gap-3">
          <div className="flex min-w-0 flex-1 flex-col gap-2">
            <label className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground">
              <MapPin className="size-4" />
              Destinație
            </label>
            <Input
              placeholder="Oraș sau nume proprietate"
              value={destination}
              onChange={(e) => setDestination(e.target.value)}
              className="h-11 text-base"
            />
          </div>
          <div className="flex flex-col gap-2">
            <label className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground">
              <Calendar className="size-4" />
              Check-in
            </label>
            <Input
              type="date"
              value={checkIn}
              onChange={(e) => setCheckIn(e.target.value)}
              className="h-11 w-full text-base sm:w-44"
            />
          </div>
          <div className="flex flex-col gap-2">
            <label className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground">
              <Calendar className="size-4" />
              Check-out
            </label>
            <Input
              type="date"
              value={checkOut}
              onChange={(e) => setCheckOut(e.target.value)}
              className="h-11 w-full text-base sm:w-44"
            />
          </div>
          <div className="flex flex-col gap-2">
            <label className="flex items-center gap-1.5 text-sm font-medium text-muted-foreground">
              <Users className="size-4" />
              Oaspeți
            </label>
            <Input
              type="number"
              min={1}
              placeholder="2"
              value={guests}
              onChange={(e) => setGuests(e.target.value)}
              className="h-11 w-full text-base sm:w-24"
            />
          </div>
          <Button size="lg" className="h-11 gap-2 px-6 text-base" onClick={handleSearch}>
            <Search className="size-4" />
            Caută
          </Button>
        </div>
      </motion.div>
    </section>
  )
}
