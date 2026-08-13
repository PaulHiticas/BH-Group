import { apiClient } from "@/lib/api/client"
import type { GdprEraseResultResponse, GdprSearchMatchResponse, GdprVerificationMethod } from "@/lib/api/types"

export interface GdprVerification {
  verificationMethod: GdprVerificationMethod
  verificationNote: string
}

export const gdprApi = {
  search: (email: string) =>
    apiClient.get<GdprSearchMatchResponse[]>(`/admin/gdpr/search?email=${encodeURIComponent(email)}`),

  exportUrl: (email: string, verification: GdprVerification) =>
    `/admin/gdpr/export?email=${encodeURIComponent(email)}` +
    `&verificationMethod=${encodeURIComponent(verification.verificationMethod)}` +
    `&verificationNote=${encodeURIComponent(verification.verificationNote)}`,

  erase: (email: string, verification: GdprVerification) =>
    apiClient.post<GdprEraseResultResponse>("/admin/gdpr/erase", {
      email,
      confirm: true,
      verificationMethod: verification.verificationMethod,
      verificationNote: verification.verificationNote,
    }),
}
