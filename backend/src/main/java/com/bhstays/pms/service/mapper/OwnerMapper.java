package com.bhstays.pms.service.mapper;

import com.bhstays.pms.domain.Property;
import com.bhstays.pms.domain.PropertyDocument;
import com.bhstays.pms.domain.PropertyPhoto;
import com.bhstays.pms.dto.owner.OwnerPropertyResponse;
import com.bhstays.pms.dto.owner.OwnerRevenueLine;
import com.bhstays.pms.dto.property.AddressDto;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OwnerMapper {

    /** The only currency the deprecated flat revenue fields ever covered. */
    public static final String LEGACY_CURRENCY = "RON";
    private final PropertyMapper propertyMapper;

    public OwnerMapper(PropertyMapper propertyMapper) {
        this.propertyMapper = propertyMapper;
    }

    /** {@code revenueByCurrency} comes from OwnerFinancialsService - nothing is computed here. */
    public OwnerPropertyResponse toResponse(Property property, List<PropertyPhoto> photos,
                                             List<OwnerRevenueLine> revenueByCurrency, List<PropertyDocument> documents) {
        String coverUrl = photos.stream()
                .sorted(Comparator.comparing(PropertyPhoto::isCover, Comparator.reverseOrder())
                        .thenComparingInt(PropertyPhoto::getSortOrder))
                .map(PropertyPhoto::getUrl)
                .findFirst()
                .orElse(null);

        OwnerRevenueLine ron = legacyLine(revenueByCurrency);
        BigDecimal grossRevenue = ron != null ? ron.netRevenue() : BigDecimal.ZERO;
        BigDecimal commissionAmount = ron != null && ron.bhStaysCommission() != null
                ? ron.bhStaysCommission() : BigDecimal.ZERO;
        BigDecimal netRevenue = ron != null && ron.ownerAmount() != null
                ? ron.ownerAmount() : grossRevenue.subtract(commissionAmount);

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
                property.getCommissionPercent(),
                coverUrl,
                grossRevenue,
                commissionAmount,
                netRevenue,
                LEGACY_CURRENCY,
                documents.stream().map(propertyMapper::toDocumentResponse).toList(),
                revenueByCurrency);
    }

    public static OwnerRevenueLine legacyLine(List<OwnerRevenueLine> revenueByCurrency) {
        return revenueByCurrency.stream()
                .filter(line -> LEGACY_CURRENCY.equals(line.currency()))
                .findFirst()
                .orElse(null);
    }
}
