package com.bhgroup.pms.dto.user;

import com.bhgroup.pms.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(

        @NotNull(message = "Status is required")
        UserStatus status,

        /**
         * Required (and must match the target user's email) only when
         * status is DISABLED - the destructive, effectively-permanent
         * transition. Never checked for PENDING/ACTIVE/SUSPENDED.
         */
        String confirmEmail
) {
}
