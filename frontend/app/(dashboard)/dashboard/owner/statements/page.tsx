import type { Metadata } from "next"
import { OwnerStatementsView } from "./owner-statements-view"

export const metadata: Metadata = {
  title: "Deconturile mele",
}

export default function OwnerStatementsPage() {
  return <OwnerStatementsView />
}
