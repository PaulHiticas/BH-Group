"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import {
  cleaningTasksApi,
  type CleaningTaskCreatePayload,
  type CleaningTaskListParams,
  type CleaningTaskUpdatePayload,
} from "@/lib/api/cleaning"
import { ApiError, type CleaningTaskStatus } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useCleaningTasks(params: CleaningTaskListParams) {
  return useQuery({
    queryKey: ["cleaning-tasks", params],
    queryFn: () => cleaningTasksApi.list(params),
  })
}

export function useCleaningTask(id: string) {
  return useQuery({
    queryKey: ["cleaning-task", id],
    queryFn: () => cleaningTasksApi.get(id),
    enabled: !!id,
  })
}

export function useCreateCleaningTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CleaningTaskCreatePayload) => cleaningTasksApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cleaning-tasks"] })
      toast.success("Task de curățenie creat")
    },
    onError: (error) => toast.error(errorMessage(error, "Crearea task-ului a eșuat")),
  })
}

export function useUpdateCleaningTask(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CleaningTaskUpdatePayload) => cleaningTasksApi.update(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cleaning-tasks"] })
      queryClient.invalidateQueries({ queryKey: ["cleaning-task", id] })
      toast.success("Task actualizat")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea a eșuat")),
  })
}

export function useAssignCleaningTask() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, cleanerId }: { id: string; cleanerId: string }) =>
      cleaningTasksApi.assign(id, cleanerId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cleaning-tasks"] })
      toast.success("Cleaner alocat")
    },
    onError: (error) => toast.error(errorMessage(error, "Alocarea a eșuat")),
  })
}

export function useUpdateCleaningTaskStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: CleaningTaskStatus }) =>
      cleaningTasksApi.updateStatus(id, status),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["cleaning-tasks"] })
      queryClient.invalidateQueries({ queryKey: ["cleaning-task", variables.id] })
      toast.success("Status actualizat")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea statusului a eșuat")),
  })
}
