"use client"

import { useState } from "react"
import { Send } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"
import type { AssistantChatMessageResponse } from "@/lib/api/assistant-chats"

const SENDER_LABELS: Record<AssistantChatMessageResponse["senderType"], string> = {
  GUEST: "Vizitator",
  AI: "Asistent AI",
  STAFF: "Echipa BH Group",
}

function formatTime(value: string) {
  return new Date(value).toLocaleString("ro-RO", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

/**
 * Same look/interaction as OwnerThreadMessages, but for the three
 * assistant-handoff sender types (GUEST/AI/STAFF instead of OWNER/STAFF) -
 * staff is always the "mine" (right-aligned) side here.
 */
export function AssistantChatMessages({
  messages,
  isLoading,
  onSend,
  isSending,
}: {
  messages: AssistantChatMessageResponse[] | undefined
  isLoading: boolean
  onSend: (body: string) => void
  isSending: boolean
}) {
  const [body, setBody] = useState("")

  function handleSend() {
    const trimmed = body.trim()
    if (!trimmed) return
    onSend(trimmed)
    setBody("")
  }

  if (isLoading) {
    return <Skeleton className="h-40 w-full" />
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex max-h-96 flex-col gap-2 overflow-y-auto rounded-md border border-border/60 bg-muted/20 p-3">
        {!messages || messages.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">Niciun mesaj încă.</p>
        ) : (
          messages.map((message) => {
            const mine = message.senderType === "STAFF"
            return (
              <div key={message.id} className={cn("flex flex-col gap-0.5", mine ? "items-end" : "items-start")}>
                <div
                  className={cn(
                    "max-w-[80%] rounded-lg px-3 py-2 text-sm whitespace-pre-line",
                    mine
                      ? "bg-primary text-primary-foreground"
                      : "bg-background text-foreground ring-1 ring-border/60"
                  )}
                >
                  {message.body}
                </div>
                <span className="px-1 text-xs text-muted-foreground">
                  {SENDER_LABELS[message.senderType]} · {formatTime(message.createdAt)}
                </span>
              </div>
            )
          })
        )}
      </div>
      <div className="flex gap-2">
        <Textarea
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="Scrie un răspuns vizitatorului..."
          rows={2}
          className="resize-none"
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault()
              handleSend()
            }
          }}
        />
        <Button
          type="button"
          size="icon"
          disabled={!body.trim() || isSending}
          onClick={handleSend}
          aria-label="Trimite mesaj"
        >
          <Send className="size-4" />
        </Button>
      </div>
    </div>
  )
}
