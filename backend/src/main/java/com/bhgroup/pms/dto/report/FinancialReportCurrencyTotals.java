package com.bhgroup.pms.dto.report;

import java.math.BigDecimal;

public record FinancialReportCurrencyTotals(
        String currency,
        BigDecimal totalGrossRevenue,
        BigDecimal totalCommission,
        BigDecimal totalExpenses,
        BigDecimal totalNetProfit
) {
}
