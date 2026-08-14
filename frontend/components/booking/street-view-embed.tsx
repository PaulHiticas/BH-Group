"use client"

import { useState } from "react"
import { Eye } from "lucide-react"
import { Button } from "@/components/ui/button"

interface StreetViewEmbedProps {
  latitude: number
  longitude: number
  label: string
}

/**
 * Google Street View is only ever loaded after an explicit click - never
 * automatically - so a third-party iframe doesn't load (and phone home to
 * Google) just because someone viewed the property page. Renders nothing
 * if no API key is configured, rather than showing a broken embed.
 */
export function StreetViewEmbed({ latitude, longitude, label }: StreetViewEmbedProps) {
  const [consented, setConsented] = useState(false)
  const apiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY

  if (!apiKey) return null

  if (!consented) {
    return (
      <div className="flex flex-col items-start gap-2 rounded-lg border border-dashed border-border/60 p-4">
        <p className="text-sm text-muted-foreground">
          Poți vedea strada din Street View (conținut încărcat de la Google).
        </p>
        <Button type="button" variant="outline" size="sm" className="gap-2" onClick={() => setConsented(true)}>
          <Eye className="size-4" />
          Afișează Street View
        </Button>
      </div>
    )
  }

  const src = `https://www.google.com/maps/embed/v1/streetview?key=${encodeURIComponent(apiKey)}&location=${latitude},${longitude}`

  return (
    <div className="overflow-hidden rounded-lg border border-border/60">
      <iframe
        src={src}
        title={`Street View — ${label}`}
        width="100%"
        height="320"
        style={{ border: 0 }}
        loading="lazy"
        referrerPolicy="no-referrer-when-downgrade"
        allowFullScreen
      />
    </div>
  )
}
