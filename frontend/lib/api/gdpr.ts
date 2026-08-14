import { apiClient } from "@/lib/api/client"
import type { GdprEraseResultResponse, GdprSearchMatchResponse, GdprVerificationMethod } from "@/lib/api/types"

export interface GdprVerification {
  verificationMethod: GdprVerificationMethod
  verificationNote: string
}

export const gdprApi = {
  search: (email: string) =>
    apiClient.post<GdprSearchMatchResponse[]>("/admin/gdpr/search", { email }),

  export: (email: string, verification: GdprVerification) =>
    apiClient.post<unknown>("/admin/gdpr/export", {
      email,
      verificationMethod: verification.verificationMethod,
      verificationNote: verification.verificationNote,
    }),

  erase: (email: string, confirmationEmail: string, verification: GdprVerification) =>
    apiClient.post<GdprEraseResultResponse>("/admin/gdpr/erase", {
      email,
      confirm: true,
      confirmationEmail,
      verificationMethod: verification.verificationMethod,
      verificationNote: verification.verificationNote,
    }),
}
