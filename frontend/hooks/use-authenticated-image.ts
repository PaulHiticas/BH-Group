import { useEffect, useState } from "react"
import { useAuthStore } from "@/lib/stores/auth-store"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1"

/**
 * Fetches an authenticated (non-public) image endpoint as a blob and exposes
 * it as an object URL, for inline <img> display. Mirrors the auth-header
 * fetch in lib/download-file.ts, but keeps the blob in memory as an object
 * URL instead of triggering a save-as-file download.
 */
export function useAuthenticatedImage(path: string | null) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(!!path)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    if (!path) {
      setObjectUrl(null)
      setIsLoading(false)
      setError(null)
      return
    }

    let cancelled = false
    let currentUrl: string | null = null
    setIsLoading(true)
    setError(null)

    const accessToken = useAuthStore.getState().accessToken
    fetch(`${API_BASE_URL}${path}`, {
      headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
    })
      .then((response) => {
        if (!response.ok) throw new Error("Imaginea nu a putut fi încărcată")
        return response.blob()
      })
      .then((blob) => {
        if (cancelled) return
        currentUrl = URL.createObjectURL(blob)
        setObjectUrl(currentUrl)
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err : new Error("Imaginea nu a putut fi încărcată"))
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
      if (currentUrl) URL.revokeObjectURL(currentUrl)
    }
  }, [path])

  return { objectUrl, isLoading, error }
}
