package com.bhgroup.pms.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialReportRowResponse(
        UUID propertyId,
        String propertyName,
        String ownerName,
        BigDecimal grossRevenue,
        BigDecimal commissionAmount,
        BigDecimal expensesTotal,
        BigDecimal netProfit,
        String currency
) {
}
