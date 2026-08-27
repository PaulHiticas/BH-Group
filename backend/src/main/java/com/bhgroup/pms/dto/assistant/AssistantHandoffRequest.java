package com.bhgroup.pms.dto.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AssistantHandoffRequest(

        // Not @NotEmpty: a visitor can ask for a human before typing
        // anything to the FAQ bot at all - an empty history is valid, just
        // means there's no prior conversation to hand off.
        @NotNull
        @Size(max = 20, message = "Conversația este prea lungă")
        @Valid
        List<AssistantMessageRequest> messages,

        @Size(max = 160)
        String guestName,

        @Email
        @Size(max = 255)
        String guestEmail
) {
}
