"use client"

import { useState } from "react"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { useOwnerExpenses } from "@/hooks/use-owner"
import { EXPENSE_CATEGORY_LABELS } from "@/lib/expense-labels"

function formatCurrency(value: number, currency: string) {
  return new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 2 }).format(value) + " " + currency
}

export function OwnerExpensesView() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useOwnerExpenses(page)

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Cheltuielile mele</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Cheltuieli facturate de BH Group pentru proprietățile tale, scăzute din venitul net.
        </p>
      </div>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Nicio cheltuială facturată deocamdată.</p>
        </div>
      ) : (
        <>
          <div className="flex flex-col divide-y divide-border/60 rounded-lg border">
            {data.content.map((expense) => (
              <div key={expense.id} className="flex flex-wrap items-center justify-between gap-3 p-4 text-sm">
                <div>
                  <p className="font-medium">
                    {formatCurrency(expense.amount, expense.currency)}
                    <span className="ml-2 font-normal text-muted-foreground">
                      {EXPENSE_CATEGORY_LABELS[expense.category]}
                    </span>
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {expense.propertyName} · {expense.expenseDate}
                    {expense.vendor ? ` · ${expense.vendor}` : ""}
                  </p>
                  {expense.notes && <p className="mt-1 text-xs text-muted-foreground">{expense.notes}</p>}
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant="secondary">{EXPENSE_CATEGORY_LABELS[expense.category]}</Badge>
                  {expense.receiptUrl && (
                    <a
                      href={expense.receiptUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-xs text-primary underline"
                    >
                      Vezi bon
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
