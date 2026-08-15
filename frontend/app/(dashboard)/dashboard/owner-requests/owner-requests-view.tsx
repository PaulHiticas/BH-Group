"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Badge } from "@/components/ui/badge"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { DataPagination } from "@/components/ui/data-pagination"
import { useOwnerRequests } from "@/hooks/use-owner-threads"
import type { OwnerThreadStatus } from "@/lib/api/owner-threads"

const STATUS_LABELS: Record<OwnerThreadStatus, string> = {
  OPEN: "Deschisă",
  RESOLVED: "Rezolvată",
}

const ALL_STATUS_VALUE = "__all__"

function formatTime(value: string) {
  return new Date(value).toLocaleString("ro-RO", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

export function OwnerRequestsView() {
  const router = useRouter()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<OwnerThreadStatus | typeof ALL_STATUS_VALUE>(ALL_STATUS_VALUE)
  const { data, isLoading } = useOwnerRequests({
    status: status === ALL_STATUS_VALUE ? undefined : status,
    page,
  })

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Cereri proprietari</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Întrebări și cereri primite de la proprietari despre apartamentele lor.
          </p>
        </div>
        <Select
          value={status}
          onValueChange={(value) => {
            setStatus(value as OwnerThreadStatus | typeof ALL_STATUS_VALUE)
            setPage(0)
          }}
        >
          <SelectTrigger className="w-44">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_STATUS_VALUE}>Toate</SelectItem>
            <SelectItem value="OPEN">Deschise</SelectItem>
            <SelectItem value="RESOLVED">Rezolvate</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="rounded-xl border border-border/60 bg-card">
        {isLoading ? (
          <div className="p-6">
            <Skeleton className="h-64 w-full" />
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center text-sm text-muted-foreground">
            Nicio cerere încă.
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Subiect</TableHead>
                <TableHead>Proprietar</TableHead>
                <TableHead>Apartament</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Ultima activitate</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((thread) => (
                <TableRow
                  key={thread.id}
                  className="cursor-pointer"
                  onClick={() => router.push(`/dashboard/owner-requests/${thread.id}`)}
                >
                  <TableCell className="font-medium">{thread.subject}</TableCell>
                  <TableCell>{thread.ownerName}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {thread.propertyName ?? "Cerere generală"}
                  </TableCell>
                  <TableCell>
                    <Badge variant={thread.status === "OPEN" ? "default" : "secondary"}>
                      {STATUS_LABELS[thread.status]}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatTime(thread.lastMessageAt)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      {data && data.totalPages > 1 && (
        <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
      )}
    </div>
  )
}
