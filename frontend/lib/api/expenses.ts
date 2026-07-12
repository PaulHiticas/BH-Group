import { apiClient } from "@/lib/api/client"
import type { ExpenseCategory, ExpenseResponse, PageResponse } from "@/lib/api/types"

export interface ExpenseListParams {
  propertyId?: string
  category?: ExpenseCategory
  from?: string
  to?: string
  page?: number
  size?: number
}

export interface ExpenseCreatePayload {
  propertyId: string
  category: ExpenseCategory
  amount: number
  currency?: string
  vendor?: string
  expenseDate: string
  notes?: string
  chargeToOwner: boolean
}

export type ExpenseUpdatePayload = Omit<ExpenseCreatePayload, "propertyId">

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

export const expensesApi = {
  list: (params: ExpenseListParams = {}) =>
    apiClient.get<PageResponse<ExpenseResponse>>(
      `/expenses${buildQuery({
        propertyId: params.propertyId,
        category: params.category,
        from: params.from,
        to: params.to,
        page: params.page,
        size: params.size ?? 10,
      })}`
    ),

  create: (payload: ExpenseCreatePayload) => apiClient.post<ExpenseResponse>("/expenses", payload),

  update: (id: string, payload: ExpenseUpdatePayload) =>
    apiClient.put<ExpenseResponse>(`/expenses/${id}`, payload),

  delete: (id: string) => apiClient.delete<void>(`/expenses/${id}`),

  uploadReceipt: (id: string, file: File) => {
    const formData = new FormData()
    formData.set("file", file)
    return apiClient.upload<ExpenseResponse>(`/expenses/${id}/receipt`, formData)
  },
}
