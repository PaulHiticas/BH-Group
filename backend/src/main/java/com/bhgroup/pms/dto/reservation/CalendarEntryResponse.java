package com.bhgroup.pms.dto.reservation;

import com.bhgroup.pms.domain.ReservationSource;
import com.bhgroup.pms.domain.ReservationStatus;
import java.time.LocalDate;
import java.util.UUID;

public record CalendarEntryResponse(
        UUID reservationId,
        String guestFullName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        ReservationStatus status,
        ReservationSource source
) {
}
