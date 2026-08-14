"use client"

import { useEffect, useState } from "react"

interface UtmParams {
  utmSource?: string
  utmMedium?: string
  utmCampaign?: string
}

/**
 * Reads utm_source/utm_medium/utm_campaign from the current URL on mount.
 * Uses window.location directly (not next/navigation's useSearchParams) so
 * callers don't need a Suspense boundary just to capture these for a form.
 */
export function useUtmParams(): UtmParams {
  const [utm, setUtm] = useState<UtmParams>({})

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    setUtm({
      utmSource: params.get("utm_source") ?? undefined,
      utmMedium: params.get("utm_medium") ?? undefined,
      utmCampaign: params.get("utm_campaign") ?? undefined,
    })
  }, [])

  return utm
}
