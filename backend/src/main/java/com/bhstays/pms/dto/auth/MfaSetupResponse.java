package com.bhstays.pms.dto.auth;

public record MfaSetupResponse(
        String secret,
        String otpAuthUrl
) {
}
