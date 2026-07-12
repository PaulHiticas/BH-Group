package com.bhgroup.pms.dto.cleaning;

import com.bhgroup.pms.domain.CleaningTaskStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CleaningTaskResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID reservationId,
        CleaningTaskStatus status,
        UUID assignedCleanerId,
        String assignedCleanerName,
        LocalDate scheduledDate,
        String notes,
        BigDecimal cost,
        Integer estimatedMinutes,
        Integer actualMinutes,
        Instant startedAt,
        Instant completedAt,
        Map<String, Boolean> checklistResults,
        List<CleaningTaskPhotoResponse> photos,
        Instant createdAt,
        Instant updatedAt
) {
}
