"use client"

import { useRef } from "react"
import Image from "next/image"
import Link from "next/link"
import { motion, useScroll, useTransform } from "motion/react"
import { ArrowRight, Search } from "lucide-react"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

const EASE_CINEMATIC = [0.16, 1, 0.3, 1] as const

export function HeroSection() {
  const sectionRef = useRef<HTMLDivElement>(null)
  const { scrollYProgress } = useScroll({
    target: sectionRef,
    offset: ["start start", "end start"],
  })

  const bgY = useTransform(scrollYProgress, [0, 1], ["0%", "22%"])
  const bgScale = useTransform(scrollYProgress, [0, 1], [1.04, 1.16])
  const contentOpacity = useTransform(scrollYProgress, [0, 0.7], [1, 0])

  return (
    <section
      ref={sectionRef}
      className="relative flex min-h-[92vh] items-end overflow-hidden bg-neutral-950 text-white"
    >
      {/* Background layer — slow continuous Ken Burns + scroll parallax */}
      <motion.div
        style={{ y: bgY, scale: bgScale }}
        className="absolute inset-0"
      >
        <Image
          src="https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=2000&q=80&auto=format&fit=crop"
          alt="Apartament modern administrat de BH Group"
          fill
          sizes="100vw"
          priority
          className="kb-image-loop object-cover"
        />
      </motion.div>

      {/* Depth layer — darkening gradient + grain-like vignette */}
      <div className="absolute inset-0 bg-gradient-to-t from-black via-black/55 to-black/10" />
      <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-black/10 to-black/40" />

      <motion.div
        style={{ opacity: contentOpacity }}
        className="relative z-10 mx-auto flex w-full max-w-6xl flex-col gap-8 px-6 pb-20 pt-40 sm:px-10"
      >
        <motion.span
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: EASE_CINEMATIC }}
          className="w-fit rounded-full border border-white/20 bg-white/10 px-4 py-1.5 text-xs font-medium tracking-wide text-white/80 backdrop-blur-sm"
        >
          Cazare premium în România
        </motion.span>

        <motion.h1
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, delay: 0.1, ease: EASE_CINEMATIC }}
          className="max-w-3xl text-balance font-heading text-5xl font-semibold leading-[1.05] tracking-tight sm:text-6xl lg:text-7xl"
        >
          Locuiește ca un localnic, oriunde te oprești.
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, delay: 0.2, ease: EASE_CINEMATIC }}
          className="max-w-xl text-balance text-lg text-white/75"
        >
          O colecție curatoriată de apartamente moderne, verificate personal de echipa
          noastră — pregătite pentru sejururi de business sau de vacanță.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, delay: 0.3, ease: EASE_CINEMATIC }}
          className="flex flex-wrap items-center gap-3"
        >
          <Link href="/book" className={cn(buttonVariants({ size: "lg" }), "gap-2")}>
            <Search className="size-4" />
            Vezi apartamentele
          </Link>
          <Link
            href="/pentru-proprietari"
            className={cn(
              buttonVariants({ size: "lg", variant: "outline" }),
              "gap-2 border-white/30 bg-white/5 text-white hover:bg-white/15 hover:text-white"
            )}
          >
            Ai o proprietate de listat?
            <ArrowRight className="size-4" />
          </Link>
        </motion.div>
      </motion.div>
    </section>
  )
}
