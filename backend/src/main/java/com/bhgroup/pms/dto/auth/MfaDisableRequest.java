package com.bhgroup.pms.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record MfaDisableRequest(

        @NotBlank(message = "Password is required")
        String password
) {
}
