package com.bhgroup.pms.dto.maintenance;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MaintenanceTicketAssignRequest(

        @NotNull(message = "Assignee is required")
        UUID assignedToId
) {
}
