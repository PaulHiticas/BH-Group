package com.bhstays.pms.dto.auth;

import com.bhstays.pms.domain.Role;
import com.bhstays.pms.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role,
        UserStatus status,
        boolean emailVerified,
        boolean mfaEnabled,
        Instant createdAt,
        /**
         * Only populated right after create/resend-invite (see
         * UserAdminService#sendInvite) so the admin can copy/hand off the
         * activation link even when outbound email isn't configured. Null
         * everywhere else (list/get/update) - never persisted or re-derived
         * from the stored token.
         */
        String inviteUrl
) {
}
