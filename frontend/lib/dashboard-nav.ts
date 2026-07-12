import {
  Building2,
  Calendar,
  CalendarClock,
  LayoutDashboard,
  Receipt,
  ScrollText,
  Settings,
  Sparkles,
  Users,
  UserPlus,
  Wrench,
} from "lucide-react"
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
  {
    href: "/dashboard/properties",
    label: "Proprietăți",
    icon: Building2,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR"],
  },
  {
    href: "/dashboard/reservations",
    label: "Rezervări",
    icon: CalendarClock,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR", "ACCOUNTANT", "SUPPORT_AGENT"],
  },
  {
    href: "/dashboard/calendar",
    label: "Calendar",
    icon: Calendar,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR"],
  },
  {
    href: "/dashboard/leads",
    label: "Lead-uri",
    icon: UserPlus,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR"],
  },
  {
    href: "/dashboard/cleaning",
    label: "Curățenie",
    icon: Sparkles,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR"],
  },
  {
    href: "/dashboard/maintenance",
    label: "Mentenanță",
    icon: Wrench,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR"],
  },
  {
    href: "/dashboard/finance",
    label: "Finanțe",
    icon: Receipt,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR", "ACCOUNTANT"],
  },
  { href: "/dashboard/users", label: "Echipă", icon: Users, roles: ["SUPER_ADMIN"] },
  { href: "/dashboard/audit-log", label: "Jurnal audit", icon: ScrollText, roles: ["SUPER_ADMIN"] },
  {
    href: "/dashboard/owner/properties",
    label: "Proprietățile mele",
    icon: Building2,
    roles: ["OWNER"],
  },
  {
    href: "/dashboard/owner/reservations",
    label: "Rezervările mele",
    icon: CalendarClock,
    roles: ["OWNER"],
  },
  {
    href: "/dashboard/owner/expenses",
    label: "Cheltuielile mele",
    icon: Receipt,
    roles: ["OWNER"],
  },
  {
    href: "/dashboard/cleaner/tasks",
    label: "Sarcinile mele",
    icon: Sparkles,
    roles: ["CLEANER"],
  },
  {
    href: "/dashboard/maintenance-portal/tickets",
    label: "Tichetele mele",
    icon: Wrench,
    roles: ["MAINTENANCE"],
  },
  { href: "/dashboard/settings", label: "Setări", icon: Settings },
]
