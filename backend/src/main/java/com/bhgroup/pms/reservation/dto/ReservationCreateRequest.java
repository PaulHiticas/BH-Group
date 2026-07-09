package com.bhgroup.pms.reservation.dto;

import com.bhgroup.pms.reservation.ReservationSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationCreateRequest(

        @NotNull(message = "Property is required")
        UUID propertyId,

        @NotBlank(message = "Guest first name is required")
        String guestFirstName,

        @NotBlank(message = "Guest last name is required")
        String guestLastName,

        @Email(message = "Guest email must be valid")
        String guestEmail,

        String guestPhone,

        @NotNull(message = "Check-in date is required")
        @FutureOrPresent(message = "Check-in date cannot be in the past")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOutDate,

        @Min(value = 1, message = "Number of guests must be at least 1")
        int numberOfGuests,

        ReservationSource source,

        BigDecimal totalAmount,

        String currency,

        String notes
) {
}
