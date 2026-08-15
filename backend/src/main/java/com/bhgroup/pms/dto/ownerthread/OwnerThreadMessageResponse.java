package com.bhgroup.pms.dto.ownerthread;

import com.bhgroup.pms.domain.OwnerThreadSenderType;
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
