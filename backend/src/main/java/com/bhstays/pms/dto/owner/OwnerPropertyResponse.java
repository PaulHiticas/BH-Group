package com.bhstays.pms.dto.owner;

import com.bhstays.pms.domain.PropertyStatus;
import com.bhstays.pms.domain.PropertyType;
import com.bhstays.pms.dto.property.AddressDto;
import com.bhstays.pms.dto.property.PropertyDocumentResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * {@code revenueByCurrency} holds the property's all-time collected money,
 * one line per currency, on the same formula as the statements. The flat
 * revenue fields are deprecated RON-only figures kept for API
 * compatibility.
 */
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
        /* Deprecated: RON net collected revenue; use {@code revenueByCurrency}. */
        @Deprecated BigDecimal grossRevenue,
        /* Deprecated: RON BH Stays commission (0 if not configured); use {@code revenueByCurrency}. */
        @Deprecated BigDecimal commissionAmount,
        /* Deprecated: RON owner amount; use {@code revenueByCurrency}. */
        @Deprecated BigDecimal netRevenue,
        /* Deprecated: always RON. */
        @Deprecated String currency,
        List<PropertyDocumentResponse> documents,
        List<OwnerRevenueLine> revenueByCurrency
) {
}
