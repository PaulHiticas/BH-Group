package com.bhgroup.pms.dto.user;

import com.bhgroup.pms.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(

        @NotNull(message = "Status is required")
        UserStatus status
) {
}
