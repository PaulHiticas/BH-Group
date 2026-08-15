import type { Metadata } from "next"
import { OwnerThreadsView } from "./owner-threads-view"

export const metadata: Metadata = {
  title: "Contact / Cereri",
}

export default function OwnerThreadsPage() {
  return <OwnerThreadsView />
}
