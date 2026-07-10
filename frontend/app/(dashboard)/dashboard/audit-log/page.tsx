import type { Metadata } from "next"
import { AuditLogView } from "./audit-log-view"

export const metadata: Metadata = {
  title: "Jurnal de audit",
}

export default function AuditLogPage() {
  return <AuditLogView />
}
