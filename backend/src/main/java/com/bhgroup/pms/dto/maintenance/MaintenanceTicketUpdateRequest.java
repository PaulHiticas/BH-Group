package com.bhgroup.pms.dto.maintenance;

import com.bhgroup.pms.domain.MaintenanceCategory;
import com.bhgroup.pms.domain.MaintenancePriority;
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
