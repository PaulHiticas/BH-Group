package com.bhgroup.pms.dto.publicapi;

import com.bhgroup.pms.domain.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.bhgroup.pms.domain.ReservationStatus;
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
