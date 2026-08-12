import { apiClient } from "@/lib/api/client"
import type { GdprEraseResultResponse, GdprSearchMatchResponse } from "@/lib/api/types"

export const gdprApi = {
  search: (email: string) =>
    apiClient.get<GdprSearchMatchResponse[]>(`/admin/gdpr/search?email=${encodeURIComponent(email)}`),

  erase: (email: string) =>
    apiClient.post<GdprEraseResultResponse>("/admin/gdpr/erase", { email, confirm: true }),
}
