package com.bhgroup.pms.dto.auth;

import java.util.List;

public record MfaEnableResponse(
        /** Shown to the user exactly once - only their hash is stored. */
        List<String> recoveryCodes
) {
}
