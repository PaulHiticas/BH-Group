package com.bhgroup.pms.dto.publicapi;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PublicBookingUpdateRequest(

        @NotNull(message = "Check-in date is required")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOutDate,

        @Min(value = 1, message = "Number of guests must be at least 1")
        int numberOfGuests
) {
}
