import type { Metadata } from "next"
import { OwnerExpensesView } from "./owner-expenses-view"

export const metadata: Metadata = {
  title: "Cheltuielile mele",
}

export default function OwnerExpensesPage() {
  return <OwnerExpensesView />
}
