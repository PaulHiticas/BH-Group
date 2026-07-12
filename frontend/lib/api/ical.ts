import { apiClient } from "@/lib/api/client"
import type { IcalImportFeedResponse, ReservationSource } from "@/lib/api/types"

export const icalApi = {
  getOrCreateExportToken: (propertyId: string) =>
    apiClient.post<{ token: string }>(`/properties/${propertyId}/ical/export-token`),

  regenerateExportToken: (propertyId: string) =>
    apiClient.post<{ token: string }>(`/properties/${propertyId}/ical/export-token/regenerate`),

  listFeeds: (propertyId: string) =>
    apiClient.get<IcalImportFeedResponse[]>(`/properties/${propertyId}/ical/feeds`),

  addFeed: (propertyId: string, source: ReservationSource, feedUrl: string) =>
    apiClient.post<IcalImportFeedResponse>(`/properties/${propertyId}/ical/feeds`, { source, feedUrl }),

  syncFeed: (propertyId: string, feedId: string) =>
    apiClient.post<IcalImportFeedResponse>(`/properties/${propertyId}/ical/feeds/${feedId}/sync`),

  deleteFeed: (propertyId: string, feedId: string) =>
    apiClient.delete<void>(`/properties/${propertyId}/ical/feeds/${feedId}`),
}
