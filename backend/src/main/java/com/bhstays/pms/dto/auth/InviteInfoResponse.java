package com.bhstays.pms.dto.auth;

import com.bhstays.pms.domain.Role;

public record InviteInfoResponse(
        String email,
        String firstName,
        String lastName,
        Role role
) {
}
