package com.bhgroup.pms.dto.user;

import com.bhgroup.pms.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.bhgroup.pms.domain.Role;
public record UserUpdateRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        String lastName,

        String phone,

        @NotNull(message = "Role is required")
        Role role
) {
}
