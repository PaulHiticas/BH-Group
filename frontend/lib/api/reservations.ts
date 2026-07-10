import { apiClient } from "@/lib/api/client"
import type {
  AvailabilityResponse,
  CalendarEntryResponse,
  PageResponse,
  ReservationResponse,
  ReservationSource,
  ReservationStatus,
} from "@/lib/api/types"

export interface ReservationPayload {
  propertyId?: string
  guestFirstName: string
  guestLastName: string
  guestEmail?: string
  guestPhone?: string
  checkInDate: string
  checkOutDate: string
  numberOfGuests: number
  source: ReservationSource
  totalAmount?: number | null
  currency: string
  notes?: string
}

export interface ReservationListParams {
  propertyId?: string
  status?: ReservationStatus
  search?: string
  from?: string
  to?: string
  page?: number
  size?: number
  sort?: string
}

function buildQuery(params: Record<string, string | number | undefined>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      query.set(key, String(value))
    }
  })
  const qs = query.toString()
  return qs ? `?${qs}` : ""
}

export const reservationsApi = {
  list: (params: ReservationListParams = {}) =>
    apiClient.get<PageResponse<ReservationResponse>>(
      `/reservations${buildQuery({
        propertyId: params.propertyId,
        status: params.status,
        search: params.search,
        from: params.from,
        to: params.to,
        page: params.page,
        size: params.size ?? 10,
        sort: params.sort ?? "checkInDate,desc",
      })}`
    ),

  get: (id: string) => apiClient.get<ReservationResponse>(`/reservations/${id}`),

  calendar: (propertyId: string, from: string, to: string) =>
    apiClient.get<CalendarEntryResponse[]>(
      `/reservations/calendar${buildQuery({ propertyId, from, to })}`
    ),

  availability: (propertyId: string, checkIn: string, checkOut: string) =>
    apiClient.get<AvailabilityResponse>(
      `/reservations/availability${buildQuery({ propertyId, checkIn, checkOut })}`
    ),

  create: (payload: ReservationPayload) =>
    apiClient.post<ReservationResponse>("/reservations", payload),

  update: (id: string, payload: ReservationPayload) =>
    apiClient.put<ReservationResponse>(`/reservations/${id}`, payload),

  updateStatus: (id: string, status: ReservationStatus) =>
    apiClient.patch<ReservationResponse>(`/reservations/${id}/status`, { status }),

  delete: (id: string) => apiClient.delete<void>(`/reservations/${id}`),

  updateAccessCode: (id: string, accessCode: string) =>
    apiClient.patch<ReservationResponse>(`/reservations/${id}/access-code`, { accessCode }),

  sendCheckinInstructions: (id: string) =>
    apiClient.post<ReservationResponse>(`/reservations/${id}/send-checkin-instructions`),
}
