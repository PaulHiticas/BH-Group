package com.bhstays.pms.dto.reservation;

import com.bhstays.pms.domain.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record ReservationStatusUpdateRequest(

        @NotNull(message = "Status is required")
        ReservationStatus status
) {
}
