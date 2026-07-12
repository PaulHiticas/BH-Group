"use client"

import { useEffect } from "react"
import { usePathname, useRouter } from "next/navigation"
import { useCurrentUser } from "@/hooks/use-current-user"
import { DASHBOARD_NAV_ITEMS } from "@/lib/dashboard-nav"

/**
 * Nav links are already role-filtered, but a role-restricted route is still
 * reachable by typing/bookmarking its URL directly. Without this guard, e.g.
 * a CLEANER hitting /dashboard/properties would render the admin page, whose
 * data hooks call an admin-only endpoint and fail with an unhandled 403.
 * This redirects away before that page's content (and its data fetching)
 * ever mounts.
 */
export function RouteRoleGuard() {
  const pathname = usePathname()
  const router = useRouter()
  const { data: user, isLoading } = useCurrentUser()

  useEffect(() => {
    if (isLoading || !user) return

    const matchingItems = DASHBOARD_NAV_ITEMS.filter(
      (item) => pathname === item.href || pathname.startsWith(`${item.href}/`)
    )
    // Prefer the most specific (longest href) match, e.g. "/dashboard/owner/properties"
    // over a shorter unrelated prefix.
    const match = matchingItems.sort((a, b) => b.href.length - a.href.length)[0]

    if (match?.roles && !match.roles.includes(user.role)) {
      router.replace("/dashboard")
    }
  }, [pathname, user, isLoading, router])

  return null
}
