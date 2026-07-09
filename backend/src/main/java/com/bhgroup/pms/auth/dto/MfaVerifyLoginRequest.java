package com.bhgroup.pms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyLoginRequest(

        @NotBlank(message = "Challenge token is required")
        String challengeToken,

        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Code must be a 6-digit number")
        String code
) {
}
