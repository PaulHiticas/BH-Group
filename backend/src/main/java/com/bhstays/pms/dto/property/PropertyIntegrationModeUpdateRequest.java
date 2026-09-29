package com.bhstays.pms.dto.property;

import com.bhstays.pms.domain.IntegrationMode;
import jakarta.validation.constraints.NotNull;

public record PropertyIntegrationModeUpdateRequest(
        @NotNull(message = "Mode is required")
        IntegrationMode mode
) {
}
