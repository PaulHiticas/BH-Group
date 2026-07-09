package com.bhgroup.pms.reservation.dto;

import com.bhgroup.pms.reservation.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record ReservationStatusUpdateRequest(

        @NotNull(message = "Status is required")
        ReservationStatus status
) {
}
