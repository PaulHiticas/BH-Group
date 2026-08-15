package com.bhgroup.pms.dto.ownerthread;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerThreadMessageCreateRequest(

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 4000)
        String body
) {
}
