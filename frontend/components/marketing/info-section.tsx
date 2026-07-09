"use client"

import { Clock, CreditCard, PawPrint, ShieldCheck, Sparkles, Wifi } from "lucide-react"
import { Reveal, RevealGroup, RevealItem } from "@/components/marketing/reveal"

const INFO_ITEMS = [
  {
    icon: Clock,
    title: "Check-in & check-out",
    description: "Check-in de la ora 14:00, check-out până la ora 11:00. Check-in autonom cu smart lock, disponibil non-stop.",
    color: "bg-blue-500/10 text-blue-600 dark:text-blue-400",
  },
  {
    icon: ShieldCheck,
    title: "Politică de anulare",
    description: "Anulare gratuită cu până la 48h înainte de check-in. Detaliile exacte apar la fiecare proprietate, înainte de plată.",
    color: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
  },
  {
    icon: CreditCard,
    title: "Plată securizată",
    description: "Card bancar, procesat securizat. Nu se percepe nimic până la confirmarea rezervării de către gazdă.",
    color: "bg-violet-500/10 text-violet-600 dark:text-violet-400",
  },
  {
    icon: Sparkles,
    title: "Curățenie inclusă",
    description: "Fiecare sejur include curățenie profesională și lenjerie/prosoape curate, fără costuri suplimentare ascunse.",
    color: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
  },
  {
    icon: Wifi,
    title: "Facilități verificate",
    description: "Wifi de mare viteză, bucătărie complet utilată și climatizare — verificate personal înainte de fiecare sejur.",
    color: "bg-cyan-500/10 text-cyan-600 dark:text-cyan-400",
  },
  {
    icon: PawPrint,
    title: "Animale de companie",
    description: "Multe proprietăți acceptă animale de companie — filtrează după această facilitate în pagina de căutare.",
    color: "bg-rose-500/10 text-rose-600 dark:text-rose-400",
  },
]

export function InfoSection() {
  return (
    <section className="border-y border-border/60 bg-muted/30 py-24 sm:py-32">
      <div className="mx-auto max-w-6xl px-6 sm:px-10">
        <Reveal className="max-w-2xl">
          <span className="text-sm font-medium text-primary">Informații pentru oaspeți</span>
          <h2 className="mt-3 text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
            Tot ce trebuie să știi înainte de rezervare
          </h2>
        </Reveal>

        <RevealGroup stagger={0.08} className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {INFO_ITEMS.map((item) => (
            <RevealItem
              key={item.title}
              className="flex flex-col gap-3 rounded-2xl border border-border/60 bg-card p-6 shadow-sm"
            >
              <span className={`flex size-10 items-center justify-center rounded-lg ${item.color}`}>
                <item.icon className="size-5" />
              </span>
              <h3 className="font-medium">{item.title}</h3>
              <p className="text-sm text-muted-foreground">{item.description}</p>
            </RevealItem>
          ))}
        </RevealGroup>
      </div>
    </section>
  )
}
