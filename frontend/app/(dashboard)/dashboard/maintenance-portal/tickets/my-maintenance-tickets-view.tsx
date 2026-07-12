"use client"

import { useState } from "react"
import Link from "next/link"
import { Badge } from "@/components/ui/badge"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { useMyMaintenanceTickets } from "@/hooks/use-maintenance-portal"
import {
  ALL_MAINTENANCE_STATUSES,
  MAINTENANCE_PRIORITY_BADGE_VARIANT,
  MAINTENANCE_PRIORITY_LABELS,
  MAINTENANCE_STATUS_BADGE_VARIANT,
  MAINTENANCE_STATUS_LABELS,
} from "@/lib/cleaning-labels"
import type { MaintenanceStatus } from "@/lib/api/types"

export function MyMaintenanceTicketsView() {
  const [status, setStatus] = useState<MaintenanceStatus | "ALL">("ALL")
  const [page, setPage] = useState(0)

  const { data, isLoading } = useMyMaintenanceTickets(status === "ALL" ? undefined : status, page)

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Tichetele mele</h1>
        <p className="mt-1 text-sm text-muted-foreground">Tichete de mentenanță alocate ție.</p>
      </div>

      <Select
        value={status}
        onValueChange={(v) => {
          setStatus(v as MaintenanceStatus | "ALL")
          setPage(0)
        }}
      >
        <SelectTrigger className="w-48">
          <SelectValue placeholder="Status" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="ALL">Toate statusurile</SelectItem>
          {ALL_MAINTENANCE_STATUSES.map((s) => (
            <SelectItem key={s} value={s}>
              {MAINTENANCE_STATUS_LABELS[s]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Niciun tichet alocat.</p>
        </div>
      ) : (
        <>
          <div className="flex flex-col divide-y divide-border/60 rounded-lg border">
            {data.content.map((ticket) => (
              <Link
                key={ticket.id}
                href={`/dashboard/maintenance-portal/tickets/${ticket.id}`}
                className="flex items-center justify-between gap-3 p-4 text-sm hover:bg-muted/40"
              >
                <div>
                  <p className="font-medium">{ticket.title}</p>
                  <p className="text-xs text-muted-foreground">{ticket.propertyName}</p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant={MAINTENANCE_PRIORITY_BADGE_VARIANT[ticket.priority]}>
                    {MAINTENANCE_PRIORITY_LABELS[ticket.priority]}
                  </Badge>
                  <Badge variant={MAINTENANCE_STATUS_BADGE_VARIANT[ticket.status]}>
                    {MAINTENANCE_STATUS_LABELS[ticket.status]}
                  </Badge>
                </div>
              </Link>
            ))}
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
