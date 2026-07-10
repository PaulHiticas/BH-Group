package com.bhgroup.pms.dto.property;

import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import java.time.Instant;
import java.util.UUID;

import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
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
