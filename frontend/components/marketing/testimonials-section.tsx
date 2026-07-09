"use client"

import { Star } from "lucide-react"
import { Reveal, RevealGroup, RevealItem } from "@/components/marketing/reveal"

const TESTIMONIALS = [
  {
    quote:
      "Am predat cheile și, de atunci, singurul lucru pe care îl fac e să verific extrasul de cont. Venitul a crescut, iar eu nu mai am nicio bătaie de cap.",
    name: "Ioana R.",
    role: "Proprietară, Cluj-Napoca",
    initials: "IR",
  },
  {
    quote:
      "Comunicarea cu echipa e impecabilă. Orice problemă apare la oaspeți e rezolvată înainte să aflu eu că a existat.",
    name: "Andrei M.",
    role: "Proprietar, București",
    initials: "AM",
  },
  {
    quote:
      "Rapoartele financiare lunare sunt exact ce îmi trebuia — transparență completă, fără să sun pe nimeni.",
    name: "Elena D.",
    role: "Proprietară, Brașov",
    initials: "ED",
  },
]

export function TestimonialsSection() {
  return (
    <section className="border-y border-border/60 bg-muted/30 py-24 sm:py-32">
      <div className="mx-auto max-w-6xl px-6 sm:px-10">
        <Reveal className="max-w-2xl">
          <span className="text-sm font-medium text-primary">Ce spun proprietarii</span>
          <h2 className="mt-3 text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
            Încredere construită rezervare cu rezervare
          </h2>
        </Reveal>

        <RevealGroup stagger={0.12} className="mt-14 grid gap-6 lg:grid-cols-3">
          {TESTIMONIALS.map((t) => (
            <RevealItem
              key={t.name}
              className="flex flex-col justify-between rounded-2xl border border-border/60 bg-card p-7 shadow-sm"
            >
              <div>
                <div className="flex gap-1 text-amber-400">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Star key={i} className="size-4 fill-current" />
                  ))}
                </div>
                <p className="mt-4 text-balance text-sm leading-relaxed text-foreground/90">
                  &ldquo;{t.quote}&rdquo;
                </p>
              </div>
              <div className="mt-6 flex items-center gap-3">
                <span className="flex size-9 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                  {t.initials}
                </span>
                <div>
                  <p className="text-sm font-medium">{t.name}</p>
                  <p className="text-xs text-muted-foreground">{t.role}</p>
                </div>
              </div>
            </RevealItem>
          ))}
        </RevealGroup>
      </div>
    </section>
  )
}
