export type Role = "SUPER_ADMIN" | "ADMINISTRATOR"

export type UserStatus = "PENDING" | "ACTIVE" | "SUSPENDED" | "DISABLED"

export interface UserResponse {
  id: string
  email: string
  firstName: string
  lastName: string
  phone: string | null
  role: Role
  status: UserStatus
  emailVerified: boolean
  mfaEnabled: boolean
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserResponse
}

export interface MfaChallengeResponse {
  challengeToken: string
  expiresIn: number
}

export type LoginResult = AuthResponse | MfaChallengeResponse

export function isMfaChallenge(result: LoginResult): result is MfaChallengeResponse {
  return (result as MfaChallengeResponse).challengeToken !== undefined
}

export interface ApiSuccessEnvelope<T> {
  success: true
  data: T | null
  message: string | null
  timestamp: string
}

export interface ApiErrorEnvelope {
  success: false
  errorCode: string
  message: string
  fieldErrors?: { field: string; message: string }[]
  timestamp: string
  path: string
}

export class ApiError extends Error {
  errorCode: string
  fieldErrors?: { field: string; message: string }[]
  status: number

  constructor(status: number, envelope: ApiErrorEnvelope) {
    super(envelope.message)
    this.name = "ApiError"
    this.status = status
    this.errorCode = envelope.errorCode
    this.fieldErrors = envelope.fieldErrors
  }
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// ---------------------------------------------------------------------------
// Properties
// ---------------------------------------------------------------------------

export type PropertyType =
  | "APARTMENT"
  | "HOUSE"
  | "VILLA"
  | "STUDIO"
  | "ROOM"
  | "CABIN"
  | "OTHER"

export type PropertyStatus = "DRAFT" | "ACTIVE" | "INACTIVE" | "MAINTENANCE"

export type Facility =
  | "WIFI"
  | "PARKING"
  | "POOL"
  | "AIR_CONDITIONING"
  | "HEATING"
  | "KITCHEN"
  | "TV"
  | "WASHER"
  | "DRYER"
  | "ELEVATOR"
  | "PET_FRIENDLY"
  | "SMART_LOCK"
  | "BALCONY"
  | "GYM"
  | "WORKSPACE"
  | "BREAKFAST"

export type PropertyDocumentType =
  | "CONTRACT"
  | "INVOICE"
  | "ID_COPY"
  | "UTILITY_BILL"
  | "OTHER"

export interface AddressDto {
  addressLine: string
  city: string
  county: string | null
  postalCode: string | null
  country: string
  latitude: number | null
  longitude: number | null
}

export interface PropertyPhotoResponse {
  id: string
  url: string
  caption: string | null
  sortOrder: number
  cover: boolean
}

export interface PropertyDocumentResponse {
  id: string
  fileName: string
  url: string
  documentType: PropertyDocumentType
  createdAt: string
}

export interface PropertySummaryResponse {
  id: string
  name: string
  city: string
  propertyType: PropertyType
  status: PropertyStatus
  bedrooms: number
  bathrooms: number
  maxGuests: number
  coverPhotoUrl: string | null
  createdAt: string
}

export interface PropertyResponse {
  id: string
  name: string
  description: string | null
  propertyType: PropertyType
  status: PropertyStatus
  address: AddressDto
  bedrooms: number
  bathrooms: number
  maxGuests: number
  sizeSqm: number | null
  basePricePerNight: number | null
  checkInTime: string
  checkOutTime: string
  facilities: Facility[]
  smartLockEnabled: boolean
  smartLockProvider: string | null
  smartLockDeviceId: string | null
  photos: PropertyPhotoResponse[]
  documents: PropertyDocumentResponse[]
  createdAt: string
  updatedAt: string
}

// ---------------------------------------------------------------------------
// Reservations
// ---------------------------------------------------------------------------

export type ReservationStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CHECKED_IN"
  | "CHECKED_OUT"
  | "CANCELLED"
  | "NO_SHOW"

export type ReservationSource = "DIRECT" | "AIRBNB" | "BOOKING_COM" | "OTHER" | "MAINTENANCE"

export interface ReservationResponse {
  id: string
  propertyId: string
  propertyName: string
  guestFirstName: string
  guestLastName: string
  guestEmail: string | null
  guestPhone: string | null
  checkInDate: string
  checkOutDate: string
  numberOfGuests: number
  status: ReservationStatus
  source: ReservationSource
  totalAmount: number | null
  currency: string
  notes: string | null
  accessCode: string | null
  accessCodeSentAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CalendarEntryResponse {
  reservationId: string
  guestFullName: string
  checkInDate: string
  checkOutDate: string
  status: ReservationStatus
  source: ReservationSource
}

export interface AvailabilityResponse {
  available: boolean
}

export interface PublicCalendarEntryResponse {
  checkInDate: string
  checkOutDate: string
}

// ---------------------------------------------------------------------------
// Public booking engine
// ---------------------------------------------------------------------------

export interface PublicPropertySummaryResponse {
  id: string
  name: string
  city: string
  county: string | null
  latitude: number | null
  longitude: number | null
  propertyType: PropertyType
  bedrooms: number
  bathrooms: number
  maxGuests: number
  basePricePerNight: number | null
  currency: string
  coverPhotoUrl: string | null
  facilities: Facility[]
}

export interface PublicPropertyResponse {
  id: string
  name: string
  description: string | null
  propertyType: PropertyType
  city: string
  county: string | null
  country: string
  latitude: number | null
  longitude: number | null
  bedrooms: number
  bathrooms: number
  maxGuests: number
  sizeSqm: number | null
  basePricePerNight: number | null
  currency: string
  checkInTime: string
  checkOutTime: string
  facilities: Facility[]
  photos: PropertyPhotoResponse[]
}

export interface PublicReservationResponse {
  id: string
  propertyName: string
  propertyCity: string
  guestFirstName: string
  guestLastName: string
  guestEmail: string | null
  guestPhone: string | null
  checkInDate: string
  checkOutDate: string
  numberOfGuests: number
  status: ReservationStatus
  totalAmount: number | null
  currency: string
  managementToken: string
}

// ---------------------------------------------------------------------------
// Leads (property owners interested in listing)
// ---------------------------------------------------------------------------

export interface LeadResponse {
  id: string
  fullName: string
  email: string
  phone: string | null
  city: string | null
  message: string | null
  contacted: boolean
  createdAt: string
}

// ---------------------------------------------------------------------------
// Admin dashboard summary
// ---------------------------------------------------------------------------

export interface DashboardSummaryResponse {
  totalProperties: number
  totalReservations: number
  totalRevenue: number
  currency: string
  uncontactedLeads: number
  upcomingReservations: ReservationResponse[]
  recentLeads: LeadResponse[]
}

// ---------------------------------------------------------------------------
// Audit log
// ---------------------------------------------------------------------------

export interface AuditLogResponse {
  id: string
  entityName: string
  entityId: string | null
  action: string
  actorId: string | null
  actorEmail: string | null
  ipAddress: string | null
  userAgent: string | null
  description: string | null
  createdAt: string
}
