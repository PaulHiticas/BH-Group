package com.bhgroup.pms.user.dto;

import com.bhgroup.pms.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(

        @NotNull(message = "Status is required")
        UserStatus status
) {
}
