package com.bhstays.pms.dto.report;

import java.math.BigDecimal;

/**
 * One property's collected money in one currency, split into what belongs
 * to the owner and what BH Stays keeps. {@code bhStaysRevenue} and
 * {@code ownerAmount} are null while the property has no commission
 * configured - the split is unknown, not zero.
 *
 * <p>{@code unallocatedNetRevenue} is the part of {@code netRevenue} whose
 * reservations have no price breakdown (e.g. a staff-entered total), so
 * its accommodation share is unknown and it is left out of
 * {@code commissionableBase}.
 */
public record PropertyCommissionCurrencyResponse(
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
        int paidReservationCount
) {
}
