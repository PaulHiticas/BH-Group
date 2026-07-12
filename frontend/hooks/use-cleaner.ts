"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import { cleanerApi } from "@/lib/api/cleaner"
import { ApiError, type CleaningTaskStatus } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useMyCleaningTasks(status: CleaningTaskStatus | undefined, page = 0) {
  return useQuery({
    queryKey: ["my-cleaning-tasks", status, page],
    queryFn: () => cleanerApi.listMyTasks(status, page),
  })
}

export function useMyCleaningTask(id: string) {
  return useQuery({
    queryKey: ["my-cleaning-task", id],
    queryFn: () => cleanerApi.getMyTask(id),
    enabled: !!id,
  })
}

export function useUpdateMyTaskStatus(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (status: CleaningTaskStatus) => cleanerApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-cleaning-tasks"] })
      queryClient.invalidateQueries({ queryKey: ["my-cleaning-task", id] })
      toast.success("Status actualizat")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea statusului a eșuat")),
  })
}

export function useUpdateMyChecklistItem(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ label, checked }: { label: string; checked: boolean }) =>
      cleanerApi.updateChecklistItem(id, label, checked),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-cleaning-task", id] })
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea checklist-ului a eșuat")),
  })
}

export function useUploadMyTaskPhoto(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => cleanerApi.uploadPhoto(id, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-cleaning-task", id] })
      toast.success("Poză adăugată")
    },
    onError: (error) => toast.error(errorMessage(error, "Încărcarea pozei a eșuat")),
  })
}
