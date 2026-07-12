"use client"

import { useRouter } from "next/navigation"
import { Bell, Check } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
  useUnreadNotificationCount,
} from "@/hooks/use-notifications"
import { cn } from "@/lib/utils"
import type { NotificationResponse } from "@/lib/api/types"

function formatTime(value: string) {
  return new Date(value).toLocaleString("ro-RO", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

export function NotificationBell({ enabled }: { enabled: boolean }) {
  const router = useRouter()
  const { data: unread } = useUnreadNotificationCount(enabled)
  const { data: notifications, isLoading } = useNotifications(enabled)
  const markRead = useMarkNotificationRead()
  const markAllRead = useMarkAllNotificationsRead()

  if (!enabled) return null

  const unreadCount = unread?.count ?? 0
  const items: NotificationResponse[] = notifications?.content ?? []

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <button
            className="relative flex size-9 items-center justify-center rounded-full outline-none hover:bg-accent focus-visible:ring-2 focus-visible:ring-ring"
            aria-label="Notificări"
          />
        }
      >
        <Bell className="size-4.5" />
        {unreadCount > 0 && (
          <Badge
            variant="destructive"
            className="absolute -top-0.5 -right-0.5 h-4.5 min-w-4.5 justify-center rounded-full px-1 text-[10px] leading-none"
          >
            {unreadCount > 9 ? "9+" : unreadCount}
          </Badge>
        )}
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        <DropdownMenuGroup>
          <div className="flex items-center justify-between px-1.5 py-1">
            <DropdownMenuLabel className="p-0">Notificări</DropdownMenuLabel>
            {unreadCount > 0 && (
              <Button
                size="sm"
                variant="ghost"
                className="h-6 gap-1 px-1.5 text-xs"
                onClick={() => markAllRead.mutate()}
              >
                <Check className="size-3" />
                Marchează toate
              </Button>
            )}
          </div>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        {isLoading ? (
          <p className="px-2 py-4 text-center text-sm text-muted-foreground">Se încarcă...</p>
        ) : items.length === 0 ? (
          <p className="px-2 py-4 text-center text-sm text-muted-foreground">Nicio notificare.</p>
        ) : (
          <div className="flex max-h-80 flex-col overflow-y-auto">
            {items.map((notification) => (
              <DropdownMenuItem
                key={notification.id}
                className={cn(
                  "flex flex-col items-start gap-0.5 whitespace-normal py-2",
                  !notification.readAt && "bg-accent/50"
                )}
                onClick={() => {
                  if (!notification.readAt) markRead.mutate(notification.id)
                  if (notification.linkPath) router.push(notification.linkPath)
                }}
              >
                <span className="text-sm font-medium">{notification.title}</span>
                {notification.body && (
                  <span className="text-xs text-muted-foreground">{notification.body}</span>
                )}
                <span className="text-[11px] text-muted-foreground">
                  {formatTime(notification.createdAt)}
                </span>
              </DropdownMenuItem>
            ))}
          </div>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
