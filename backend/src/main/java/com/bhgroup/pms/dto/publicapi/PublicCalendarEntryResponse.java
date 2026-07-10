package com.bhgroup.pms.dto.publicapi;

import java.time.LocalDate;

public record PublicCalendarEntryResponse(
        LocalDate checkInDate,
        LocalDate checkOutDate
) {
}
