import Link from "next/link"
import { Building2, Home, Search } from "lucide-react"
import { buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 bg-background px-6 text-center">
      <span className="flex size-14 items-center justify-center rounded-2xl bg-primary/10 text-primary">
        <Building2 className="size-6" />
      </span>
      <div className="flex flex-col gap-2">
        <h1 className="font-heading text-4xl font-semibold tracking-tight">404</h1>
        <p className="text-muted-foreground">
          Pagina pe care o cauți nu există sau a fost mutată.
        </p>
      </div>
      <div className="flex gap-3">
        <Link href="/" className={cn(buttonVariants({ variant: "outline" }), "gap-2")}>
          <Home className="size-4" />
          Acasă
        </Link>
        <Link href="/book" className={cn(buttonVariants(), "gap-2")}>
          <Search className="size-4" />
          Caută cazare
        </Link>
      </div>
    </div>
  )
}
