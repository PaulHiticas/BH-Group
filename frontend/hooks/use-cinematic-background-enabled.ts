"use client"

import { useEffect, useState } from "react"
import { useTheme } from "next-themes"

interface NetworkInformation {
  saveData?: boolean
}

/**
 * True only when the cinematic video background layer should render at all:
 * desktop-sized viewport (matches Tailwind's lg breakpoint, 1024px), no
 * prefers-reduced-motion, no navigator.connection.saveData, AND dark theme -
 * the video was tuned against the dark background; on light mode it just
 * covers the light/blue background instead of complementing it. False on
 * the server and on first client render (matches SSR output, no hydration
 * mismatch - resolvedTheme is also undefined pre-hydration, which correctly
 * fails the isDark check), flips true in an effect once the device+theme
 * qualify, and re-evaluates whenever the theme changes (not just once on
 * mount) so toggling light/dark live shows or hides the video immediately.
 *
 * On mobile/tablet/reduced-motion/save-data/light-mode this stays false, so
 * callers can render the exact same markup the site had before any video
 * background work - original per-page hero image, plain theme background.
 */
export function useCinematicBackgroundEnabled(): boolean {
  const { resolvedTheme } = useTheme()
  const [enabled, setEnabled] = useState(false)

  useEffect(() => {
    const isDesktop = window.matchMedia("(min-width: 1024px)").matches
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    const saveData = (navigator as Navigator & { connection?: NetworkInformation }).connection?.saveData
    const isDark = resolvedTheme === "dark"
    setEnabled(isDesktop && !reduceMotion && !saveData && isDark)
  }, [resolvedTheme])

  return enabled
}
