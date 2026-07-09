package com.bhgroup.pms.property.dto;

import com.bhgroup.pms.property.PropertyStatus;
import com.bhgroup.pms.property.PropertyType;
import java.time.Instant;
import java.util.UUID;

public record PropertySummaryResponse(
        UUID id,
        String name,
        String city,
        PropertyType propertyType,
        PropertyStatus status,
        int bedrooms,
        int bathrooms,
        int maxGuests,
        String coverPhotoUrl,
        Instant createdAt
) {
}
