package com.bhgroup.pms.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record FinancialReportSummaryResponse(
        List<FinancialReportRowResponse> rows,
        BigDecimal totalGrossRevenue,
        BigDecimal totalCommission,
        BigDecimal totalExpenses,
        BigDecimal totalNetProfit,
        String currency
) {
}
