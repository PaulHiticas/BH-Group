import type { Metadata } from "next"
import { OwnerPropertiesView } from "./owner-properties-view"

export const metadata: Metadata = {
  title: "Proprietățile mele",
}

export default function OwnerPropertiesPage() {
  return <OwnerPropertiesView />
}
