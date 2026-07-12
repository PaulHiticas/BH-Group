package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyDocument;
import com.bhgroup.pms.domain.PropertyPhoto;
import com.bhgroup.pms.dto.owner.OwnerPropertyResponse;
import com.bhgroup.pms.dto.property.AddressDto;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OwnerMapper {

    private static final String CURRENCY = "RON";
    private final PropertyMapper propertyMapper;

    public OwnerMapper(PropertyMapper propertyMapper) {
        this.propertyMapper = propertyMapper;
    }

    public OwnerPropertyResponse toResponse(Property property, List<PropertyPhoto> photos,
                                             BigDecimal grossRevenue, List<PropertyDocument> documents) {
        String coverUrl = photos.stream()
                .sorted(Comparator.comparing(PropertyPhoto::isCover, Comparator.reverseOrder())
                        .thenComparingInt(PropertyPhoto::getSortOrder))
                .map(PropertyPhoto::getUrl)
                .findFirst()
                .orElse(null);

        BigDecimal commissionPercent = property.getCommissionPercent();
        BigDecimal commissionAmount = commissionPercent != null
                ? grossRevenue.multiply(commissionPercent).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal netRevenue = grossRevenue.subtract(commissionAmount);

        return new OwnerPropertyResponse(
                property.getId(),
                property.getName(),
                property.getPropertyType(),
                property.getStatus(),
                new AddressDto(
                        property.getAddress().getAddressLine(), property.getAddress().getCity(),
                        property.getAddress().getCounty(), property.getAddress().getPostalCode(),
                        property.getAddress().getCountry(), property.getAddress().getLatitude(),
                        property.getAddress().getLongitude()),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getMaxGuests(),
                commissionPercent,
                coverUrl,
                grossRevenue,
                commissionAmount,
                netRevenue,
                CURRENCY,
                documents.stream().map(propertyMapper::toDocumentResponse).toList());
    }
}
