"use client"

import { use } from "react"
import Link from "next/link"
import { ArrowLeft, CheckCircle2 } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { OwnerThreadMessages } from "@/components/owner-threads/owner-thread-messages"
import {
  useAddOwnerRequestReply,
  useOwnerRequest,
  useResolveOwnerRequest,
} from "@/hooks/use-owner-threads"
import type { OwnerThreadStatus } from "@/lib/api/owner-threads"

const STATUS_LABELS: Record<OwnerThreadStatus, string> = {
  OPEN: "Deschisă",
  RESOLVED: "Rezolvată",
}

export default function OwnerRequestDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: thread, isLoading } = useOwnerRequest(id)
  const addReply = useAddOwnerRequestReply(id)
  const resolve = useResolveOwnerRequest(id)

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
        href="/dashboard/owner-requests"
        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-3.5" />
        Înapoi la cereri
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">{thread.subject}</h1>
            <Badge variant={thread.status === "OPEN" ? "default" : "secondary"}>
              {STATUS_LABELS[thread.status]}
            </Badge>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            {thread.ownerName} · {thread.propertyName ?? "Cerere generală"}
          </p>
        </div>
        {thread.status === "OPEN" && (
          <Button
            type="button"
            variant="outline"
            className="gap-2"
            disabled={resolve.isPending}
            onClick={() => resolve.mutate()}
          >
            <CheckCircle2 className="size-4" />
            Marchează rezolvată
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Conversație</CardTitle>
        </CardHeader>
        <CardContent>
          <OwnerThreadMessages
            messages={thread.messages}
            isLoading={false}
            viewerType="STAFF"
            onSend={(body) => addReply.mutate(body)}
            isSending={addReply.isPending}
            placeholder="Scrie un răspuns proprietarului..."
          />
        </CardContent>
      </Card>
    </div>
  )
}
