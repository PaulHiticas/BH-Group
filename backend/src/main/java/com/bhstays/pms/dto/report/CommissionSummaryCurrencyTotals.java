package com.bhstays.pms.dto.report;

import java.math.BigDecimal;

/**
 * Portfolio totals for one currency. {@code propertiesNetRevenue} is money
 * collected for the owners' properties - not BH Stays revenue; BH Stays
 * only keeps {@code bhStaysRevenue}. The figures always reconcile as
 * {@code propertiesNetRevenue = bhStaysRevenue + ownersAmount + unconfiguredNetRevenue}:
 * properties without a commission are counted in the net revenue but in
 * neither of the two shares.
 */
public record CommissionSummaryCurrencyTotals(
        String currency,
        BigDecimal propertiesNetRevenue,
        BigDecimal bhStaysRevenue,
        BigDecimal ownersAmount,
        int includedPropertyCount,
        int unconfiguredPropertyCount,
        BigDecimal unconfiguredNetRevenue,
        BigDecimal unallocatedNetRevenue
) {
}
