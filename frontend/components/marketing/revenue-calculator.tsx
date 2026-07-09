"use client"

import { useMemo, useState } from "react"
import { motion } from "motion/react"
import { Sparkles } from "lucide-react"
import { Reveal } from "@/components/marketing/reveal"
import { Slider } from "@/components/ui/slider"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

const CITY_BASE_PRICE: Record<string, number> = {
  "Cluj-Napoca": 260,
  București: 300,
  Timișoara: 220,
  Brașov: 280,
  Iași: 190,
}

const OCCUPANCY_RATE = 0.72

export function RevenueCalculator() {
  const [bedrooms, setBedrooms] = useState(2)
  const [city, setCity] = useState("Cluj-Napoca")

  const estimate = useMemo(() => {
    const base = CITY_BASE_PRICE[city] ?? 220
    const pricePerNight = base + (bedrooms - 1) * 70
    const monthlyRevenue = Math.round(pricePerNight * 30 * OCCUPANCY_RATE)
    return { pricePerNight, monthlyRevenue }
  }, [bedrooms, city])

  return (
    <section id="calculator" className="mx-auto max-w-6xl px-6 py-24 sm:px-10 sm:py-32">
      <div className="grid gap-12 lg:grid-cols-2 lg:items-center">
        <Reveal>
          <span className="text-sm font-medium text-primary">Calculator de venit</span>
          <h2 className="mt-3 text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
            Cât poți câștiga din proprietatea ta?
          </h2>
          <p className="mt-4 max-w-md text-balance text-muted-foreground">
            O estimare rapidă, bazată pe orașul și dimensiunea proprietății tale. Cifrele finale
            depind de locație exactă, dotări și sezon.
          </p>
        </Reveal>

        <Reveal delay={0.1}>
          <div className="rounded-3xl border border-border/60 bg-card p-8 shadow-sm">
            <div className="flex flex-col gap-6">
              <div>
                <label className="text-sm font-medium">Oraș</label>
                <Select value={city} onValueChange={(value) => value && setCity(value)}>
                  <SelectTrigger className="mt-2 w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.keys(CITY_BASE_PRICE).map((c) => (
                      <SelectItem key={c} value={c}>
                        {c}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <div className="flex items-center justify-between">
                  <label className="text-sm font-medium">Dormitoare</label>
                  <span className="text-sm text-muted-foreground">{bedrooms}</span>
                </div>
                <Slider
                  className="mt-3"
                  min={1}
                  max={5}
                  step={1}
                  value={[bedrooms]}
                  onValueChange={(v) => setBedrooms(Array.isArray(v) ? v[0] : v)}
                />
              </div>

              <div className="mt-2 overflow-hidden rounded-2xl border border-primary/20 bg-primary/5 p-6">
                <div className="flex items-center gap-2 text-sm text-primary">
                  <Sparkles className="size-4" />
                  Estimare venit lunar
                </div>
                <motion.p
                  key={estimate.monthlyRevenue}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
                  className="mt-2 text-4xl font-semibold tracking-tight"
                >
                  {estimate.monthlyRevenue.toLocaleString("ro-RO")} RON
                </motion.p>
                <p className="mt-1 text-xs text-muted-foreground">
                  ~{estimate.pricePerNight} RON / noapte · {Math.round(OCCUPANCY_RATE * 100)}% grad de ocupare
                </p>
              </div>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  )
}
