package com.bhgroup.pms.dto.reservation;

import jakarta.validation.constraints.Size;

public record AccessCodeUpdateRequest(
        @Size(max = 50, message = "Access code must be at most 50 characters") String accessCode
) {
}
