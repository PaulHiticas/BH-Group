package com.bhgroup.pms.auth.dto;

import com.bhgroup.pms.user.Role;
import com.bhgroup.pms.user.UserStatus;
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
        Instant createdAt
) {
}
