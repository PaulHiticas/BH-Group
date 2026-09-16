package com.bhstays.pms.dto.maintenance;

import com.bhstays.pms.domain.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;

public record MaintenanceTicketStatusUpdateRequest(

        @NotNull(message = "Status is required")
        MaintenanceStatus status
) {
}
