import type { ExpenseCategory } from "@/lib/api/types"

export const EXPENSE_CATEGORY_LABELS: Record<ExpenseCategory, string> = {
  CLEANING: "Curățenie",
  MAINTENANCE: "Mentenanță",
  UTILITIES: "Utilități",
  SUPPLIES: "Consumabile",
  TAX: "Taxe",
  INSURANCE: "Asigurare",
  COMMISSION: "Comision",
  OTHER: "Altele",
}

export const ALL_EXPENSE_CATEGORIES: ExpenseCategory[] = [
  "CLEANING",
  "MAINTENANCE",
  "UTILITIES",
  "SUPPLIES",
  "TAX",
  "INSURANCE",
  "COMMISSION",
  "OTHER",
]
