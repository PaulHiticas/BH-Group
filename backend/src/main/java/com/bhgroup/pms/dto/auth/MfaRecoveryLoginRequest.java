package com.bhgroup.pms.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record MfaRecoveryLoginRequest(

        @NotBlank(message = "Challenge token is required")
        String challengeToken,

        @NotBlank(message = "Recovery code is required")
        String recoveryCode
) {
}
