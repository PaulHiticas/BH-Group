package com.bhgroup.pms.dto.property;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SeasonalRateResponse(
        UUID id,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal pricePerNight
) {
}
