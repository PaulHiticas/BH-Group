import type { Metadata } from "next"
import { PropertiesView } from "./properties-view"

export const metadata: Metadata = {
  title: "Proprietăți",
}

export default function PropertiesPage() {
  return <PropertiesView />
}
