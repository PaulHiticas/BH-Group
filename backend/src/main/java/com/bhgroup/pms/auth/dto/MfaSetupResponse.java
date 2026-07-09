package com.bhgroup.pms.auth.dto;

public record MfaSetupResponse(
        String secret,
        String otpAuthUrl
) {
}
