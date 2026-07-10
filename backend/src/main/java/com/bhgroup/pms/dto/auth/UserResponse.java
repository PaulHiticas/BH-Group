package com.bhgroup.pms.dto.auth;

import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;

import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.UserStatus;
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
