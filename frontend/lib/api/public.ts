import { apiClient } from "@/lib/api/client"
import type {
  AvailabilityResponse,
  Facility,
  PageResponse,
  PublicCalendarEntryResponse,
  PublicPropertyResponse,
  PublicPropertySummaryResponse,
  PublicReservationResponse,
} from "@/lib/api/types"

export interface PublicPropertySearchParams {
  search?: string
  guests?: number
  bedrooms?: number
  minPrice?: number
  maxPrice?: number
  facilities?: Facility[]
  checkIn?: string
  checkOut?: string
  page?: number
  size?: number
}

export interface PublicBookingPayload {
  propertyId: string
  guestFirstName: string
  guestLastName: string
  guestEmail: string
  guestPhone: string
  checkInDate: string
  checkOutDate: string
  numberOfGuests: number
  notes?: string
}

export interface PublicBookingUpdatePayload {
  checkInDate: string
  checkOutDate: string
  numberOfGuests: number
}

function buildQuery(params: Record<string, string | number | undefined | string[]>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      value.forEach((item) => query.append(key, item))
    } else if (value !== undefined && value !== "") {
      query.set(key, String(value))
    }
  })
  const qs = query.toString()
  return qs ? `?${qs}` : ""
}

export const publicApi = {
  searchProperties: (params: PublicPropertySearchParams = {}) =>
    apiClient.get<PageResponse<PublicPropertySummaryResponse>>(
      `/public/properties${buildQuery({
        search: params.search,
        guests: params.guests,
        bedrooms: params.bedrooms,
        minPrice: params.minPrice,
        maxPrice: params.maxPrice,
        facilities: params.facilities,
        checkIn: params.checkIn,
        checkOut: params.checkOut,
        page: params.page,
        size: params.size ?? 12,
      })}`,
      { skipAuth: true }
    ),

  getProperty: (id: string) =>
    apiClient.get<PublicPropertyResponse>(`/public/properties/${id}`, { skipAuth: true }),

  checkAvailability: (propertyId: string, checkIn: string, checkOut: string) =>
    apiClient.get<AvailabilityResponse>(
      `/public/reservations/availability${buildQuery({ propertyId, checkIn, checkOut })}`,
      { skipAuth: true }
    ),

  getCalendar: (propertyId: string, from: string, to: string) =>
    apiClient.get<PublicCalendarEntryResponse[]>(
      `/public/properties/${propertyId}/calendar${buildQuery({ from, to })}`,
      { skipAuth: true }
    ),

  createBooking: (payload: PublicBookingPayload) =>
    apiClient.post<PublicReservationResponse>("/public/reservations", payload, { skipAuth: true }),

  getBookingByToken: (token: string) =>
    apiClient.get<PublicReservationResponse>(`/public/reservations/manage/${token}`, { skipAuth: true }),

  updateBookingByToken: (token: string, payload: PublicBookingUpdatePayload) =>
    apiClient.put<PublicReservationResponse>(`/public/reservations/manage/${token}`, payload, {
      skipAuth: true,
    }),

  cancelBookingByToken: (token: string) =>
    apiClient.post<PublicReservationResponse>(`/public/reservations/manage/${token}/cancel`, undefined, {
      skipAuth: true,
    }),
}
