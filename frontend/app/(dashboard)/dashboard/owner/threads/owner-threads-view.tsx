"use client"

import { useState } from "react"
import Link from "next/link"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { Plus } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { DataPagination } from "@/components/ui/data-pagination"
import { useOwnerProperties } from "@/hooks/use-owner"
import { useCreateOwnerThread, useMyOwnerThreads } from "@/hooks/use-owner-threads"
import type { OwnerThreadStatus } from "@/lib/api/owner-threads"

const STATUS_LABELS: Record<OwnerThreadStatus, string> = {
  OPEN: "Deschisă",
  RESOLVED: "Rezolvată",
}

const NO_PROPERTY_VALUE = "__none__"

export const createThreadSchema = z.object({
  subject: z.string().min(1, "Subiectul este obligatoriu").max(160),
  propertyId: z.string(),
  body: z.string().min(1, "Mesajul este obligatoriu").max(4000),
})

type CreateThreadValues = z.infer<typeof createThreadSchema>

function NewThreadDialog() {
  const [open, setOpen] = useState(false)
  const { data: properties } = useOwnerProperties(0, 100)
  const createThread = useCreateOwnerThread()

  const form = useForm<CreateThreadValues>({
    resolver: zodResolver(createThreadSchema),
    defaultValues: { subject: "", propertyId: NO_PROPERTY_VALUE, body: "" },
  })

  function onSubmit(values: CreateThreadValues) {
    createThread.mutate(
      {
        subject: values.subject,
        propertyId: values.propertyId === NO_PROPERTY_VALUE ? undefined : values.propertyId,
        body: values.body,
      },
      {
        onSuccess: () => {
          form.reset()
          setOpen(false)
        },
      }
    )
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<Button className="gap-2" />}>
        <Plus className="size-4" />
        Cerere nouă
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Cerere nouă către BH Group</DialogTitle>
          <DialogDescription>
            Trimite o întrebare generală sau una despre unul dintre apartamentele tale.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <FormField
              control={form.control}
              name="subject"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Subiect</FormLabel>
                  <FormControl>
                    <Input placeholder="Ex: Reparație boiler" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="propertyId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Apartament (opțional)</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value={NO_PROPERTY_VALUE}>Cerere generală</SelectItem>
                      {properties?.content.map((property) => (
                        <SelectItem key={property.id} value={property.id}>
                          {property.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="body"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Mesaj</FormLabel>
                  <FormControl>
                    <Textarea rows={4} placeholder="Descrie cererea ta..." {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button type="submit" className="w-full" disabled={createThread.isPending}>
              {createThread.isPending ? "Se trimite..." : "Trimite cererea"}
            </Button>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}

function formatTime(value: string) {
  return new Date(value).toLocaleString("ro-RO", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

export function OwnerThreadsView() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useMyOwnerThreads(page)

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Contact / Cereri</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Cererile și întrebările tale trimise echipei BH Group.
          </p>
        </div>
        <NewThreadDialog />
      </div>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="text-sm text-muted-foreground">Nicio cerere trimisă încă.</p>
        </div>
      ) : (
        <>
          <div className="flex flex-col divide-y divide-border/60 rounded-lg border">
            {data.content.map((thread) => (
              <Link
                key={thread.id}
                href={`/dashboard/owner/threads/${thread.id}`}
                className="flex flex-wrap items-center justify-between gap-3 p-4 text-sm hover:bg-accent/50"
              >
                <div>
                  <p className="font-medium">{thread.subject}</p>
                  <p className="text-xs text-muted-foreground">
                    {thread.propertyName ?? "Cerere generală"} · {formatTime(thread.lastMessageAt)}
                  </p>
                </div>
                <Badge variant={thread.status === "OPEN" ? "default" : "secondary"}>
                  {STATUS_LABELS[thread.status]}
                </Badge>
              </Link>
            ))}
          </div>
          {data.totalPages > 1 && (
            <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
          )}
        </>
      )}
    </div>
  )
}
