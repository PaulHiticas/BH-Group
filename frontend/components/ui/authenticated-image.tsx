"use client"

import { useAuthenticatedImage } from "@/hooks/use-authenticated-image"
import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"

interface AuthenticatedImageProps {
  path: string
  alt: string
  className?: string
}

export function AuthenticatedImage({ path, alt, className }: AuthenticatedImageProps) {
  const { objectUrl, isLoading, error } = useAuthenticatedImage(path)

  if (isLoading) {
    return <Skeleton className={className} />
  }

  if (error || !objectUrl) {
    return (
      <div className={cn("flex items-center justify-center bg-muted text-xs text-muted-foreground", className)}>
        Eroare
      </div>
    )
  }

  // eslint-disable-next-line @next/next/no-img-element
  return <img src={objectUrl} alt={alt} className={className} />
}
