package com.bhstays.pms.service.mapper;

import com.bhstays.pms.domain.AssistantChat;
import com.bhstays.pms.domain.AssistantChatMessage;
import com.bhstays.pms.dto.assistant.AssistantChatDetailResponse;
import com.bhstays.pms.dto.assistant.AssistantChatMessageResponse;
import com.bhstays.pms.dto.assistant.AssistantChatSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AssistantChatMapper {

    public AssistantChatSummaryResponse toSummaryResponse(AssistantChat chat) {
        return new AssistantChatSummaryResponse(
                chat.getId(), chat.getGuestName(), chat.getGuestEmail(),
                chat.getStatus().name(), chat.getLastMessageAt(), chat.getCreatedAt());
    }

    public AssistantChatDetailResponse toDetailResponse(AssistantChat chat, List<AssistantChatMessage> messages) {
        return new AssistantChatDetailResponse(
                chat.getId(), chat.getGuestName(), chat.getGuestEmail(),
                chat.getStatus().name(), chat.getLastMessageAt(), chat.getCreatedAt(),
                messages.stream().map(this::toMessageResponse).toList());
    }

    public AssistantChatMessageResponse toMessageResponse(AssistantChatMessage message) {
        return new AssistantChatMessageResponse(
                message.getId(), message.getSenderType().name(), message.getBody(), message.getCreatedAt());
    }
}
