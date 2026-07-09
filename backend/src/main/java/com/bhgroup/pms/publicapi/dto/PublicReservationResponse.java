package com.bhgroup.pms.publicapi.dto;

import com.bhgroup.pms.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PublicReservationResponse(
        UUID id,
        String propertyName,
        String propertyCity,
        String guestFirstName,
        String guestLastName,
        String guestEmail,
        String guestPhone,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numberOfGuests,
        ReservationStatus status,
        BigDecimal totalAmount,
        String currency,
        String managementToken
) {
}
