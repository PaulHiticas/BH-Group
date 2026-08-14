"use client"

import { useEffect } from "react"
import Image from "next/image"
import { ChevronLeft, ChevronRight, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog"

export interface LightboxPhoto {
  id: string
  url: string
  caption: string | null
}

interface PhotoLightboxProps {
  photos: LightboxPhoto[]
  index: number
  onIndexChange: (index: number) => void
  onClose: () => void
  propertyName: string
}

export function PhotoLightbox({ photos, index, onIndexChange, onClose, propertyName }: PhotoLightboxProps) {
  const photo = photos[index]

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "ArrowRight") onIndexChange((index + 1) % photos.length)
      if (event.key === "ArrowLeft") onIndexChange((index - 1 + photos.length) % photos.length)
    }
    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [index, photos.length, onIndexChange])

  if (!photo) return null

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent
        showCloseButton={false}
        className="flex h-[90vh] max-h-[90vh] w-[95vw] max-w-5xl flex-col gap-0 border-none bg-black/95 p-0 sm:rounded-xl"
      >
        <DialogTitle className="sr-only">
          {propertyName} — fotografia {index + 1} din {photos.length}
        </DialogTitle>

        <div className="relative flex-1">
          <Image
            src={photo.url}
            alt={photo.caption || `${propertyName} — fotografia ${index + 1}`}
            fill
            sizes="95vw"
            className="object-contain"
            priority
          />

          <Button
            type="button"
            size="icon"
            variant="ghost"
            className="absolute right-3 top-3 text-white hover:bg-white/10 hover:text-white"
            onClick={onClose}
            aria-label="Închide galeria"
          >
            <X className="size-5" />
          </Button>

          {photos.length > 1 && (
            <>
              <Button
                type="button"
                size="icon"
                variant="ghost"
                className="absolute left-2 top-1/2 -translate-y-1/2 text-white hover:bg-white/10 hover:text-white"
                onClick={() => onIndexChange((index - 1 + photos.length) % photos.length)}
                aria-label="Fotografia anterioară"
              >
                <ChevronLeft className="size-6" />
              </Button>
              <Button
                type="button"
                size="icon"
                variant="ghost"
                className="absolute right-2 top-1/2 -translate-y-1/2 text-white hover:bg-white/10 hover:text-white"
                onClick={() => onIndexChange((index + 1) % photos.length)}
                aria-label="Fotografia următoare"
              >
                <ChevronRight className="size-6" />
              </Button>
            </>
          )}
        </div>

        <div className="flex items-center justify-between gap-3 px-4 py-3 text-xs text-white/70">
          <span>
            {index + 1} / {photos.length}
          </span>
          {photo.caption && <span className="truncate">{photo.caption}</span>}
        </div>
      </DialogContent>
    </Dialog>
  )
}
