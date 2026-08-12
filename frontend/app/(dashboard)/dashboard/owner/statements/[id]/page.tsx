"use client"

import { use } from "react"
import Link from "next/link"
import { ArrowLeft, Printer } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useMyOwnerStatement } from "@/hooks/use-owner-statements"
import type { OwnerStatementStatus } from "@/lib/api/types"

function formatCurrency(value: number, currency: string) {
  return new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 2 }).format(value) + " " + currency
}

const STATUS_LABELS: Record<OwnerStatementStatus, string> = {
  ISSUED: "Emis",
  PAID: "Plătit",
}

export default function OwnerStatementDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: statement, isLoading } = useMyOwnerStatement(id)

  if (isLoading || !statement) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div className="flex items-center justify-between gap-3 print:hidden">
        <Link
          href="/dashboard/owner/statements"
          className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="size-3.5" />
          Înapoi la deconturile mele
        </Link>
        <Button type="button" variant="outline" size="sm" className="gap-2" onClick={() => window.print()}>
          <Printer className="size-4" />
          Printează / salvează PDF
        </Button>
      </div>

      <div>
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-semibold tracking-tight">
            Decont {statement.periodStart} → {statement.periodEnd}
          </h1>
          <Badge variant={statement.status === "PAID" ? "secondary" : "outline"}>
            {STATUS_LABELS[statement.status]}
          </Badge>
        </div>
        <p className="mt-1 text-sm text-muted-foreground">Proprietar: {statement.ownerName}</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Rezumat</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 text-sm sm:grid-cols-2">
          <div className="flex items-center justify-between rounded-md bg-muted/40 p-3 sm:col-span-2">
            <span className="text-muted-foreground">Venit brut</span>
            <span className="font-medium">{formatCurrency(statement.grossRevenue, statement.currency)}</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Comision BH Group</span>
            <span>-{formatCurrency(statement.commissionAmount, statement.currency)}</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Cheltuieli facturate</span>
            <span>-{formatCurrency(statement.expensesTotal, statement.currency)}</span>
          </div>
          <div className="flex items-center justify-between text-base font-medium sm:col-span-2">
            <span>Net de plată</span>
            <span>{formatCurrency(statement.netPayout, statement.currency)}</span>
          </div>
          {statement.paymentReference && (
            <p className="text-xs text-muted-foreground sm:col-span-2">
              Referință plată: {statement.paymentReference}
              {statement.paidAt ? ` · ${statement.paidAt.slice(0, 10)}` : ""}
            </p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Defalcare pe proprietăți</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Proprietate</TableHead>
                <TableHead className="text-right">Venit brut</TableHead>
                <TableHead className="text-right">Comision</TableHead>
                <TableHead className="text-right">Cheltuieli</TableHead>
                <TableHead className="text-right">Net</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {statement.lines.map((line, i) => (
                <TableRow key={line.propertyId ?? i}>
                  <TableCell>{line.propertyName}</TableCell>
                  <TableCell className="text-right">{formatCurrency(line.grossRevenue, statement.currency)}</TableCell>
                  <TableCell className="text-right">{formatCurrency(line.commissionAmount, statement.currency)}</TableCell>
                  <TableCell className="text-right">{formatCurrency(line.expensesTotal, statement.currency)}</TableCell>
                  <TableCell className="text-right font-medium">{formatCurrency(line.netAmount, statement.currency)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}
