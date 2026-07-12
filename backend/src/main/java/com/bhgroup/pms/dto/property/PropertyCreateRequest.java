package com.bhgroup.pms.dto.property;

import com.bhgroup.pms.domain.CancellationPolicy;
import com.bhgroup.pms.domain.Facility;
import com.bhgroup.pms.domain.PropertyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.bhgroup.pms.domain.Address;
import com.bhgroup.pms.domain.Facility;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyType;
public record PropertyCreateRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 200)
        String name,

        @Size(max = 4000)
        String description,

        @NotNull(message = "Property type is required")
        PropertyType propertyType,

        @NotNull(message = "Address is required")
        @Valid
        AddressDto address,

        @Min(value = 0, message = "Bedrooms cannot be negative")
        int bedrooms,

        @Min(value = 0, message = "Bathrooms cannot be negative")
        int bathrooms,

        @Min(value = 1, message = "Maximum guests must be at least 1")
        int maxGuests,

        BigDecimal sizeSqm,

        BigDecimal basePricePerNight,

        BigDecimal weekendPricePerNight,

        BigDecimal cleaningFee,

        BigDecimal extraGuestFee,

        Integer baseGuestsIncluded,

        BigDecimal weeklyDiscountPercent,

        BigDecimal monthlyDiscountPercent,

        @Min(value = 1, message = "Minimum stay must be at least 1 night")
        Integer minStayNights,

        Integer maxStayNights,

        CancellationPolicy cancellationPolicy,

        UUID ownerId,

        BigDecimal commissionPercent,

        java.util.List<String> cleaningChecklist,

        LocalTime checkInTime,

        LocalTime checkOutTime,

        Set<Facility> facilities,

        boolean smartLockEnabled,

        @Size(max = 100)
        String smartLockProvider,

        @Size(max = 150)
        String smartLockDeviceId
) {
}
