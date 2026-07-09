import type { Metadata } from "next"
import { ReservationsView } from "./reservations-view"

export const metadata: Metadata = {
  title: "Rezervări",
}

export default function ReservationsPage() {
  return <ReservationsView />
}
