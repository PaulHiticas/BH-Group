package com.bhgroup.pms.dto.reservation;

import com.bhgroup.pms.domain.ReservationStatus;
import jakarta.validation.constraints.NotNull;

import com.bhgroup.pms.domain.ReservationStatus;
public record ReservationStatusUpdateRequest(

        @NotNull(message = "Status is required")
        ReservationStatus status
) {
}
