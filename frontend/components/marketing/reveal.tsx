"use client"

import { motion, useReducedMotion } from "motion/react"
import type { ReactNode } from "react"

const EASE_CINEMATIC = [0.16, 1, 0.3, 1] as const

interface RevealProps {
  children: ReactNode
  delay?: number
  y?: number
  className?: string
  once?: boolean
}

// Every reveal keeps its opacity fade (that's just content appearing) but
// drops the translate-y motion and shortens the transition to nearly
// instant when the user has asked for reduced motion - per WCAG 2.3.3,
// not just a nicety.
export function Reveal({ children, delay = 0, y = 28, className, once = true }: RevealProps) {
  const reduceMotion = useReducedMotion()
  return (
    <motion.div
      initial={{ opacity: 0, y: reduceMotion ? 0 : y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once, margin: "-80px" }}
      transition={{ duration: reduceMotion ? 0.2 : 0.9, delay: reduceMotion ? 0 : delay, ease: EASE_CINEMATIC }}
      className={className}
    >
      {children}
    </motion.div>
  )
}

interface RevealGroupProps {
  children: ReactNode
  className?: string
  stagger?: number
}

export function RevealGroup({ children, className, stagger = 0.1 }: RevealGroupProps) {
  const reduceMotion = useReducedMotion()
  return (
    <motion.div
      initial="hidden"
      whileInView="show"
      viewport={{ once: true, margin: "-80px" }}
      variants={{
        hidden: {},
        show: { transition: { staggerChildren: reduceMotion ? 0 : stagger } },
      }}
      className={className}
    >
      {children}
    </motion.div>
  )
}

export function RevealItem({ children, className }: { children: ReactNode; className?: string }) {
  const reduceMotion = useReducedMotion()
  const variants = {
    hidden: { opacity: 0, y: reduceMotion ? 0 : 24 },
    show: { opacity: 1, y: 0, transition: { duration: reduceMotion ? 0.2 : 0.7, ease: EASE_CINEMATIC } },
  }
  return (
    <motion.div variants={variants} className={className}>
      {children}
    </motion.div>
  )
}
