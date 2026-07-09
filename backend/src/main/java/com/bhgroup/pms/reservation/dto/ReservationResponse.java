package com.bhgroup.pms.reservation.dto;

import com.bhgroup.pms.reservation.ReservationSource;
import com.bhgroup.pms.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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
        Instant createdAt,
        Instant updatedAt
) {
}
