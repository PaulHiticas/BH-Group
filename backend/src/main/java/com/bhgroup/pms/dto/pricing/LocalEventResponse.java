package com.bhgroup.pms.dto.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LocalEventResponse(
        UUID id,
        String city,
        UUID propertyId,
        String propertyName,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal priceMultiplier,
        Instant createdAt
) {
}
