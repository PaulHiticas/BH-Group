package com.bhgroup.pms.dto.cleaning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChecklistItemUpdateRequest(

        @NotBlank(message = "Item label is required")
        String label,

        @NotNull(message = "Checked is required")
        Boolean checked
) {
}
