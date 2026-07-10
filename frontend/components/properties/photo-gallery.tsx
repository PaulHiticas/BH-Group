"use client"

import { useRef } from "react"
import Image from "next/image"
import { ImagePlus, Star, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import type { PropertyPhotoResponse } from "@/lib/api/types"
import {
  useDeletePropertyPhoto,
  useSetCoverPhoto,
  useUploadPropertyPhoto,
} from "@/hooks/use-properties"

export function PhotoGallery({
  propertyId,
  photos,
  canManage,
}: {
  propertyId: string
  photos: PropertyPhotoResponse[]
  canManage: boolean
}) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const uploadPhoto = useUploadPropertyPhoto(propertyId)
  const deletePhoto = useDeletePropertyPhoto(propertyId)
  const setCoverPhoto = useSetCoverPhoto(propertyId)

  function handleFileSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) {
      uploadPhoto.mutate({ file })
    }
    e.target.value = ""
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {photos.map((photo) => (
          <div key={photo.id} className="group relative aspect-square overflow-hidden rounded-lg bg-muted">
            <Image
              src={photo.url}
              alt={photo.caption ?? ""}
              fill
              sizes="(min-width: 1024px) 25vw, (min-width: 640px) 33vw, 50vw"
              className="object-cover"
            />
            {photo.cover && (
              <span className="absolute left-2 top-2 rounded-full bg-primary px-2 py-0.5 text-xs font-medium text-primary-foreground">
                Copertă
              </span>
            )}
            {canManage && (
              <div className="absolute inset-0 flex items-center justify-center gap-2 bg-black/50 opacity-0 transition-opacity group-hover:opacity-100">
                {!photo.cover && (
                  <Button
                    size="icon-sm"
                    variant="secondary"
                    onClick={() => setCoverPhoto.mutate(photo.id)}
                    aria-label="Setează copertă"
                  >
                    <Star className="size-4" />
                  </Button>
                )}
                <Button
                  size="icon-sm"
                  variant="destructive"
                  onClick={() => deletePhoto.mutate(photo.id)}
                  aria-label="Șterge poza"
                >
                  <Trash2 className="size-4" />
                </Button>
              </div>
            )}
          </div>
        ))}

        {canManage && (
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadPhoto.isPending}
            className="flex aspect-square flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-border text-muted-foreground hover:bg-muted/40"
          >
            <ImagePlus className="size-6" />
            <span className="text-xs">
              {uploadPhoto.isPending ? "Se încarcă..." : "Adaugă poză"}
            </span>
          </button>
        )}
      </div>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/avif"
        className="hidden"
        onChange={handleFileSelected}
      />
      {photos.length === 0 && !canManage && (
        <p className="text-sm text-muted-foreground">Nu există poze încă.</p>
      )}
    </div>
  )
}
