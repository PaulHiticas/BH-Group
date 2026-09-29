package com.bhstays.pms.dto.maintenance;

import com.bhstays.pms.domain.MaintenanceCategory;
import com.bhstays.pms.domain.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record MaintenanceTicketUpdateRequest(

        @NotBlank(message = "Title is required")
        String title,

        String description,

        MaintenanceCategory category,

        MaintenancePriority priority,

        String vendor,

        BigDecimal estimatedCost,

        BigDecimal actualCost
) {
}
