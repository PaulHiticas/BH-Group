package com.bhgroup.pms.publicapi.dto;

import com.bhgroup.pms.property.PropertyType;
import java.math.BigDecimal;
import java.util.UUID;

public record PublicPropertySummaryResponse(
        UUID id,
        String name,
        String city,
        String county,
        PropertyType propertyType,
        int bedrooms,
        int bathrooms,
        int maxGuests,
        BigDecimal basePricePerNight,
        String currency,
        String coverPhotoUrl
) {
}
