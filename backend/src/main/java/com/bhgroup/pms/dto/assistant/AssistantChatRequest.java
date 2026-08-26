package com.bhgroup.pms.dto.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AssistantChatRequest(

        @NotEmpty(message = "Conversația nu poate fi goală")
        @Size(max = 20, message = "Conversația este prea lungă")
        @Valid
        List<AssistantMessageRequest> messages
) {
}
