package com.bhgroup.pms.service;

import com.bhgroup.pms.domain.PaymentStatus;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.dto.report.FinancialReportCurrencyTotals;
import com.bhgroup.pms.dto.report.FinancialReportRowResponse;
import com.bhgroup.pms.dto.report.FinancialReportSummaryResponse;
import com.bhgroup.pms.repository.ExpenseRepository;
import com.bhgroup.pms.repository.PaymentRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revenue here is net captured payments (amount minus successful refunds),
 * never a reservation's {@code totalAmount} - a CONFIRMED reservation can
 * still be unpaid, and this report must not count money that was never
 * actually received. Amounts are grouped by currency throughout (a property
 * can have payments/expenses in more than one currency) rather than summed
 * together, since there's no FX conversion here.
 */
@Service
@RequiredArgsConstructor
public class FinancialReportService {

    private static final Set<PaymentStatus> NET_PAID_STATUSES =
            Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED);
    private static final String DEFAULT_CURRENCY = "RON";

    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public FinancialReportSummaryResponse summary(UUID propertyId, LocalDate from, LocalDate to) {
        List<Property> properties = propertyId != null
                ? propertyRepository.findById(propertyId).map(List::of).orElseGet(List::of)
                : propertyRepository.findAll();

        List<FinancialReportRowResponse> rows = properties.stream()
                .flatMap(property -> buildRows(property, from, to).stream())
                .toList();

        Map<String, List<FinancialReportRowResponse>> rowsByCurrency = rows.stream()
                .collect(Collectors.groupingBy(FinancialReportRowResponse::currency, LinkedHashMap::new, Collectors.toList()));

        List<FinancialReportCurrencyTotals> totals = rowsByCurrency.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new FinancialReportCurrencyTotals(
                        entry.getKey(),
                        sum(entry.getValue(), FinancialReportRowResponse::grossRevenue),
                        sum(entry.getValue(), FinancialReportRowResponse::commissionAmount),
                        sum(entry.getValue(), FinancialReportRowResponse::expensesTotal),
                        sum(entry.getValue(), FinancialReportRowResponse::netProfit)))
                .toList();

        return new FinancialReportSummaryResponse(rows, totals);
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

    /** One row per currency that had any captured revenue or expense for this property in the period. */
    private List<FinancialReportRowResponse> buildRows(Property property, LocalDate from, LocalDate to) {
        Map<String, BigDecimal> revenueByCurrency = toCurrencyMap(
                paymentRepository.sumNetPaidByPropertyGroupedByCurrency(property.getId(), NET_PAID_STATUSES, from, to));
        Map<String, BigDecimal> expensesByCurrency = toCurrencyMap(
                expenseRepository.sumForPropertyGroupedByCurrency(property.getId(), from, to));

        Set<String> currencies = new TreeSet<>();
        currencies.addAll(revenueByCurrency.keySet());
        currencies.addAll(expensesByCurrency.keySet());
        if (currencies.isEmpty()) {
            currencies.add(DEFAULT_CURRENCY);
        }

        String ownerName = property.getOwner() != null
                ? property.getOwner().getFirstName() + " " + property.getOwner().getLastName()
                : null;

        return currencies.stream()
                .map(currency -> {
                    BigDecimal grossRevenue = revenueByCurrency.getOrDefault(currency, BigDecimal.ZERO);
                    BigDecimal commissionAmount = property.getCommissionPercent() != null
                            ? grossRevenue.multiply(property.getCommissionPercent())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal expensesTotal = expensesByCurrency.getOrDefault(currency, BigDecimal.ZERO);
                    BigDecimal netProfit = grossRevenue.subtract(expensesTotal);

                    return new FinancialReportRowResponse(property.getId(), property.getName(), ownerName,
                            grossRevenue, commissionAmount, expensesTotal, netProfit, currency);
                })
                .toList();
    }

    private Map<String, BigDecimal> toCurrencyMap(List<Object[]> rows) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    private BigDecimal sum(List<FinancialReportRowResponse> rows, Function<FinancialReportRowResponse, BigDecimal> extractor) {
        return rows.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
