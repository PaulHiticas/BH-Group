import type { Metadata } from "next"
import { LeadsView } from "./leads-view"

export const metadata: Metadata = {
  title: "Lead-uri",
}

export default function LeadsPage() {
  return <LeadsView />
}
