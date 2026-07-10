"use client"

import { useEffect, useRef, useState } from "react"
import { Loader2, Search } from "lucide-react"
import "leaflet/dist/leaflet.css"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

const ROMANIA_CENTER: [number, number] = [45.9432, 24.9668]
const MARKER_ICON_BASE = "https://unpkg.com/leaflet@1.9.4/dist/images"

interface NominatimResult {
  lat: string
  lon: string
}

export function LocationPicker({
  latitude,
  longitude,
  searchHint,
  onChange,
}: {
  latitude?: number
  longitude?: number
  searchHint?: string
  onChange: (lat: number, lng: number) => void
}) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<import("leaflet").Map | null>(null)
  const markerRef = useRef<import("leaflet").Marker | null>(null)
  const onChangeRef = useRef(onChange)
  onChangeRef.current = onChange

  const [query, setQuery] = useState(searchHint ?? "")
  const [isSearching, setIsSearching] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function init() {
      const L = (await import("leaflet")).default
      if (cancelled || !containerRef.current || mapRef.current) return

      const icon = L.icon({
        iconUrl: `${MARKER_ICON_BASE}/marker-icon.png`,
        iconRetinaUrl: `${MARKER_ICON_BASE}/marker-icon-2x.png`,
        shadowUrl: `${MARKER_ICON_BASE}/marker-shadow.png`,
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41],
      })

      const initialCenter: [number, number] =
        latitude != null && longitude != null ? [latitude, longitude] : ROMANIA_CENTER
      const initialZoom = latitude != null && longitude != null ? 15 : 6

      const map = L.map(containerRef.current).setView(initialCenter, initialZoom)
      mapRef.current = map

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors",
        maxZoom: 19,
      }).addTo(map)

      if (latitude != null && longitude != null) {
        markerRef.current = L.marker([latitude, longitude], { icon, draggable: true }).addTo(map)
        markerRef.current.on("dragend", () => {
          const pos = markerRef.current!.getLatLng()
          onChangeRef.current(pos.lat, pos.lng)
        })
      }

      map.on("click", (e: import("leaflet").LeafletMouseEvent) => {
        placeMarker(L, map, icon, e.latlng.lat, e.latlng.lng)
        onChangeRef.current(e.latlng.lat, e.latlng.lng)
      })
    }

    function placeMarker(
      L: typeof import("leaflet"),
      map: import("leaflet").Map,
      icon: import("leaflet").Icon,
      lat: number,
      lng: number
    ) {
      if (markerRef.current) {
        markerRef.current.setLatLng([lat, lng])
      } else {
        markerRef.current = L.marker([lat, lng], { icon, draggable: true }).addTo(map)
        markerRef.current.on("dragend", () => {
          const pos = markerRef.current!.getLatLng()
          onChangeRef.current(pos.lat, pos.lng)
        })
      }
    }

    init()

    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    return () => {
      mapRef.current?.remove()
      mapRef.current = null
      markerRef.current = null
    }
  }, [])

  async function handleSearch() {
    if (!query.trim()) return
    setIsSearching(true)
    setSearchError(null)
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(query)}`
      )
      if (!res.ok) throw new Error("Căutarea a eșuat")
      const results: NominatimResult[] = await res.json()
      if (results.length === 0) {
        setSearchError("Nu am găsit această adresă. Încearcă să fii mai specific sau plasează pinul manual.")
        return
      }
      const lat = Number(results[0].lat)
      const lng = Number(results[0].lon)

      const L = (await import("leaflet")).default
      const map = mapRef.current
      if (!map) return
      map.setView([lat, lng], 16)

      if (markerRef.current) {
        markerRef.current.setLatLng([lat, lng])
      } else {
        const icon = L.icon({
          iconUrl: `${MARKER_ICON_BASE}/marker-icon.png`,
          iconRetinaUrl: `${MARKER_ICON_BASE}/marker-icon-2x.png`,
          shadowUrl: `${MARKER_ICON_BASE}/marker-shadow.png`,
          iconSize: [25, 41],
          iconAnchor: [12, 41],
          popupAnchor: [1, -34],
          shadowSize: [41, 41],
        })
        markerRef.current = L.marker([lat, lng], { icon, draggable: true }).addTo(map)
        markerRef.current.on("dragend", () => {
          const pos = markerRef.current!.getLatLng()
          onChangeRef.current(pos.lat, pos.lng)
        })
      }
      onChangeRef.current(lat, lng)
    } catch {
      setSearchError("Căutarea a eșuat. Încearcă din nou sau plasează pinul manual.")
    } finally {
      setIsSearching(false)
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex gap-2">
        <Input
          placeholder="Caută adresa (ex: Strada Memorandumului 28, Cluj-Napoca)"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault()
              handleSearch()
            }
          }}
        />
        <Button type="button" variant="outline" onClick={handleSearch} disabled={isSearching}>
          {isSearching ? <Loader2 className="size-4 animate-spin" /> : <Search className="size-4" />}
        </Button>
      </div>
      {searchError && <p className="text-xs text-destructive">{searchError}</p>}
      <div ref={containerRef} className="h-72 w-full overflow-hidden rounded-lg border border-border/60" />
      <p className="text-xs text-muted-foreground">
        {latitude != null && longitude != null
          ? `Poziție selectată: ${latitude.toFixed(5)}, ${longitude.toFixed(5)}`
          : "Caută adresa sau dă click direct pe hartă pentru a plasa pinul."}{" "}
        Poți trage pinul pentru a-l ajusta.
      </p>
    </div>
  )
}
