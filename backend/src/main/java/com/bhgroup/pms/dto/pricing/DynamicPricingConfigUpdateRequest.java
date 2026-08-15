package com.bhgroup.pms.dto.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DynamicPricingConfigUpdateRequest(
        boolean enabled,

        BigDecimal minPrice,

        BigDecimal maxPrice,

        @NotNull(message = "Occupancy window is required")
        @Min(value = 1, message = "Occupancy window must be at least 1 day")
        Integer occupancyWindowDays,

        @NotNull(message = "Minimum occupancy multiplier is required")
        @DecimalMin(value = "0.01", message = "Minimum occupancy multiplier must be greater than 0")
        BigDecimal occupancyMultiplierMin,

        @NotNull(message = "Maximum occupancy multiplier is required")
        @DecimalMin(value = "0.01", message = "Maximum occupancy multiplier must be greater than 0")
        BigDecimal occupancyMultiplierMax,

        @NotNull(message = "Lead time is required")
        @Min(value = 0, message = "Lead time cannot be negative")
        Integer leadTimeDays,

        @NotNull(message = "Lead time multiplier is required")
        @DecimalMin(value = "0.01", message = "Lead time multiplier must be greater than 0")
        BigDecimal leadTimeMultiplier
) {
}
