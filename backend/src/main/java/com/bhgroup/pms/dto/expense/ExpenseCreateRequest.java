package com.bhgroup.pms.dto.expense;

import com.bhgroup.pms.domain.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseCreateRequest(

        @NotNull(message = "Property is required")
        UUID propertyId,

        UUID maintenanceTicketId,

        ExpenseCategory category,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        String currency,

        String vendor,

        @NotNull(message = "Expense date is required")
        LocalDate expenseDate,

        String notes,

        boolean chargeToOwner
) {
}
