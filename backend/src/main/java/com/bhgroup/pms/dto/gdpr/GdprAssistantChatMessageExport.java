package com.bhgroup.pms.dto.gdpr;

import com.bhgroup.pms.domain.AssistantChatSenderType;
import java.time.Instant;

public record GdprAssistantChatMessageExport(
        AssistantChatSenderType senderType,
        String body,
        Instant createdAt
) {
}
