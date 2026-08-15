package com.bhgroup.pms.dto.pricing;

import java.math.BigDecimal;
import java.util.UUID;

public record DynamicPricingConfigResponse(
        UUID propertyId,
        boolean enabled,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int occupancyWindowDays,
        BigDecimal occupancyMultiplierMin,
        BigDecimal occupancyMultiplierMax,
        int leadTimeDays,
        BigDecimal leadTimeMultiplier
) {
}
