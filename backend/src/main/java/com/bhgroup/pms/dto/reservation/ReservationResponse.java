package com.bhgroup.pms.dto.reservation;

import com.bhgroup.pms.domain.ReservationSource;
import com.bhgroup.pms.domain.ReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.bhgroup.pms.domain.ReservationSource;
import com.bhgroup.pms.domain.ReservationStatus;
public record ReservationResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        String guestFirstName,
        String guestLastName,
        String guestEmail,
        String guestPhone,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numberOfGuests,
        ReservationStatus status,
        ReservationSource source,
        BigDecimal totalAmount,
        String currency,
        String notes,
        String accessCode,
        Instant accessCodeSentAt,
        Instant createdAt,
        Instant updatedAt
) {
}
