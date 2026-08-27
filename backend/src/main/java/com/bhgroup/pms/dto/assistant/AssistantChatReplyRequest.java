package com.bhgroup.pms.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantChatReplyRequest(

        @NotBlank(message = "Mesajul nu poate fi gol")
        @Size(max = 2000, message = "Mesajul este prea lung")
        String body
) {
}
