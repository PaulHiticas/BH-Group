package com.bhgroup.pms.service;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.dto.report.FinancialReportRowResponse;
import com.bhgroup.pms.dto.report.FinancialReportSummaryResponse;
import com.bhgroup.pms.repository.ExpenseRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.repository.ReservationSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancialReportService {

    private final PropertyRepository propertyRepository;
    private final ReservationRepository reservationRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public FinancialReportSummaryResponse summary(UUID propertyId, LocalDate from, LocalDate to) {
        List<Property> properties = propertyId != null
                ? propertyRepository.findById(propertyId).map(List::of).orElseGet(List::of)
                : propertyRepository.findAll();

        List<FinancialReportRowResponse> rows = properties.stream()
                .map(property -> buildRow(property, from, to))
                .toList();

        BigDecimal totalGross = sum(rows, FinancialReportRowResponse::grossRevenue);
        BigDecimal totalCommission = sum(rows, FinancialReportRowResponse::commissionAmount);
        BigDecimal totalExpenses = sum(rows, FinancialReportRowResponse::expensesTotal);
        BigDecimal totalNet = sum(rows, FinancialReportRowResponse::netProfit);

        return new FinancialReportSummaryResponse(rows, totalGross, totalCommission, totalExpenses, totalNet, "RON");
    }

    @Transactional(readOnly = true)
    public List<List<String>> exportRows(UUID propertyId, LocalDate from, LocalDate to) {
        return summary(propertyId, from, to).rows().stream()
                .map(row -> List.of(
                        row.propertyName(),
                        row.ownerName() != null ? row.ownerName() : "BH Group",
                        row.grossRevenue().toString(),
                        row.commissionAmount().toString(),
                        row.expensesTotal().toString(),
                        row.netProfit().toString(),
                        row.currency()
                ))
                .toList();
    }

    private FinancialReportRowResponse buildRow(Property property, LocalDate from, LocalDate to) {
        Specification<Reservation> spec = ReservationSpecifications.combine(
                ReservationSpecifications.hasProperty(property.getId()),
                ReservationSpecifications.activeOnly(),
                ReservationSpecifications.checkInFrom(from),
                ReservationSpecifications.checkInTo(to)
        );
        BigDecimal grossRevenue = reservationRepository.findAll(spec).stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commissionAmount = property.getCommissionPercent() != null
                ? grossRevenue.multiply(property.getCommissionPercent())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal expensesTotal = expenseRepository.sumForProperty(property.getId(), from, to);
        BigDecimal netProfit = grossRevenue.subtract(expensesTotal);

        return new FinancialReportRowResponse(
                property.getId(), property.getName(),
                property.getOwner() != null ? property.getOwner().getFirstName() + " " + property.getOwner().getLastName() : null,
                grossRevenue, commissionAmount, expensesTotal, netProfit, "RON");
    }

    private BigDecimal sum(List<FinancialReportRowResponse> rows,
                            java.util.function.Function<FinancialReportRowResponse, BigDecimal> extractor) {
        return rows.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
