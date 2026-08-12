import type { Metadata } from "next"
import { GdprView } from "./gdpr-view"

export const metadata: Metadata = {
  title: "GDPR",
}

export default function GdprPage() {
  return <GdprView />
}
