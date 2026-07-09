import type { Facility, PropertyDocumentType, PropertyStatus, PropertyType } from "@/lib/api/types"

export const PROPERTY_STATUS_LABELS: Record<PropertyStatus, string> = {
  DRAFT: "Ciornă",
  ACTIVE: "Activă",
  INACTIVE: "Inactivă",
  MAINTENANCE: "Mentenanță",
}

export const PROPERTY_TYPE_LABELS: Record<PropertyType, string> = {
  APARTMENT: "Apartament",
  HOUSE: "Casă",
  VILLA: "Vilă",
  STUDIO: "Studio",
  ROOM: "Cameră",
  CABIN: "Cabană",
  OTHER: "Altul",
}

export const FACILITY_LABELS: Record<Facility, string> = {
  WIFI: "Wi-Fi",
  PARKING: "Parcare",
  POOL: "Piscină",
  AIR_CONDITIONING: "Aer condiționat",
  HEATING: "Încălzire",
  KITCHEN: "Bucătărie",
  TV: "TV",
  WASHER: "Mașină de spălat",
  DRYER: "Uscător",
  ELEVATOR: "Lift",
  PET_FRIENDLY: "Pet friendly",
  SMART_LOCK: "Smart Lock",
  BALCONY: "Balcon",
  GYM: "Sală fitness",
  WORKSPACE: "Spațiu de lucru",
  BREAKFAST: "Mic dejun",
}

export const PROPERTY_DOCUMENT_TYPE_LABELS: Record<PropertyDocumentType, string> = {
  CONTRACT: "Contract",
  INVOICE: "Factură",
  ID_COPY: "Copie CI",
  UTILITY_BILL: "Factură utilități",
  OTHER: "Altul",
}

export const ALL_FACILITIES: Facility[] = [
  "WIFI",
  "PARKING",
  "POOL",
  "AIR_CONDITIONING",
  "HEATING",
  "KITCHEN",
  "TV",
  "WASHER",
  "DRYER",
  "ELEVATOR",
  "PET_FRIENDLY",
  "SMART_LOCK",
  "BALCONY",
  "GYM",
  "WORKSPACE",
  "BREAKFAST",
]

export const ALL_PROPERTY_TYPES: PropertyType[] = [
  "APARTMENT",
  "HOUSE",
  "VILLA",
  "STUDIO",
  "ROOM",
  "CABIN",
  "OTHER",
]

export const ALL_PROPERTY_STATUSES: PropertyStatus[] = [
  "DRAFT",
  "ACTIVE",
  "INACTIVE",
  "MAINTENANCE",
]

export const PROPERTY_STATUS_BADGE_VARIANT: Record<
  PropertyStatus,
  "default" | "secondary" | "outline" | "destructive"
> = {
  DRAFT: "outline",
  ACTIVE: "default",
  INACTIVE: "secondary",
  MAINTENANCE: "destructive",
}
