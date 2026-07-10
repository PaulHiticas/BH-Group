package com.bhgroup.pms.dto.auth;

public record MfaSetupResponse(
        String secret,
        String otpAuthUrl
) {
}
