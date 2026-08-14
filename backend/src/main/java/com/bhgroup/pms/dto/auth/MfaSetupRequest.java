package com.bhgroup.pms.dto.auth;

public record MfaSetupRequest(
        /** Required (and checked) only when the account already has MFA enabled - see AuthService.setupMfa. */
        String password
) {
}
