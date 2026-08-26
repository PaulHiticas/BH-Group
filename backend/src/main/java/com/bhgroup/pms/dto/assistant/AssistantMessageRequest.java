package com.bhgroup.pms.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssistantMessageRequest(

        @NotBlank
        @Pattern(regexp = "user|assistant", message = "role must be 'user' or 'assistant'")
        String role,

        @NotBlank(message = "Mesajul nu poate fi gol")
        @Size(max = 2000, message = "Mesajul este prea lung")
        String content
) {
}
