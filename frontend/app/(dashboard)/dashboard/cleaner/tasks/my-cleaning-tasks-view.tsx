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
import { useMyCleaningTasks } from "@/hooks/use-cleaner"
import {
  ALL_CLEANING_TASK_STATUSES,
  CLEANING_TASK_STATUS_BADGE_VARIANT,
  CLEANING_TASK_STATUS_LABELS,
} from "@/lib/cleaning-labels"
import type { CleaningTaskStatus } from "@/lib/api/types"

export function MyCleaningTasksView() {
  const [status, setStatus] = useState<CleaningTaskStatus | "ALL">("ALL")
  const [page, setPage] = useState(0)

  const { data, isLoading } = useMyCleaningTasks(status === "ALL" ? undefined : status, page)

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Sarcinile mele</h1>
        <p className="mt-1 text-sm text-muted-foreground">Task-urile de curățenie alocate ție.</p>
      </div>

      <Select
        value={status}
        onValueChange={(v) => {
          setStatus(v as CleaningTaskStatus | "ALL")
          setPage(0)
        }}
      >
        <SelectTrigger className="w-48">
          <SelectValue placeholder="Status" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="ALL">Toate statusurile</SelectItem>
          {ALL_CLEANING_TASK_STATUSES.map((s) => (
            <SelectItem key={s} value={s}>
              {CLEANING_TASK_STATUS_LABELS[s]}
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
          <p className="text-sm text-muted-foreground">Nicio sarcină alocată.</p>
        </div>
      ) : (
        <>
          <div className="flex flex-col divide-y divide-border/60 rounded-lg border">
            {data.content.map((task) => {
              const checklistEntries = Object.entries(task.checklistResults)
              const checkedCount = checklistEntries.filter(([, checked]) => checked).length
              return (
                <Link
                  key={task.id}
                  href={`/dashboard/cleaner/tasks/${task.id}`}
                  className="flex items-center justify-between gap-3 p-4 text-sm hover:bg-muted/40"
                >
                  <div>
                    <p className="font-medium">{task.propertyName}</p>
                    <p className="text-xs text-muted-foreground">
                      {task.scheduledDate}
                      {checklistEntries.length > 0 && ` · ${checkedCount}/${checklistEntries.length} checklist`}
                    </p>
                  </div>
                  <Badge variant={CLEANING_TASK_STATUS_BADGE_VARIANT[task.status]}>
                    {CLEANING_TASK_STATUS_LABELS[task.status]}
                  </Badge>
                </Link>
              )
            })}
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
