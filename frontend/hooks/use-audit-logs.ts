"use client"

import { useQuery } from "@tanstack/react-query"
import { auditApi, type AuditLogSearchParams } from "@/lib/api/audit"

export function useAuditLogs(params: AuditLogSearchParams) {
  return useQuery({
    queryKey: ["audit-logs", params],
    queryFn: () => auditApi.search(params),
  })
}
