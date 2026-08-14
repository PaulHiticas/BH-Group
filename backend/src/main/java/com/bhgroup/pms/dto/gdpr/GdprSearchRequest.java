package com.bhgroup.pms.dto.gdpr;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GdprSearchRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {
}
