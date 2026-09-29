package com.bhstays.pms.dto.cleaning;

import com.bhstays.pms.domain.CleaningTaskStatus;
import jakarta.validation.constraints.NotNull;

public record CleaningTaskStatusUpdateRequest(

        @NotNull(message = "Status is required")
        CleaningTaskStatus status
) {
}
