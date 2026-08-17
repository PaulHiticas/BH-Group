import Link from "next/link"
import { Building2 } from "lucide-react"
import { ThemeToggle } from "@/components/layout/theme-toggle"
import { SiteBackgroundVideo } from "@/components/marketing/site-background-video"

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="relative flex min-h-screen flex-col overflow-hidden text-white">
      <SiteBackgroundVideo />
      {/* Extra darkening on top of the page-wide video for the white form/text. */}
      <div className="absolute inset-0 bg-gradient-to-b from-black/70 via-black/60 to-black/80" />

      <header className="relative z-10 flex items-center justify-between px-6 py-6 sm:px-10">
        <Link href="/" className="flex items-center gap-2 font-heading text-lg font-semibold tracking-tight text-white">
          <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Building2 className="size-4" />
          </span>
          BH Group
        </Link>
        <ThemeToggle />
      </header>

      <main className="relative z-10 flex flex-1 flex-col items-center justify-center px-6 py-10">
        <div className="w-full max-w-sm">{children}</div>
        <p className="mt-6 max-w-sm text-center text-sm text-white/60">
          Cauți o proprietate de rezervat?{" "}
          <Link href="/book" className="underline underline-offset-2 hover:text-white">
            Caută aici
          </Link>
          , fără cont necesar.
        </p>
      </main>

      <footer className="relative z-10 px-6 py-6 text-center text-xs text-white/50">
        © {new Date().getFullYear()} BH Group. Toate drepturile rezervate.
      </footer>
    </div>
  )
}
