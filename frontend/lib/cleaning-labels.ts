import type {
  CleaningTaskStatus,
  MaintenanceCategory,
  MaintenancePriority,
  MaintenanceStatus,
} from "@/lib/api/types"

export const CLEANING_TASK_STATUS_LABELS: Record<CleaningTaskStatus, string> = {
  NEW: "Nouă",
  ACCEPTED: "Acceptată",
  IN_PROGRESS: "În desfășurare",
  DONE: "Finalizată",
  REJECTED: "Respinsă",
}

export const ALL_CLEANING_TASK_STATUSES: CleaningTaskStatus[] = [
  "NEW",
  "ACCEPTED",
  "IN_PROGRESS",
  "DONE",
  "REJECTED",
]

export const CLEANING_TASK_STATUS_BADGE_VARIANT: Record<
  CleaningTaskStatus,
  "default" | "secondary" | "outline" | "destructive"
> = {
  NEW: "outline",
  ACCEPTED: "secondary",
  IN_PROGRESS: "default",
  DONE: "secondary",
  REJECTED: "destructive",
}

const CLEANING_TASK_TRANSITIONS: Record<CleaningTaskStatus, CleaningTaskStatus[]> = {
  NEW: ["ACCEPTED", "REJECTED"],
  ACCEPTED: ["IN_PROGRESS", "REJECTED"],
  IN_PROGRESS: ["DONE"],
  DONE: [],
  REJECTED: [],
}

export function nextCleaningTaskStatuses(current: CleaningTaskStatus): CleaningTaskStatus[] {
  return CLEANING_TASK_TRANSITIONS[current]
}

export const MAINTENANCE_CATEGORY_LABELS: Record<MaintenanceCategory, string> = {
  PLUMBING: "Instalații sanitare",
  ELECTRICAL: "Electrice",
  APPLIANCE: "Electrocasnice",
  HVAC: "Climatizare",
  STRUCTURAL: "Structural",
  OTHER: "Altele",
}

export const ALL_MAINTENANCE_CATEGORIES: MaintenanceCategory[] = [
  "PLUMBING",
  "ELECTRICAL",
  "APPLIANCE",
  "HVAC",
  "STRUCTURAL",
  "OTHER",
]

export const MAINTENANCE_PRIORITY_LABELS: Record<MaintenancePriority, string> = {
  LOW: "Scăzută",
  MEDIUM: "Medie",
  HIGH: "Ridicată",
  CRITICAL: "Critică",
}

export const ALL_MAINTENANCE_PRIORITIES: MaintenancePriority[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]

export const MAINTENANCE_PRIORITY_BADGE_VARIANT: Record<
  MaintenancePriority,
  "default" | "secondary" | "outline" | "destructive"
> = {
  LOW: "outline",
  MEDIUM: "secondary",
  HIGH: "default",
  CRITICAL: "destructive",
}

export const MAINTENANCE_STATUS_LABELS: Record<MaintenanceStatus, string> = {
  OPEN: "Deschis",
  IN_PROGRESS: "În lucru",
  RESOLVED: "Rezolvat",
  CLOSED: "Închis",
}

export const ALL_MAINTENANCE_STATUSES: MaintenanceStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]

export const MAINTENANCE_STATUS_BADGE_VARIANT: Record<
  MaintenanceStatus,
  "default" | "secondary" | "outline" | "destructive"
> = {
  OPEN: "destructive",
  IN_PROGRESS: "default",
  RESOLVED: "secondary",
  CLOSED: "outline",
}

const MAINTENANCE_STATUS_TRANSITIONS: Record<MaintenanceStatus, MaintenanceStatus[]> = {
  OPEN: ["IN_PROGRESS", "RESOLVED", "CLOSED"],
  IN_PROGRESS: ["RESOLVED", "CLOSED"],
  RESOLVED: ["CLOSED", "IN_PROGRESS"],
  CLOSED: [],
}

export function nextMaintenanceStatuses(current: MaintenanceStatus): MaintenanceStatus[] {
  return MAINTENANCE_STATUS_TRANSITIONS[current]
}
