package com.bhgroup.pms.dto.publicapi;

import com.bhgroup.pms.domain.Facility;
import com.bhgroup.pms.domain.PropertyType;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record PublicPropertySummaryResponse(
        UUID id,
        String name,
        String city,
        String county,
        Double latitude,
        Double longitude,
        PropertyType propertyType,
        int bedrooms,
        int bathrooms,
        int maxGuests,
        BigDecimal basePricePerNight,
        String currency,
        String coverPhotoUrl,
        Set<Facility> facilities
) {
}
