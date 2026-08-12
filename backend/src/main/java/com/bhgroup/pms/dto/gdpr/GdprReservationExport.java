package com.bhgroup.pms.dto.gdpr;

import com.bhgroup.pms.domain.ReservationSource;
import com.bhgroup.pms.domain.ReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GdprReservationExport(
        UUID id,
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
        List<GdprMessageExport> messages
) {
}
