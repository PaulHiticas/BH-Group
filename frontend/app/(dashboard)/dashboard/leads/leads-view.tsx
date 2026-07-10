"use client"

import { useState } from "react"
import { toast } from "sonner"
import { Download, Mail, Phone } from "lucide-react"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { useLeads, useMarkLeadContacted } from "@/hooks/use-leads"
import { downloadFile } from "@/lib/download-file"

export function LeadsView() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useLeads(page, 10)
  const markContacted = useMarkLeadContacted()

  async function handleExport() {
    try {
      await downloadFile("/leads/export", "lead-uri.csv")
    } catch {
      toast.error("Exportul a eșuat")
    }
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Lead-uri proprietari</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Persoane interesate să-și listeze proprietatea, primite prin site.
          </p>
        </div>
        <Button type="button" variant="outline" className="gap-2" onClick={handleExport}>
          <Download className="size-4" />
          Export CSV
        </Button>
      </div>

      <div className="rounded-xl border border-border/60 bg-card">
        {isLoading ? (
          <div className="p-6">
            <Skeleton className="h-64 w-full" />
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center text-sm text-muted-foreground">
            Niciun lead încă.
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Nume</TableHead>
                <TableHead>Contact</TableHead>
                <TableHead>Oraș</TableHead>
                <TableHead>Mesaj</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Acțiune</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((lead) => (
                <TableRow key={lead.id}>
                  <TableCell className="font-medium">{lead.fullName}</TableCell>
                  <TableCell>
                    <div className="flex flex-col gap-1 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1">
                        <Mail className="size-3" /> {lead.email}
                      </span>
                      {lead.phone && (
                        <span className="flex items-center gap-1">
                          <Phone className="size-3" /> {lead.phone}
                        </span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">{lead.city ?? "—"}</TableCell>
                  <TableCell className="max-w-64 truncate text-sm text-muted-foreground">
                    {lead.message ?? "—"}
                  </TableCell>
                  <TableCell>
                    <Badge variant={lead.contacted ? "secondary" : "default"}>
                      {lead.contacted ? "Contactat" : "Nou"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={markContacted.isPending}
                      onClick={() =>
                        markContacted.mutate({ id: lead.id, contacted: !lead.contacted })
                      }
                    >
                      {lead.contacted ? "Marchează necontactat" : "Marchează contactat"}
                    </Button>
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
