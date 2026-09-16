package com.bhstays.pms.dto.ownerthread;

import com.bhstays.pms.domain.OwnerThreadSenderType;
import java.time.Instant;
import java.util.UUID;

public record OwnerThreadMessageResponse(
        UUID id,
        OwnerThreadSenderType senderType,
        String senderName,
        String body,
        Instant readAt,
        Instant createdAt
) {
}
