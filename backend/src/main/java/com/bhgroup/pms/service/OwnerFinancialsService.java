package com.bhgroup.pms.service;

import com.bhgroup.pms.domain.PaymentStatus;
import com.bhgroup.pms.domain.Property;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import com.bhgroup.pms.repository.ExpenseRepository;
import com.bhgroup.pms.repository.PaymentRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single source of truth for "what does BH Group owe this owner" - used by
 * both the owner dashboard summary and owner statement generation, so the
 * two never drift apart (they used to: the dashboard summed
 * {@code reservation.totalAmount}, which counts confirmed-but-unpaid
 * bookings as revenue, while the financial report correctly counts only
 * captured payments net of refunds - see {@link FinancialReportService}).
 *
 * Deliberately does not reuse {@link FinancialReportService}'s row-building:
 * that report's expense total is company-wide (every expense on the
 * property), while an owner's payout must only be reduced by expenses
 * explicitly flagged {@code chargeToOwner}.
 */
@Service
@RequiredArgsConstructor
public class OwnerFinancialsService {

    private static final Set<PaymentStatus> NET_PAID_STATUSES =
            Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED);
    private static final String DEFAULT_CURRENCY = "RON";

    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;

    public record PropertyFinancials(
            UUID propertyId,
            String propertyName,
            String currency,
            BigDecimal grossRevenue,
            BigDecimal commissionAmount,
            BigDecimal expensesTotal,
            BigDecimal netPayout) {
    }

    /** One entry per property per currency that had any revenue or owner-chargeable expense in the period. */
    @Transactional(readOnly = true)
    public List<PropertyFinancials> computeForOwner(UUID ownerId, LocalDate from, LocalDate to) {
        return propertyRepository.findByOwnerId(ownerId).stream()
                .flatMap(property -> computeForProperty(property, from, to).stream())
                .toList();
    }

    /**
     * Convenience for single-property, currency-agnostic display (e.g. an
     * owner's property list card) - sums net captured payments across
     * whatever currencies exist rather than keeping them separate, same
     * simplification the owner dashboard summary already made.
     */
    @Transactional(readOnly = true)
    public BigDecimal sumGrossRevenueForProperty(UUID propertyId, LocalDate from, LocalDate to) {
        return toCurrencyMap(paymentRepository.sumNetPaidByPropertyGroupedByCurrency(propertyId, NET_PAID_STATUSES, from, to))
                .values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<PropertyFinancials> computeForProperty(Property property, LocalDate from, LocalDate to) {
        Map<String, BigDecimal> revenueByCurrency = toCurrencyMap(
                paymentRepository.sumNetPaidByPropertyGroupedByCurrency(property.getId(), NET_PAID_STATUSES, from, to));
        Map<String, BigDecimal> expensesByCurrency = toCurrencyMap(
                expenseRepository.sumChargeableToOwnerForPropertyGroupedByCurrency(property.getId(), from, to));

        Set<String> currencies = new TreeSet<>();
        currencies.addAll(revenueByCurrency.keySet());
        currencies.addAll(expensesByCurrency.keySet());
        if (currencies.isEmpty()) {
            currencies.add(DEFAULT_CURRENCY);
        }

        return currencies.stream()
                .map(currency -> {
                    BigDecimal grossRevenue = revenueByCurrency.getOrDefault(currency, BigDecimal.ZERO);
                    BigDecimal commissionAmount = property.getCommissionPercent() != null
                            ? grossRevenue.multiply(property.getCommissionPercent())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal expensesTotal = expensesByCurrency.getOrDefault(currency, BigDecimal.ZERO);
                    BigDecimal netPayout = grossRevenue.subtract(commissionAmount).subtract(expensesTotal);

                    return new PropertyFinancials(property.getId(), property.getName(), currency,
                            grossRevenue, commissionAmount, expensesTotal, netPayout);
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
}
