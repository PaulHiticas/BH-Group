"use client"

import { useState } from "react"
import { Search } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { useAuditLogs } from "@/hooks/use-audit-logs"

const ACTION_BADGE_VARIANT: Record<string, "default" | "secondary" | "outline" | "destructive"> = {
  USER_LOGIN_FAILED: "destructive",
  USER_LOCKED_OUT: "destructive",
  USER_LOGIN_SUCCESS: "secondary",
  USER_LOGOUT: "outline",
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("ro-RO", {
    dateStyle: "medium",
    timeStyle: "short",
  })
}

export function AuditLogView() {
  const [actorEmail, setActorEmail] = useState("")
  const [action, setAction] = useState("")
  const [page, setPage] = useState(0)

  const { data, isLoading } = useAuditLogs({
    actorEmail: actorEmail || undefined,
    action: action || undefined,
    page,
  })

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Jurnal de audit</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Evenimente de securitate și acțiuni ale utilizatorilor înregistrate de platformă.
        </p>
      </div>

      <div className="flex flex-wrap gap-3">
        <div className="relative min-w-64 flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Caută după email actor..."
            className="pl-9"
            value={actorEmail}
            onChange={(e) => {
              setActorEmail(e.target.value)
              setPage(0)
            }}
          />
        </div>
        <Input
          placeholder="Filtrează după acțiune (ex: USER_LOGIN_FAILED)"
          className="w-80"
          value={action}
          onChange={(e) => {
            setAction(e.target.value)
            setPage(0)
          }}
        />
      </div>

      {isLoading ? (
        <Skeleton className="h-96 w-full" />
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Niciun eveniment găsit.</p>
        </div>
      ) : (
        <>
          <div className="rounded-lg border border-border/60">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Dată</TableHead>
                  <TableHead>Acțiune</TableHead>
                  <TableHead>Actor</TableHead>
                  <TableHead>IP</TableHead>
                  <TableHead>Descriere</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((log) => (
                  <TableRow key={log.id}>
                    <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                      {formatDateTime(log.createdAt)}
                    </TableCell>
                    <TableCell>
                      <Badge variant={ACTION_BADGE_VARIANT[log.action] ?? "outline"}>
                        {log.action}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-sm">{log.actorEmail ?? "—"}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {log.ipAddress ?? "—"}
                    </TableCell>
                    <TableCell className="max-w-80 truncate text-sm text-muted-foreground">
                      {log.description ?? "—"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
