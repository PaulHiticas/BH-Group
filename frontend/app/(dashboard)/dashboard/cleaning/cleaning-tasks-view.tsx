"use client"

import { useState } from "react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { DataPagination } from "@/components/ui/data-pagination"
import { useCleaningTasks, useAssignCleaningTask, useUpdateCleaningTaskStatus } from "@/hooks/use-cleaning-tasks"
import { useUsers } from "@/hooks/use-users"
import {
  ALL_CLEANING_TASK_STATUSES,
  CLEANING_TASK_STATUS_BADGE_VARIANT,
  CLEANING_TASK_STATUS_LABELS,
  nextCleaningTaskStatuses,
} from "@/lib/cleaning-labels"
import type { CleaningTaskStatus } from "@/lib/api/types"

export function CleaningTasksView() {
  const [status, setStatus] = useState<CleaningTaskStatus | "ALL">("ALL")
  const [page, setPage] = useState(0)

  const { data, isLoading } = useCleaningTasks({
    status: status === "ALL" ? undefined : status,
    page,
  })
  const { data: cleaners } = useUsers({ role: "CLEANER", size: 100 })
  const assignCleaner = useAssignCleaningTask()
  const updateStatus = useUpdateCleaningTaskStatus()

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Curățenie</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Task-uri de curățenie generate automat la check-out sau create manual.
        </p>
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
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Niciun task de curățenie.</p>
        </div>
      ) : (
        <>
          <div className="flex flex-col divide-y divide-border/60 rounded-lg border">
            {data.content.map((task) => {
              const checklistEntries = Object.entries(task.checklistResults)
              const checkedCount = checklistEntries.filter(([, checked]) => checked).length
              const nextStatuses = nextCleaningTaskStatuses(task.status)

              return (
                <div key={task.id} className="flex flex-wrap items-center justify-between gap-3 p-4 text-sm">
                  <div>
                    <p className="font-medium">{task.propertyName}</p>
                    <p className="text-xs text-muted-foreground">
                      {task.scheduledDate}
                      {checklistEntries.length > 0 && ` · ${checkedCount}/${checklistEntries.length} checklist`}
                    </p>
                  </div>

                  <div className="flex flex-col items-start gap-1">
                    <span className="text-xs text-muted-foreground">
                      {task.assignedCleanerName ?? "Neasignat"}
                    </span>
                    <Select
                      value=""
                      onValueChange={(v) => {
                        if (v) assignCleaner.mutate({ id: task.id, cleanerId: v })
                      }}
                    >
                      <SelectTrigger className="w-48">
                        <SelectValue placeholder={task.assignedCleanerId ? "Realocă" : "Alocă cleaner"} />
                      </SelectTrigger>
                      <SelectContent>
                        {cleaners?.content.map((cleaner) => (
                          <SelectItem key={cleaner.id} value={cleaner.id}>
                            {cleaner.firstName} {cleaner.lastName}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <Badge variant={CLEANING_TASK_STATUS_BADGE_VARIANT[task.status]}>
                    {CLEANING_TASK_STATUS_LABELS[task.status]}
                  </Badge>

                  {nextStatuses.length > 0 && (
                    <div className="flex gap-1.5">
                      {nextStatuses.map((next) => (
                        <Button
                          key={next}
                          size="sm"
                          variant="outline"
                          onClick={() => updateStatus.mutate({ id: task.id, status: next })}
                        >
                          {CLEANING_TASK_STATUS_LABELS[next]}
                        </Button>
                      ))}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
          <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  )
}
