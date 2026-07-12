"use client"

import { useQuery } from "@tanstack/react-query"
import { financialReportsApi, type FinancialReportParams } from "@/lib/api/financial-reports"

export function useFinancialReport(params: FinancialReportParams) {
  return useQuery({
    queryKey: ["financial-report", params],
    queryFn: () => financialReportsApi.summary(params),
  })
}
