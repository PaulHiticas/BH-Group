package com.bhgroup.pms.dto.cleaning;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CleaningTaskAssignRequest(

        @NotNull(message = "Cleaner is required")
        UUID cleanerId
) {
}
