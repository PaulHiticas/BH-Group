package com.bhgroup.pms.dto.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String entityName,
        String entityId,
        String action,
        UUID actorId,
        String actorEmail,
        String ipAddress,
        String userAgent,
        String description,
        Instant createdAt
) {
}
