package com.bhgroup.pms.dto.owner;

import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import com.bhgroup.pms.dto.property.AddressDto;
import com.bhgroup.pms.dto.property.PropertyDocumentResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OwnerPropertyResponse(
        UUID id,
        String name,
        PropertyType propertyType,
        PropertyStatus status,
        AddressDto address,
        int bedrooms,
        int bathrooms,
        int maxGuests,
        BigDecimal commissionPercent,
        String coverPhotoUrl,
        BigDecimal grossRevenue,
        BigDecimal commissionAmount,
        BigDecimal netRevenue,
        String currency,
        List<PropertyDocumentResponse> documents
) {
}
