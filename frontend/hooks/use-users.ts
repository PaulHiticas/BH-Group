"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"
import {
  usersApi,
  type UserCreatePayload,
  type UserListParams,
  type UserUpdatePayload,
} from "@/lib/api/users"
import { ApiError, type UserStatus } from "@/lib/api/types"

function errorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) return error.message
  return fallback
}

export function useUsers(params: UserListParams = {}) {
  return useQuery({
    queryKey: ["users", params],
    queryFn: () => usersApi.list(params),
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: UserCreatePayload) => usersApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] })
      toast.success("Cont creat cu succes.")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Nu am putut crea contul. Încearcă din nou."))
    },
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UserUpdatePayload }) =>
      usersApi.update(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] })
      toast.success("Cont actualizat.")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Nu am putut actualiza contul."))
    },
  })
}

export function useUpdateUserStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: UserStatus }) =>
      usersApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] })
      toast.success("Status actualizat.")
    },
    onError: (error) => {
      toast.error(errorMessage(error, "Nu am putut actualiza statusul."))
    },
  })
}
