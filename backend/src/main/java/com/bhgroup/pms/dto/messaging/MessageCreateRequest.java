package com.bhgroup.pms.dto.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageCreateRequest(

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 4000)
        String body
) {
}
