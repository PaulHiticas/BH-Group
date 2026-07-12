package com.bhgroup.pms.dto.maintenance;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceTicketPhotoResponse(
        UUID id,
        String url,
        String caption,
        Instant createdAt
) {
}
