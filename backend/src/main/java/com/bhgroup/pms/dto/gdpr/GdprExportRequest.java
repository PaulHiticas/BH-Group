package com.bhgroup.pms.dto.gdpr;

import com.bhgroup.pms.domain.GdprVerificationMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GdprExportRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Verification method is required")
        GdprVerificationMethod verificationMethod,

        @NotBlank(message = "Verification note is required")
        @Size(max = 300, message = "Verification note must be under 300 characters")
        String verificationNote
) {
}
