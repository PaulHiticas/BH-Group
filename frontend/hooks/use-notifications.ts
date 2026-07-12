"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { notificationsApi } from "@/lib/api/notifications"

export function useNotifications(enabled: boolean) {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: () => notificationsApi.list(),
    enabled,
    refetchInterval: 30_000,
  })
}

export function useUnreadNotificationCount(enabled: boolean) {
  return useQuery({
    queryKey: ["notifications-unread-count"],
    queryFn: () => notificationsApi.unreadCount(),
    enabled,
    refetchInterval: 30_000,
  })
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => notificationsApi.markRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] })
      queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] })
    },
  })
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => notificationsApi.markAllRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] })
      queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] })
    },
  })
}
