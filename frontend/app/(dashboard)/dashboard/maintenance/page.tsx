import type { Metadata } from "next"
import { MaintenanceTicketsView } from "./maintenance-tickets-view"

export const metadata: Metadata = {
  title: "Mentenanță",
}

export default function MaintenancePage() {
  return <MaintenanceTicketsView />
}
