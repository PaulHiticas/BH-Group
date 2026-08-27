"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Badge } from "@/components/ui/badge"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { DataPagination } from "@/components/ui/data-pagination"
import { useAssistantChats } from "@/hooks/use-assistant-chats"
import type { AssistantChatStatus } from "@/lib/api/assistant-chats"

const STATUS_LABELS: Record<AssistantChatStatus, string> = {
  OPEN: "Deschisă",
  RESOLVED: "Rezolvată",
}

const ALL_STATUS_VALUE = "__all__"

function formatTime(value: string) {
  return new Date(value).toLocaleString("ro-RO", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

export function AssistantChatsView() {
  const router = useRouter()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<AssistantChatStatus | typeof ALL_STATUS_VALUE>(ALL_STATUS_VALUE)
  const { data, isLoading } = useAssistantChats({
    status: status === ALL_STATUS_VALUE ? undefined : status,
    page,
  })

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Conversații asistent AI</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Vizitatori care au cerut să vorbească cu o persoană din echipă.
          </p>
        </div>
        <Select
          value={status}
          onValueChange={(value) => {
            setStatus(value as AssistantChatStatus | typeof ALL_STATUS_VALUE)
            setPage(0)
          }}
        >
          <SelectTrigger className="w-44">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_STATUS_VALUE}>Toate</SelectItem>
            <SelectItem value="OPEN">Deschise</SelectItem>
            <SelectItem value="RESOLVED">Rezolvate</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="rounded-xl border border-border/60 bg-card">
        {isLoading ? (
          <div className="p-6">
            <Skeleton className="h-64 w-full" />
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center text-sm text-muted-foreground">
            Nicio conversație încă.
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Vizitator</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Ultima activitate</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.content.map((chat) => (
                <TableRow
                  key={chat.id}
                  className="cursor-pointer"
                  onClick={() => router.push(`/dashboard/assistant-chats/${chat.id}`)}
                >
                  <TableCell className="font-medium">
                    {chat.guestName ?? "Vizitator anonim"}
                    {chat.guestEmail && (
                      <span className="ml-2 text-xs text-muted-foreground">{chat.guestEmail}</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <Badge variant={chat.status === "OPEN" ? "default" : "secondary"}>
                      {STATUS_LABELS[chat.status]}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatTime(chat.lastMessageAt)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      {data && data.totalPages > 1 && (
        <DataPagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
      )}
    </div>
  )
}
