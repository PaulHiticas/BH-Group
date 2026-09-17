"use client"

import { useEffect, useState } from "react"
import { useTheme } from "next-themes"
import { Moon, Sun } from "lucide-react"
import { Button } from "@/components/ui/button"

/**
 * One button, one click: flips straight between light and dark. The label
 * names the theme you get by pressing it, not the one you are on - it is
 * hidden below sm so the header stays compact on phones.
 */
export function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme()
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const isDark = resolvedTheme === "dark"
  const targetLabel = isDark ? "Light" : "Dark"

  // The resolved theme is unknowable on the server, so anything derived from
  // it would hydrate differently than it rendered. Hold the same footprint
  // until mount instead, so the header does not shift when the label appears.
  if (!mounted) {
    return (
      <Button
        variant="ghost"
        className="relative gap-2"
        aria-label="Comută tema"
        disabled
      >
        <span className="size-4 shrink-0" />
        <span className="hidden w-9 sm:inline" />
      </Button>
    )
  }

  return (
    <Button
      variant="ghost"
      className="relative gap-2"
      aria-label={`Comută tema (${targetLabel})`}
      onClick={() => setTheme(isDark ? "light" : "dark")}
    >
      {/* Both icons are stacked in this box and cross-fade on the `dark`
          class, so the swap animates without waiting on React. */}
      <span className="relative inline-flex size-4 shrink-0">
        <Sun className="absolute inset-0 size-4 scale-100 rotate-0 transition-all dark:scale-0 dark:-rotate-90" />
        <Moon className="absolute inset-0 size-4 scale-0 rotate-90 transition-all dark:scale-100 dark:rotate-0" />
      </span>
      <span className="hidden sm:inline">{targetLabel}</span>
    </Button>
  )
}
