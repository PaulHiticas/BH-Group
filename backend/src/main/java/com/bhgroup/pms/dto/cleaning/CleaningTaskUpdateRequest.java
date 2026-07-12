package com.bhgroup.pms.dto.cleaning;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CleaningTaskUpdateRequest(
        LocalDate scheduledDate,
        String notes,
        BigDecimal cost,
        Integer estimatedMinutes
) {
}
