"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"

const CONSENT_KEY = "bh-cookie-consent"

export function CookieConsent() {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    if (!localStorage.getItem(CONSENT_KEY)) {
      setVisible(true)
    }
  }, [])

  function respond(value: "accepted" | "rejected") {
    localStorage.setItem(CONSENT_KEY, value)
    setVisible(false)
  }

  if (!visible) return null

  return (
    <div className="fixed inset-x-0 bottom-0 z-50 border-t border-border/60 bg-card/95 px-6 py-4 shadow-lg backdrop-blur-sm sm:px-10">
      <div className="mx-auto flex max-w-6xl flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-muted-foreground">
          Folosim cookie-uri strict necesare pentru funcționarea site-ului și, cu
          acordul tău, cookie-uri de analiză. Detalii în{" "}
          <Link href="/confidentialitate" className="text-foreground underline">
            Politica de confidențialitate
          </Link>
          .
        </p>
        <div className="flex shrink-0 gap-2">
          <Button size="sm" variant="outline" onClick={() => respond("rejected")}>
            Doar necesare
          </Button>
          <Button size="sm" onClick={() => respond("accepted")}>
            Accept toate
          </Button>
        </div>
      </div>
    </div>
  )
}
