import type { Metadata } from "next"
import { SettingsView } from "./settings-view"

export const metadata: Metadata = {
  title: "Setări",
}

export default function SettingsPage() {
  return <SettingsView />
}
