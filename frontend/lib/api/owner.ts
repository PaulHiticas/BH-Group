import { apiClient } from "@/lib/api/client"
import type {
  ExpenseResponse,
  MaintenanceTicketResponse,
  OwnerDashboardSummaryResponse,
  OwnerPropertyResponse,
  PageResponse,
  ReservationResponse,
  ReservationStatus,
} from "@/lib/api/types"

export interface OwnerReservationListParams {
  status?: ReservationStatus
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

export const ownerApi = {
  dashboardSummary: () =>
    apiClient.get<OwnerDashboardSummaryResponse>("/owner/dashboard/summary"),

  listProperties: (page = 0, size = 12) =>
    apiClient.get<PageResponse<OwnerPropertyResponse>>(
      `/owner/properties${buildQuery({ page, size })}`
    ),

  getProperty: (id: string) =>
    apiClient.get<OwnerPropertyResponse>(`/owner/properties/${id}`),

  listReservations: (params: OwnerReservationListParams = {}) =>
    apiClient.get<PageResponse<ReservationResponse>>(
      `/owner/reservations${buildQuery({
        status: params.status,
        from: params.from,
        to: params.to,
        page: params.page,
        size: params.size ?? 10,
        sort: params.sort ?? "checkInDate,desc",
      })}`
    ),

  listMaintenanceTickets: (page = 0, size = 10) =>
    apiClient.get<PageResponse<MaintenanceTicketResponse>>(
      `/owner/maintenance-tickets${buildQuery({ page, size })}`
    ),

  listExpenses: (page = 0, size = 10) =>
    apiClient.get<PageResponse<ExpenseResponse>>(
      `/owner/expenses${buildQuery({ page, size })}`
    ),
}
