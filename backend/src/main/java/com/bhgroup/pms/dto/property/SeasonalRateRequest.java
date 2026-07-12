package com.bhgroup.pms.dto.property;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SeasonalRateRequest(

        @NotBlank(message = "Label is required")
        @Size(max = 100)
        String label,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Price per night is required")
        @DecimalMin(value = "0", message = "Price per night cannot be negative")
        BigDecimal pricePerNight
) {
}
