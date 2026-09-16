package com.bhstays.pms.dto.reservation;

import com.bhstays.pms.domain.ReservationSource;
import com.bhstays.pms.domain.ReservationStatus;
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
