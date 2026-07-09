"use client"

import Link from "next/link"
import { ArrowRight } from "lucide-react"
import { Reveal } from "@/components/marketing/reveal"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export function WelcomeSection() {
  return (
    <section className="mx-auto max-w-6xl px-6 py-24 sm:px-10 sm:py-32">
      <div className="grid gap-10 lg:grid-cols-2 lg:items-center">
        <Reveal className="relative aspect-[4/3] overflow-hidden rounded-3xl">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1200&q=80&auto=format&fit=crop"
            alt="Apartament BH Group"
            className="kb-image size-full object-cover"
          />
        </Reveal>
        <Reveal delay={0.1}>
          <span className="text-sm font-medium text-primary">Bine ai venit la BH Group</span>
          <h2 className="mt-3 text-balance font-heading text-3xl font-semibold tracking-tight sm:text-4xl">
            Locuiește ca un localnic, oriunde te oprești
          </h2>
          <p className="mt-4 text-balance text-muted-foreground">
            Administrăm o colecție curatoriată de apartamente moderne, gata pregătite pentru sejururi
            de business sau de vacanță. Fiecare proprietate e verificată personal de echipa noastră,
            pentru un sejur fără surprize — de la check-in până la check-out.
          </p>
          <Link href="/book" className={cn(buttonVariants({ size: "lg" }), "mt-6 gap-2")}>
            Vezi apartamentele
            <ArrowRight className="size-4" />
          </Link>
        </Reveal>
      </div>
    </section>
  )
}
