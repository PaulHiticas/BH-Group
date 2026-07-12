package com.bhgroup.pms.dto.cleaning;

import com.bhgroup.pms.domain.CleaningTaskStatus;
import jakarta.validation.constraints.NotNull;

public record CleaningTaskStatusUpdateRequest(

        @NotNull(message = "Status is required")
        CleaningTaskStatus status
) {
}
