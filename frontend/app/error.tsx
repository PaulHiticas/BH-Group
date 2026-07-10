"use client"

import { useEffect } from "react"
import Link from "next/link"
import { AlertTriangle, RotateCcw } from "lucide-react"
import { Button, buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    console.error(error)
  }, [error])

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 bg-background px-6 text-center">
      <span className="flex size-14 items-center justify-center rounded-2xl bg-destructive/10 text-destructive">
        <AlertTriangle className="size-6" />
      </span>
      <div className="flex flex-col gap-2">
        <h1 className="font-heading text-2xl font-semibold tracking-tight">
          A apărut o eroare neașteptată
        </h1>
        <p className="text-muted-foreground">
          Ne cerem scuze pentru inconveniență. Poți încerca din nou sau te poți întoarce
          la pagina principală.
        </p>
      </div>
      <div className="flex gap-3">
        <Link href="/" className={cn(buttonVariants({ variant: "outline" }))}>
          Acasă
        </Link>
        <Button className="gap-2" onClick={() => reset()}>
          <RotateCcw className="size-4" />
          Încearcă din nou
        </Button>
      </div>
    </div>
  )
}
