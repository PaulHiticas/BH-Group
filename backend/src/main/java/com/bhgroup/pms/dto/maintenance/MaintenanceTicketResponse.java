package com.bhgroup.pms.dto.maintenance;

import com.bhgroup.pms.domain.MaintenanceCategory;
import com.bhgroup.pms.domain.MaintenancePriority;
import com.bhgroup.pms.domain.MaintenanceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MaintenanceTicketResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        String title,
        String description,
        MaintenanceCategory category,
        MaintenancePriority priority,
        MaintenanceStatus status,
        UUID reportedById,
        String reportedByName,
        UUID assignedToId,
        String assignedToName,
        String vendor,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        Instant resolvedAt,
        List<MaintenanceTicketPhotoResponse> photos,
        Instant createdAt,
        Instant updatedAt
) {
}
