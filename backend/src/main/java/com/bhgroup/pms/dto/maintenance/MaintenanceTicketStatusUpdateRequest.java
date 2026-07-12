package com.bhgroup.pms.dto.maintenance;

import com.bhgroup.pms.domain.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;

public record MaintenanceTicketStatusUpdateRequest(

        @NotNull(message = "Status is required")
        MaintenanceStatus status
) {
}
