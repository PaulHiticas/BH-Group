"use client"

import { use, useRef } from "react"
import Link from "next/link"
import { ArrowLeft, Upload } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Skeleton } from "@/components/ui/skeleton"
import {
  useMyCleaningTask,
  useUpdateMyChecklistItem,
  useUpdateMyTaskStatus,
  useUploadMyTaskPhoto,
} from "@/hooks/use-cleaner"
import {
  CLEANING_TASK_STATUS_BADGE_VARIANT,
  CLEANING_TASK_STATUS_LABELS,
  nextCleaningTaskStatuses,
} from "@/lib/cleaning-labels"

export default function MyCleaningTaskDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: task, isLoading } = useMyCleaningTask(id)
  const updateStatus = useUpdateMyTaskStatus(id)
  const updateChecklist = useUpdateMyChecklistItem(id)
  const uploadPhoto = useUploadMyTaskPhoto(id)
  const fileInputRef = useRef<HTMLInputElement>(null)

  if (isLoading || !task) {
    return (
      <div className="mx-auto flex max-w-2xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  const nextStatuses = nextCleaningTaskStatuses(task.status)

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6">
      <Link
        href="/dashboard/cleaner/tasks"
        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-3.5" />
        Înapoi la sarcinile mele
      </Link>

      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{task.propertyName}</h1>
          <p className="mt-1 text-sm text-muted-foreground">Programat: {task.scheduledDate}</p>
        </div>
        <Badge variant={CLEANING_TASK_STATUS_BADGE_VARIANT[task.status]}>
          {CLEANING_TASK_STATUS_LABELS[task.status]}
        </Badge>
      </div>

      {nextStatuses.length > 0 && (
        <div className="flex gap-2">
          {nextStatuses.map((next) => (
            <Button key={next} onClick={() => updateStatus.mutate(next)} disabled={updateStatus.isPending}>
              {CLEANING_TASK_STATUS_LABELS[next]}
            </Button>
          ))}
        </div>
      )}

      {task.notes && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Note</CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground">{task.notes}</CardContent>
        </Card>
      )}

      {Object.keys(task.checklistResults).length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Checklist</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            {Object.entries(task.checklistResults).map(([label, checked]) => (
              <label key={label} className="flex items-center gap-2 text-sm">
                <Checkbox
                  checked={checked}
                  onCheckedChange={(value) => updateChecklist.mutate({ label, checked: !!value })}
                />
                {label}
              </label>
            ))}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Poze</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          {task.photos.length > 0 && (
            <div className="grid grid-cols-3 gap-2">
              {task.photos.map((photo) => (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  key={photo.id}
                  src={photo.url}
                  alt="poză task curățenie"
                  className="aspect-square w-full rounded-md object-cover"
                />
              ))}
            </div>
          )}
          <Button
            variant="outline"
            size="sm"
            className="self-start"
            disabled={uploadPhoto.isPending}
            onClick={() => fileInputRef.current?.click()}
          >
            <Upload className="size-4" />
            {uploadPhoto.isPending ? "Se încarcă..." : "Adaugă poză"}
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp,image/avif"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0]
              if (file) uploadPhoto.mutate(file)
              e.target.value = ""
            }}
          />
        </CardContent>
      </Card>
    </div>
  )
}
