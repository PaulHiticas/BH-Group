package com.bhgroup.pms.dto.expense;

import com.bhgroup.pms.domain.ExpenseCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID maintenanceTicketId,
        ExpenseCategory category,
        BigDecimal amount,
        String currency,
        String vendor,
        LocalDate expenseDate,
        String notes,
        boolean chargeToOwner,
        String receiptUrl,
        String createdByName,
        Instant createdAt
) {
}
