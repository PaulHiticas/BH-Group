package com.bhstays.pms.dto.gdpr;

import com.bhstays.pms.domain.AssistantChatSenderType;
import java.time.Instant;

public record GdprAssistantChatMessageExport(
        AssistantChatSenderType senderType,
        String body,
        Instant createdAt
) {
}
