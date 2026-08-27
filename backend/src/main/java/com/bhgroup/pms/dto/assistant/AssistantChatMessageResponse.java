package com.bhgroup.pms.dto.assistant;

import java.time.Instant;
import java.util.UUID;

public record AssistantChatMessageResponse(
        UUID id,
        String senderType,
        String body,
        Instant createdAt
) {
}
