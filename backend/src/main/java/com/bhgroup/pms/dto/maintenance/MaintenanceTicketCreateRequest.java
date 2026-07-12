package com.bhgroup.pms.dto.maintenance;

import com.bhgroup.pms.domain.MaintenanceCategory;
import com.bhgroup.pms.domain.MaintenancePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record MaintenanceTicketCreateRequest(

        @NotNull(message = "Property is required")
        UUID propertyId,

        @NotBlank(message = "Title is required")
        String title,

        String description,

        MaintenanceCategory category,

        MaintenancePriority priority,

        String vendor,

        BigDecimal estimatedCost
) {
}
