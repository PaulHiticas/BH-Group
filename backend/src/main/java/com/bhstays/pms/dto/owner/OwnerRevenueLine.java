package com.bhstays.pms.dto.owner;

import java.math.BigDecimal;

/**
 * An owner's collected money in one currency, on the same formula as the
 * statements: {@code ownerAmount = netRevenue - bhStaysCommission},
 * {@code netPayout = ownerAmount - expensesTotal} (owner-chargeable
 * expenses only). Commission, owner amount and payout are null while
 * money collected for a property without a commission percent
 * ({@code unconfiguredNetRevenue}) cannot be split. {@code commissionPercent}
 * is set only when the line covers a single property.
 */
public record OwnerRevenueLine(
        String currency,
        BigDecimal capturedTotal,
        BigDecimal refundedTotal,
        BigDecimal netRevenue,
        BigDecimal commissionableBase,
        BigDecimal commissionPercent,
        BigDecimal bhStaysCommission,
        BigDecimal ownerAmount,
        BigDecimal netPayout,
        BigDecimal expensesTotal,
        BigDecimal unconfiguredNetRevenue,
        BigDecimal unallocatedNetRevenue,
        int unallocatedReservationCount
) {
}
