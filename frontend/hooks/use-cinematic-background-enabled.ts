"use client"

import { useEffect, useState } from "react"

interface NetworkInformation {
  saveData?: boolean
}

/**
 * True only when the cinematic video background layer should render at all:
 * desktop-sized viewport (matches Tailwind's lg breakpoint, 1024px), no
 * prefers-reduced-motion, no navigator.connection.saveData. False on the
 * server and on first client render (matches SSR output, no hydration
 * mismatch), flips true in an effect if the device qualifies.
 *
 * On mobile/tablet/reduced-motion/save-data this stays false forever, so
 * callers can render the exact same markup the site had before any video
 * background work - original per-page hero image, plain theme background.
 */
export function useCinematicBackgroundEnabled(): boolean {
  const [enabled, setEnabled] = useState(false)

  useEffect(() => {
    const isDesktop = window.matchMedia("(min-width: 1024px)").matches
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    const saveData = (navigator as Navigator & { connection?: NetworkInformation }).connection?.saveData
    setEnabled(isDesktop && !reduceMotion && !saveData)
  }, [])

  return enabled
}
