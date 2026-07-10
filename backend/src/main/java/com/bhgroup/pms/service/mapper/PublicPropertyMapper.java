package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyPhoto;
import com.bhgroup.pms.dto.publicapi.PublicPropertyResponse;
import com.bhgroup.pms.dto.publicapi.PublicPropertySummaryResponse;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyPhoto;
import com.bhgroup.pms.dto.publicapi.PublicPropertyResponse;
import com.bhgroup.pms.dto.publicapi.PublicPropertySummaryResponse;
@Component
@RequiredArgsConstructor
public class PublicPropertyMapper {

    private static final String DEFAULT_CURRENCY = "RON";

    private final PropertyMapper propertyMapper;

    public PublicPropertySummaryResponse toSummary(Property property, List<PropertyPhoto> photos) {
        String coverUrl = photos.stream()
                .sorted(Comparator.comparing(PropertyPhoto::isCover, Comparator.reverseOrder())
                        .thenComparingInt(PropertyPhoto::getSortOrder))
                .map(PropertyPhoto::getUrl)
                .findFirst()
                .orElse(null);

        return new PublicPropertySummaryResponse(
                property.getId(),
                property.getName(),
                property.getAddress().getCity(),
                property.getAddress().getCounty(),
                property.getAddress().getLatitude(),
                property.getAddress().getLongitude(),
                property.getPropertyType(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getMaxGuests(),
                property.getBasePricePerNight(),
                DEFAULT_CURRENCY,
                coverUrl,
                property.getFacilities()
        );
    }

    public PublicPropertyResponse toResponse(Property property, List<PropertyPhoto> photos) {
        return new PublicPropertyResponse(
                property.getId(),
                property.getName(),
                property.getDescription(),
                property.getPropertyType(),
                property.getAddress().getCity(),
                property.getAddress().getCounty(),
                property.getAddress().getCountry(),
                property.getAddress().getLatitude(),
                property.getAddress().getLongitude(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getMaxGuests(),
                property.getSizeSqm(),
                property.getBasePricePerNight(),
                DEFAULT_CURRENCY,
                property.getCheckInTime(),
                property.getCheckOutTime(),
                property.getFacilities(),
                photos.stream().map(propertyMapper::toPhotoResponse).toList()
        );
    }
}
