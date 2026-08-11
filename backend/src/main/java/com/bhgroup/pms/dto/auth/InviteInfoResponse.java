package com.bhgroup.pms.dto.auth;

import com.bhgroup.pms.domain.Role;

public record InviteInfoResponse(
        String email,
        String firstName,
        String lastName,
        Role role
) {
}
