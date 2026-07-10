package com.bhgroup.pms.dto.publicapi;

import com.bhgroup.pms.domain.Facility;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.dto.property.PropertyPhotoResponse;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bhgroup.pms.domain.Facility;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.dto.property.PropertyPhotoResponse;
public record PublicPropertyResponse(
        UUID id,
        String name,
        String description,
        PropertyType propertyType,
        String city,
        String county,
        String country,
        Double latitude,
        Double longitude,
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
