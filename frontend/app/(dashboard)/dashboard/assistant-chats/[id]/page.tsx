"use client"

import { use } from "react"
import Link from "next/link"
import { ArrowLeft, CheckCircle2 } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { AssistantChatMessages } from "@/components/assistant-chats/assistant-chat-messages"
import {
  useAddAssistantChatReply,
  useAssistantChatDetail,
  useResolveAssistantChat,
} from "@/hooks/use-assistant-chats"
import type { AssistantChatStatus } from "@/lib/api/assistant-chats"

const STATUS_LABELS: Record<AssistantChatStatus, string> = {
  OPEN: "Deschisă",
  RESOLVED: "Rezolvată",
}

export default function AssistantChatDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const { data: chat, isLoading } = useAssistantChatDetail(id)
  const addReply = useAddAssistantChatReply(id)
  const resolve = useResolveAssistantChat(id)

  if (isLoading || !chat) {
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
        href="/dashboard/assistant-chats"
        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-3.5" />
        Înapoi la conversații
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">
              {chat.guestName ?? "Vizitator anonim"}
            </h1>
            <Badge variant={chat.status === "OPEN" ? "default" : "secondary"}>
              {STATUS_LABELS[chat.status]}
            </Badge>
          </div>
          {chat.guestEmail && <p className="mt-1 text-sm text-muted-foreground">{chat.guestEmail}</p>}
        </div>
        {chat.status === "OPEN" && (
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
          <AssistantChatMessages
            messages={chat.messages}
            isLoading={false}
            onSend={(body) => addReply.mutate(body)}
            isSending={addReply.isPending}
          />
        </CardContent>
      </Card>
    </div>
  )
}
