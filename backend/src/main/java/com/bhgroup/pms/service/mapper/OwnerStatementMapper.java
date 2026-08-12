package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.domain.OwnerStatement;
import com.bhgroup.pms.domain.OwnerStatementLine;
import com.bhgroup.pms.dto.ownerstatement.OwnerStatementLineResponse;
import com.bhgroup.pms.dto.ownerstatement.OwnerStatementResponse;
import com.bhgroup.pms.dto.ownerstatement.OwnerStatementSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OwnerStatementMapper {

    public OwnerStatementResponse toResponse(OwnerStatement statement, List<OwnerStatementLine> lines) {
        return new OwnerStatementResponse(
                statement.getId(),
                statement.getOwner().getId(),
                fullName(statement.getOwner().getFirstName(), statement.getOwner().getLastName()),
                statement.getPeriodStart(),
                statement.getPeriodEnd(),
                statement.getCurrency(),
                statement.getGrossRevenue(),
                statement.getCommissionAmount(),
                statement.getExpensesTotal(),
                statement.getNetPayout(),
                statement.getStatus(),
                statement.getGeneratedBy() != null
                        ? fullName(statement.getGeneratedBy().getFirstName(), statement.getGeneratedBy().getLastName())
                        : null,
                statement.getPaidAt(),
                statement.getPaymentReference(),
                statement.getCreatedAt(),
                lines.stream().map(this::toLineResponse).toList());
    }

    public OwnerStatementSummaryResponse toSummaryResponse(OwnerStatement statement) {
        return new OwnerStatementSummaryResponse(
                statement.getId(),
                statement.getOwner().getId(),
                fullName(statement.getOwner().getFirstName(), statement.getOwner().getLastName()),
                statement.getPeriodStart(),
                statement.getPeriodEnd(),
                statement.getCurrency(),
                statement.getNetPayout(),
                statement.getStatus(),
                statement.getCreatedAt(),
                statement.getPaidAt());
    }

    private OwnerStatementLineResponse toLineResponse(OwnerStatementLine line) {
        return new OwnerStatementLineResponse(
                line.getProperty() != null ? line.getProperty().getId() : null,
                line.getPropertyName(),
                line.getGrossRevenue(),
                line.getCommissionAmount(),
                line.getExpensesTotal(),
                line.getNetAmount());
    }

    private String fullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }
}
