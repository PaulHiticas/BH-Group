package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyPhoto;
import com.bhgroup.pms.dto.publicapi.PublicPropertyResponse;
import com.bhgroup.pms.dto.publicapi.PublicPropertySummaryResponse;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicPropertyMapper {

    private static final String DEFAULT_CURRENCY = "RON";

    /**
     * Rounding to 2 decimal places blurs a pin to roughly a 1km area - close
     * enough to place the property on a map for browsing, not precise
     * enough to find the actual building. Used whenever a property hasn't
     * opted into showing its exact public location.
     */
    private static final int APPROXIMATE_COORDINATE_SCALE = 2;

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
                publicLatitude(property),
                publicLongitude(property),
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
                publicLatitude(property),
                publicLongitude(property),
                property.isShowExactAddressPublicly(),
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

    private Double publicLatitude(Property property) {
        return publicCoordinate(property, property.getAddress().getLatitude());
    }

    private Double publicLongitude(Property property) {
        return publicCoordinate(property, property.getAddress().getLongitude());
    }

    private Double publicCoordinate(Property property, Double exact) {
        if (exact == null) return null;
        if (property.isShowExactAddressPublicly()) return exact;
        double scale = Math.pow(10, APPROXIMATE_COORDINATE_SCALE);
        return Math.round(exact * scale) / scale;
    }
}
