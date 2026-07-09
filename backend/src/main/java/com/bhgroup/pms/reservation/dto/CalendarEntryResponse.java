package com.bhgroup.pms.reservation.dto;

import com.bhgroup.pms.reservation.ReservationStatus;
import java.time.LocalDate;
import java.util.UUID;

public record CalendarEntryResponse(
        UUID reservationId,
        String guestFullName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        ReservationStatus status
) {
}
