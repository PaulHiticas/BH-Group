"use client"

import { useState } from "react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { ApiError } from "@/lib/api/types"

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30 * 1000,
            // A 4xx is a predictable, non-transient failure (bad request,
            // unauthorized, a blocked-until-MFA-setup 403, not found, ...) -
            // retrying it just delays the UI and, for the MFA gate
            // specifically, doubles up the redirect in api/client.ts.
            // Only worth retrying once for actually-transient failures
            // (5xx, network errors).
            retry: (failureCount, error) => {
              if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
                return false
              }
              return failureCount < 1
            },
            refetchOnWindowFocus: false,
          },
        },
      })
  )

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}
