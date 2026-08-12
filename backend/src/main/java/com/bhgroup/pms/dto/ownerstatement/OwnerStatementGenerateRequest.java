package com.bhgroup.pms.dto.ownerstatement;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record OwnerStatementGenerateRequest(
        @NotNull(message = "Owner is required")
        UUID ownerId,

        @NotNull(message = "Period start is required")
        LocalDate periodStart,

        @NotNull(message = "Period end is required")
        LocalDate periodEnd
) {
}
