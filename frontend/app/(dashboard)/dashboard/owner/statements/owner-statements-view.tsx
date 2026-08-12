"use client"

import { useState } from "react"
import Link from "next/link"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { useMyOwnerStatements } from "@/hooks/use-owner-statements"
import type { OwnerStatementStatus } from "@/lib/api/types"

function formatCurrency(value: number, currency: string) {
  return new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 2 }).format(value) + " " + currency
}

const STATUS_LABELS: Record<OwnerStatementStatus, string> = {
  ISSUED: "Emis",
  PAID: "Plătit",
}

export function OwnerStatementsView() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useMyOwnerStatements(page)

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Deconturile mele</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Istoricul deconturilor de plată generate de BH Group pentru proprietățile tale.
        </p>
      </div>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Niciun decont generat încă.</p>
        </div>
      ) : (
        <>
          <div className="flex flex-col divide-y divide-border/60 rounded-lg border">
            {data.content.map((statement) => (
              <Link
                key={statement.id}
                href={`/dashboard/owner/statements/${statement.id}`}
                className="flex flex-wrap items-center justify-between gap-3 p-4 text-sm hover:bg-accent/50"
              >
                <div>
                  <p className="font-medium">
                    {statement.periodStart} → {statement.periodEnd}
                  </p>
                  <p className="text-xs text-muted-foreground">Generat la {statement.createdAt.slice(0, 10)}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="font-medium">{formatCurrency(statement.netPayout, statement.currency)}</span>
                  <Badge variant={statement.status === "PAID" ? "secondary" : "outline"}>
                    {STATUS_LABELS[statement.status]}
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
