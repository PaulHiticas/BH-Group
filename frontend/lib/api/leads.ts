import { apiClient } from "@/lib/api/client"
import type { LeadResponse, PageResponse } from "@/lib/api/types"

export interface LeadCreatePayload {
  fullName: string
  email: string
  phone?: string
  city?: string
  message?: string
}

export const leadsApi = {
  create: (payload: LeadCreatePayload) =>
    apiClient.post<LeadResponse>("/public/leads", payload, { skipAuth: true }),

  list: (page = 0, size = 10) =>
    apiClient.get<PageResponse<LeadResponse>>(`/leads?page=${page}&size=${size}&sort=createdAt,desc`),

  markContacted: (id: string, contacted: boolean) =>
    apiClient.patch<LeadResponse>(`/leads/${id}/contacted?contacted=${contacted}`),
}
