import { apiClient } from "@/lib/api/client"

export interface DynamicPricingConfigResponse {
  propertyId: string
  enabled: boolean
  minPrice: number | null
  maxPrice: number | null
  occupancyWindowDays: number
  occupancyMultiplierMin: number
  occupancyMultiplierMax: number
  leadTimeDays: number
  leadTimeMultiplier: number
}

export interface DynamicPricingConfigPayload {
  enabled: boolean
  minPrice: number | null
  maxPrice: number | null
  occupancyWindowDays: number
  occupancyMultiplierMin: number
  occupancyMultiplierMax: number
  leadTimeDays: number
  leadTimeMultiplier: number
}

export interface LocalEventResponse {
  id: string
  city: string | null
  propertyId: string | null
  propertyName: string | null
  label: string
  startDate: string
  endDate: string
  priceMultiplier: number
  createdAt: string
}

export interface LocalEventPayload {
  city?: string
  propertyId?: string
  label: string
  startDate: string
  endDate: string
  priceMultiplier: number
}

export interface NightlyPriceBreakdown {
  date: string
  baseRate: number
  occupancyFactor: number
  commerciallyBookedNights: number
  sellableNights: number
  leadTimeFactor: number
  eventFactor: number
  rateBeforeClamp: number
  rateAfterClamp: number
}

export interface DynamicPriceBreakdownResponse {
  available: boolean
  unavailableReason: string | null
  checkInDate: string
  checkOutDate: string
  nights: number
  nightlyBreakdown: NightlyPriceBreakdown[]
  subtotal: number | null
  extraGuestFee: number | null
  cleaningFee: number | null
  discountPercent: number | null
  discountAmount: number | null
  totalAmount: number | null
  currency: string
}

export interface BreakdownParams {
  checkIn: string
  checkOut: string
  guests: number
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

export const pricingApi = {
  getConfig: (propertyId: string) =>
    apiClient.get<DynamicPricingConfigResponse>(`/properties/${propertyId}/pricing/config`),

  updateConfig: (propertyId: string, payload: DynamicPricingConfigPayload) =>
    apiClient.put<DynamicPricingConfigResponse>(`/properties/${propertyId}/pricing/config`, payload),

  getBreakdown: (propertyId: string, params: BreakdownParams) =>
    apiClient.get<DynamicPriceBreakdownResponse>(
      `/properties/${propertyId}/pricing/breakdown${buildQuery({
        checkIn: params.checkIn,
        checkOut: params.checkOut,
        guests: params.guests,
      })}`
    ),

  // The backend does not combine city + propertyId in one request: passing
  // propertyId makes it ignore city entirely. Callers must issue two
  // separate requests (one per scope) and merge the results themselves.
  listEventsForProperty: (propertyId: string) =>
    apiClient.get<LocalEventResponse[]>(`/local-events${buildQuery({ propertyId })}`),

  listEventsForCity: (city: string) =>
    apiClient.get<LocalEventResponse[]>(`/local-events${buildQuery({ city })}`),

  createEvent: (payload: LocalEventPayload) =>
    apiClient.post<LocalEventResponse>("/local-events", payload),

  deleteEvent: (id: string) => apiClient.delete<void>(`/local-events/${id}`),
}
