package com.bhgroup.pms.dto.messaging;

import com.bhgroup.pms.domain.MessageSenderType;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        MessageSenderType senderType,
        String senderName,
        String body,
        Instant readAt,
        Instant createdAt
) {
}
