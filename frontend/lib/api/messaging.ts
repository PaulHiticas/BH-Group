import { apiClient } from "@/lib/api/client"
import type { MessageResponse } from "@/lib/api/types"

export const messagingApi = {
  listForReservation: (reservationId: string) =>
    apiClient.get<MessageResponse[]>(`/reservations/${reservationId}/messages`),

  sendStaffMessage: (reservationId: string, body: string) =>
    apiClient.post<MessageResponse>(`/reservations/${reservationId}/messages`, { body }),
}
