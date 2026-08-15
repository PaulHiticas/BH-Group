import type { Metadata } from "next"
import { OwnerRequestsView } from "./owner-requests-view"

export const metadata: Metadata = {
  title: "Cereri proprietari",
}

export default function OwnerRequestsPage() {
  return <OwnerRequestsView />
}
