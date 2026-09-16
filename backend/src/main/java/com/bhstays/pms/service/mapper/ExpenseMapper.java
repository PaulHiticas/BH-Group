package com.bhstays.pms.service.mapper;

import com.bhstays.pms.domain.Expense;
import com.bhstays.pms.dto.expense.ExpenseResponse;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

    public ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getProperty().getId(),
                expense.getProperty().getName(),
                expense.getMaintenanceTicket() != null ? expense.getMaintenanceTicket().getId() : null,
                expense.getCategory(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getVendor(),
                expense.getExpenseDate(),
                expense.getNotes(),
                expense.isChargeToOwner(),
                expense.getReceiptUrl(),
                expense.getCreatedBy() != null
                        ? expense.getCreatedBy().getFirstName() + " " + expense.getCreatedBy().getLastName()
                        : null,
                expense.getCreatedAt());
    }
}
