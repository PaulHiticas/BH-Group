package com.bhgroup.pms.dto.property;

import com.bhgroup.pms.domain.CancellationPolicy;
import com.bhgroup.pms.domain.Facility;
import com.bhgroup.pms.domain.IntegrationMode;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.PropertyType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public record PropertyResponse(
        UUID id,
        String name,
        String description,
        PropertyType propertyType,
        PropertyStatus status,
        AddressDto address,
        boolean showExactAddressPublicly,
        int bedrooms,
        int bathrooms,
        int maxGuests,
        BigDecimal sizeSqm,
        BigDecimal basePricePerNight,
        BigDecimal weekendPricePerNight,
        BigDecimal cleaningFee,
        BigDecimal extraGuestFee,
        Integer baseGuestsIncluded,
        BigDecimal weeklyDiscountPercent,
        BigDecimal monthlyDiscountPercent,
        Integer minStayNights,
        Integer maxStayNights,
        CancellationPolicy cancellationPolicy,
        UUID ownerId,
        String ownerName,
        BigDecimal commissionPercent,
        List<String> cleaningChecklist,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        Set<Facility> facilities,
        boolean smartLockEnabled,
        String smartLockProvider,
        String smartLockDeviceId,
        boolean lateCheckoutEnabled,
        java.time.LocalTime lateCheckoutTime,
        BigDecimal lateCheckoutFee,
        String icalExportUrl,
        IntegrationMode integrationMode,
        List<PropertyPhotoResponse> photos,
        List<PropertyDocumentResponse> documents,
        Instant createdAt,
        Instant updatedAt
) {
}
