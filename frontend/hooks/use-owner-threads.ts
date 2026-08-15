"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import {
  ownerRequestsApi,
  ownerThreadsSelfApi,
  type OwnerThreadCreatePayload,
  type OwnerThreadStatus,
} from "@/lib/api/owner-threads"
import { ApiError } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

// ---------------------------------------------------------------------------
// Owner-facing
// ---------------------------------------------------------------------------

export function useMyOwnerThreads(page = 0, size = 10) {
  return useQuery({
    queryKey: ["my-owner-threads", page, size],
    queryFn: () => ownerThreadsSelfApi.list(page, size),
  })
}

export function useMyOwnerThread(id: string | undefined) {
  return useQuery({
    queryKey: ["my-owner-thread", id],
    queryFn: () => ownerThreadsSelfApi.get(id as string),
    enabled: !!id,
  })
}

export function useCreateOwnerThread() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: OwnerThreadCreatePayload) => ownerThreadsSelfApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-owner-threads"] })
      toast.success("Cererea a fost trimisă")
    },
    onError: (error) => toast.error(errorMessage(error, "Trimiterea cererii a eșuat")),
  })
}

export function useAddMyThreadMessage(threadId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: string) => ownerThreadsSelfApi.addMessage(threadId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-owner-thread", threadId] })
      queryClient.invalidateQueries({ queryKey: ["my-owner-threads"] })
    },
    onError: (error) => toast.error(errorMessage(error, "Mesajul nu a putut fi trimis")),
  })
}

// ---------------------------------------------------------------------------
// Staff-facing
// ---------------------------------------------------------------------------

export function useOwnerRequests(params: { status?: OwnerThreadStatus; page?: number; size?: number }) {
  return useQuery({
    queryKey: ["owner-requests", params],
    queryFn: () => ownerRequestsApi.list(params),
  })
}

export function useOwnerRequest(id: string | undefined) {
  return useQuery({
    queryKey: ["owner-request", id],
    queryFn: () => ownerRequestsApi.get(id as string),
    enabled: !!id,
  })
}

export function useAddOwnerRequestReply(threadId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: string) => ownerRequestsApi.addMessage(threadId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["owner-request", threadId] })
      queryClient.invalidateQueries({ queryKey: ["owner-requests"] })
    },
    onError: (error) => toast.error(errorMessage(error, "Răspunsul nu a putut fi trimis")),
  })
}

export function useResolveOwnerRequest(threadId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => ownerRequestsApi.resolve(threadId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["owner-request", threadId] })
      queryClient.invalidateQueries({ queryKey: ["owner-requests"] })
      toast.success("Cererea a fost marcată ca rezolvată")
    },
    onError: (error) => toast.error(errorMessage(error, "Actualizarea statusului a eșuat")),
  })
}
