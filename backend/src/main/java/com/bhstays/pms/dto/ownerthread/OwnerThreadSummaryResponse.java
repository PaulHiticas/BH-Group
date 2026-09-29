package com.bhstays.pms.dto.ownerthread;

import com.bhstays.pms.domain.OwnerThreadStatus;
import java.time.Instant;
import java.util.UUID;

public record OwnerThreadSummaryResponse(
        UUID id,
        String subject,
        OwnerThreadStatus status,
        UUID propertyId,
        String propertyName,
        UUID ownerId,
        String ownerName,
        Instant lastMessageAt,
        Instant createdAt
) {
}
