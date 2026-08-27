import {
  Bot,
  Building2,
  Calendar,
  CalendarClock,
  Inbox,
  LayoutDashboard,
  MessageCircle,
  Receipt,
  ScrollText,
  Settings,
  ShieldCheck,
  Sparkles,
  Users,
  UserPlus,
  Wallet,
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
  {
    href: "/dashboard/owner-requests",
    label: "Cereri proprietari",
    icon: Inbox,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR", "SUPPORT_AGENT"],
  },
  {
    href: "/dashboard/assistant-chats",
    label: "Conversații asistent",
    icon: Bot,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR"],
  },
  {
    href: "/dashboard/users",
    label: "Echipă",
    icon: Users,
    roles: ["SUPER_ADMIN", "ADMINISTRATOR"],
  },
  { href: "/dashboard/audit-log", label: "Jurnal audit", icon: ScrollText, roles: ["SUPER_ADMIN"] },
  { href: "/dashboard/gdpr", label: "GDPR", icon: ShieldCheck, roles: ["SUPER_ADMIN"] },
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
    href: "/dashboard/owner/statements",
    label: "Deconturile mele",
    icon: Wallet,
    roles: ["OWNER"],
  },
  {
    href: "/dashboard/owner/threads",
    label: "Contact / Cereri",
    icon: MessageCircle,
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
