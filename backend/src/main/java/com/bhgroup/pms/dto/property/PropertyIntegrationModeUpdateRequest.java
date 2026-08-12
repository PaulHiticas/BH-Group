package com.bhgroup.pms.dto.property;

import com.bhgroup.pms.domain.IntegrationMode;
import jakarta.validation.constraints.NotNull;

public record PropertyIntegrationModeUpdateRequest(
        @NotNull(message = "Mode is required")
        IntegrationMode mode
) {
}
