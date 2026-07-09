import Link from "next/link"
import { Building2 } from "lucide-react"

export function SiteFooter() {
  return (
    <footer className="border-t border-border/60 bg-card">
      <div className="mx-auto flex max-w-6xl flex-col gap-8 px-6 py-12 sm:px-10 lg:flex-row lg:items-start lg:justify-between">
        <div className="flex flex-col gap-3">
          <div className="flex items-center gap-2 font-heading text-lg font-semibold tracking-tight">
            <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <Building2 className="size-4" />
            </span>
            BH Group
          </div>
          <p className="max-w-xs text-sm text-muted-foreground">
            Administrare premium de proprietăți pentru închirieri pe termen scurt —
            Airbnb, Booking.com și rezervări directe.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-8 sm:grid-cols-3">
          <div>
            <p className="text-sm font-medium">Oaspeți</p>
            <ul className="mt-3 flex flex-col gap-2 text-sm text-muted-foreground">
              <li><Link href="/book" className="hover:text-foreground">Vezi apartamente</Link></li>
              <li><Link href="/#informatii" className="hover:text-foreground">Informații pentru oaspeți</Link></li>
            </ul>
          </div>
          <div>
            <p className="text-sm font-medium">Proprietari</p>
            <ul className="mt-3 flex flex-col gap-2 text-sm text-muted-foreground">
              <li><Link href="/pentru-proprietari" className="hover:text-foreground">Servicii de administrare</Link></li>
              <li><Link href="/pentru-proprietari#calculator" className="hover:text-foreground">Calculator venit</Link></li>
              <li><Link href="/pentru-proprietari#faq" className="hover:text-foreground">Întrebări frecvente</Link></li>
            </ul>
          </div>
          <div>
            <p className="text-sm font-medium">Contact</p>
            <ul className="mt-3 flex flex-col gap-2 text-sm text-muted-foreground">
              <li>contact@bhgroup.io</li>
              <li>+40 700 000 000</li>
            </ul>
          </div>
        </div>
      </div>
      <div className="flex flex-col items-center justify-between gap-2 border-t border-border/60 px-6 py-6 text-center text-xs text-muted-foreground sm:flex-row sm:px-10 sm:text-left">
        <span>© {new Date().getFullYear()} BH Group. Toate drepturile rezervate.</span>
        <Link href="/login" className="text-muted-foreground/70 hover:text-foreground">
          Acces echipă
        </Link>
      </div>
    </footer>
  )
}
