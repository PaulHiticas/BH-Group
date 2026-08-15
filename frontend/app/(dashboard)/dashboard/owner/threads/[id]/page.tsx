"use client"

import { use } from "react"
import Link from "next/link"
import { ArrowLeft } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { OwnerThreadMessages } from "@/components/owner-threads/owner-thread-messages"
import { useAddMyThreadMessage, useMyOwnerThread } from "@/hooks/use-owner-threads"
import type { OwnerThreadStatus } from "@/lib/api/owner-threads"

const STATUS_LABELS: Record<OwnerThreadStatus, string> = {
  OPEN: "Deschisă",
  RESOLVED: "Rezolvată",
}

export default function OwnerThreadDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: thread, isLoading } = useMyOwnerThread(id)
  const addMessage = useAddMyThreadMessage(id)

  if (isLoading || !thread) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <Link
        href="/dashboard/owner/threads"
        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-3.5" />
        Înapoi la cererile mele
      </Link>

      <div>
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-semibold tracking-tight">{thread.subject}</h1>
          <Badge variant={thread.status === "OPEN" ? "default" : "secondary"}>
            {STATUS_LABELS[thread.status]}
          </Badge>
        </div>
        <p className="mt-1 text-sm text-muted-foreground">
          {thread.propertyName ?? "Cerere generală"}
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Conversație</CardTitle>
        </CardHeader>
        <CardContent>
          <OwnerThreadMessages
            messages={thread.messages}
            isLoading={false}
            viewerType="OWNER"
            onSend={(body) => addMessage.mutate(body)}
            isSending={addMessage.isPending}
            placeholder="Scrie un mesaj echipei BH Group..."
          />
        </CardContent>
      </Card>
    </div>
  )
}
