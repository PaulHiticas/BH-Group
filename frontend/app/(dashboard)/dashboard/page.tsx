import type { Metadata } from "next"
import { DashboardRouter } from "./dashboard-router"

export const metadata: Metadata = {
  title: "Panou",
}

export default function DashboardPage() {
  return <DashboardRouter />
}
