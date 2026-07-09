package com.bhgroup.pms.publicapi.dto;

import com.bhgroup.pms.property.Facility;
import com.bhgroup.pms.property.PropertyType;
import com.bhgroup.pms.property.dto.PropertyPhotoResponse;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PublicPropertyResponse(
        UUID id,
        String name,
        String description,
        PropertyType propertyType,
        String city,
        String county,
        String country,
        int bedrooms,
        int bathrooms,
        int maxGuests,
        BigDecimal sizeSqm,
        BigDecimal basePricePerNight,
        String currency,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        Set<Facility> facilities,
        List<PropertyPhotoResponse> photos
) {
}
