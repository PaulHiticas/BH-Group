package com.bhgroup.pms.dto.ownerthread;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record OwnerThreadCreateRequest(

        @NotBlank(message = "Subject is required")
        @Size(max = 160)
        String subject,

        /** Optional — a thread can be about one specific property or a general request. */
        UUID propertyId,

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 4000)
        String body
) {
}
