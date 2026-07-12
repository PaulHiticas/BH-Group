import type { Metadata } from "next"
import { MyMaintenanceTicketsView } from "./my-maintenance-tickets-view"

export const metadata: Metadata = {
  title: "Tichetele mele",
}

export default function MyMaintenanceTicketsPage() {
  return <MyMaintenanceTicketsView />
}
