package com.bhgroup.pms.dto.pricing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LocalEventCreateRequest(
        /** Applies to every property in this city (case-insensitive, trimmed) - mutually optional with {@link #propertyId}, but at least one is required. */
        @Size(max = 120)
        String city,

        /** Overrides for a single property instead of a whole city. */
        UUID propertyId,

        @NotBlank(message = "Label is required")
        @Size(max = 100)
        String label,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Price multiplier is required")
        @DecimalMin(value = "0.01", message = "Price multiplier must be greater than 0")
        BigDecimal priceMultiplier
) {
}
