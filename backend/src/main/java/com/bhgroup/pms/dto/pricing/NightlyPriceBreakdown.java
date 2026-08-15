package com.bhgroup.pms.dto.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Auditable breakdown of a single night's price: the base (seasonal/
 * weekend/base) rate, each dynamic-pricing factor applied on top of it in
 * order, and the resulting rate before vs. after the min/max guardrail
 * clamp - so an admin/owner can see exactly why the price changed.
 */
public record NightlyPriceBreakdown(
        LocalDate date,
        BigDecimal baseRate,
        BigDecimal occupancyFactor,
        int commerciallyBookedNights,
        int sellableNights,
        BigDecimal leadTimeFactor,
        BigDecimal eventFactor,
        BigDecimal rateBeforeClamp,
        BigDecimal rateAfterClamp
) {
}
