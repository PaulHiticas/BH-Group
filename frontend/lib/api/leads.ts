import { apiClient } from "@/lib/api/client"
import type { LeadResponse, LeadType, PageResponse } from "@/lib/api/types"

export interface LeadCreatePayload {
  fullName: string
  email: string
  phone?: string
  city?: string
  message?: string
  leadType?: LeadType
  bedrooms?: number
  consentGiven: boolean
  utmSource?: string
  utmMedium?: string
  utmCampaign?: string
  /** Honeypot - always sent empty by real users, never rendered visibly. */
  website?: string
}

export const leadsApi = {
  create: (payload: LeadCreatePayload) =>
    apiClient.post<LeadResponse>("/public/leads", payload, { skipAuth: true }),

  list: (page = 0, size = 10) =>
    apiClient.get<PageResponse<LeadResponse>>(`/leads?page=${page}&size=${size}&sort=createdAt,desc`),

  markContacted: (id: string, contacted: boolean) =>
    apiClient.patch<LeadResponse>(`/leads/${id}/contacted?contacted=${contacted}`),
}
