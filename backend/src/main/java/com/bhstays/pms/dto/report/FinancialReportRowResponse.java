package com.bhstays.pms.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One property in one currency on the /finance page. The revenue figures
 * are exactly those of the property commission report for the same period
 * (they come from the same calculation); expenses and net profit are added
 * on top.
 *
 * <p>{@code bhStaysRevenue} and {@code ownerAmount} are null while the
 * property has no commission configured.
 */
public record FinancialReportRowResponse(
        UUID propertyId,
        String propertyName,
        String ownerName,
        String currency,
        BigDecimal capturedTotal,
        BigDecimal refundedTotal,
        BigDecimal netRevenue,
        BigDecimal commissionableBase,
        BigDecimal commissionPercent,
        boolean commissionConfigured,
        BigDecimal bhStaysRevenue,
        BigDecimal ownerAmount,
        BigDecimal unallocatedNetRevenue,
        int unallocatedReservationCount,
        BigDecimal expensesTotal,
        /* netRevenue - expensesTotal. */
        BigDecimal netProfit,
        /* Deprecated: same value as {@code netRevenue} (it was never gross); kept for API compatibility. */
        @Deprecated BigDecimal grossRevenue,
        /*
         * Deprecated: same value as {@code bhStaysRevenue}, but 0 instead of null when the commission is
         * not configured; kept for API compatibility.
         */
        @Deprecated BigDecimal commissionAmount
) {
}
