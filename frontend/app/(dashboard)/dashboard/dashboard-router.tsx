"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { Skeleton } from "@/components/ui/skeleton"
import { useCurrentUser } from "@/hooks/use-current-user"
import { DashboardOverview } from "./dashboard-overview"
import { OwnerDashboardOverview } from "./owner-dashboard-overview"

const LOADING = (
  <div className="mx-auto flex max-w-6xl flex-col gap-6">
    <Skeleton className="h-8 w-64" />
    <Skeleton className="h-40 w-full" />
  </div>
)

export function DashboardRouter() {
  const router = useRouter()
  const { data: user, isLoading } = useCurrentUser()

  useEffect(() => {
    if (user?.role === "CLEANER") {
      router.replace("/dashboard/cleaner/tasks")
    } else if (user?.role === "MAINTENANCE") {
      router.replace("/dashboard/maintenance-portal/tickets")
    } else if (user?.role === "ACCOUNTANT") {
      router.replace("/dashboard/finance")
    } else if (user?.role === "SUPPORT_AGENT") {
      router.replace("/dashboard/reservations")
    }
  }, [user?.role, router])

  if (isLoading) {
    return LOADING
  }

  if (user?.role === "OWNER") {
    return <OwnerDashboardOverview />
  }

  if (
    user?.role === "CLEANER" ||
    user?.role === "MAINTENANCE" ||
    user?.role === "ACCOUNTANT" ||
    user?.role === "SUPPORT_AGENT"
  ) {
    return LOADING
  }

  return <DashboardOverview />
}
