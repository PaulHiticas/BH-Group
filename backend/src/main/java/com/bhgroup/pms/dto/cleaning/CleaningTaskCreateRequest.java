package com.bhgroup.pms.dto.cleaning;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CleaningTaskCreateRequest(

        @NotNull(message = "Property is required")
        UUID propertyId,

        @NotNull(message = "Scheduled date is required")
        LocalDate scheduledDate,

        UUID assignedCleanerId,

        String notes
) {
}
