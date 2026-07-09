import type { ReservationSource, ReservationStatus } from "@/lib/api/types"

export const RESERVATION_STATUS_LABELS: Record<ReservationStatus, string> = {
  PENDING: "În așteptare",
  CONFIRMED: "Confirmată",
  CHECKED_IN: "Check-in efectuat",
  CHECKED_OUT: "Check-out efectuat",
  CANCELLED: "Anulată",
  NO_SHOW: "Neprezentare",
}

export const ALL_RESERVATION_STATUSES: ReservationStatus[] = [
  "PENDING",
  "CONFIRMED",
  "CHECKED_IN",
  "CHECKED_OUT",
  "CANCELLED",
  "NO_SHOW",
]

export const RESERVATION_STATUS_BADGE_VARIANT: Record<
  ReservationStatus,
  "default" | "secondary" | "outline" | "destructive"
> = {
  PENDING: "outline",
  CONFIRMED: "default",
  CHECKED_IN: "secondary",
  CHECKED_OUT: "secondary",
  CANCELLED: "destructive",
  NO_SHOW: "destructive",
}

export const RESERVATION_SOURCE_LABELS: Record<ReservationSource, string> = {
  DIRECT: "Direct",
  AIRBNB: "Airbnb",
  BOOKING_COM: "Booking.com",
  OTHER: "Altul",
}

export const ALL_RESERVATION_SOURCES: ReservationSource[] = [
  "DIRECT",
  "AIRBNB",
  "BOOKING_COM",
  "OTHER",
]

const RESERVATION_TRANSITIONS: Record<ReservationStatus, ReservationStatus[]> = {
  PENDING: ["CONFIRMED", "CANCELLED"],
  CONFIRMED: ["CHECKED_IN", "CANCELLED", "NO_SHOW"],
  CHECKED_IN: ["CHECKED_OUT"],
  CHECKED_OUT: [],
  CANCELLED: [],
  NO_SHOW: [],
}

export function nextReservationStatuses(current: ReservationStatus): ReservationStatus[] {
  return RESERVATION_TRANSITIONS[current]
}
