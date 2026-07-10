import { Building2, CalendarClock, LayoutDashboard, Settings, Users, UserPlus } from "lucide-react"
import type { LucideIcon } from "lucide-react"
import type { Role } from "@/lib/api/types"

export interface DashboardNavItem {
  href: string
  label: string
  icon: LucideIcon
  roles?: Role[]
}

export const DASHBOARD_NAV_ITEMS: DashboardNavItem[] = [
  { href: "/dashboard", label: "Panou", icon: LayoutDashboard },
  { href: "/dashboard/properties", label: "Proprietăți", icon: Building2 },
  { href: "/dashboard/reservations", label: "Rezervări", icon: CalendarClock },
  { href: "/dashboard/leads", label: "Lead-uri", icon: UserPlus },
  { href: "/dashboard/users", label: "Echipă", icon: Users, roles: ["SUPER_ADMIN"] },
  { href: "/dashboard/settings", label: "Setări", icon: Settings },
]
