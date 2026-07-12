import type { Metadata } from "next"
import { OwnerReservationsView } from "./owner-reservations-view"

export const metadata: Metadata = {
  title: "Rezervările mele",
}

export default function OwnerReservationsPage() {
  return <OwnerReservationsView />
}
