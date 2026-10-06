package com.bhstays.pms.service;

import com.bhstays.pms.dto.owner.OwnerRevenueLine;
import com.bhstays.pms.dto.report.PropertyCommissionCurrencyResponse;
import com.bhstays.pms.repository.ExpenseRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.projection.PropertyCommissionSettings;
import com.bhstays.pms.repository.projection.PropertyCurrencyAmount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What BH Stays owes an owner, per property and currency - used by owner
 * statement generation, the owner dashboard and the owner's property cards.
 *
 * <p>Revenue and commission are never computed here: they are the lines of
 * {@link PropertyCommissionReportService}, exactly what the property
 * report, the dashboard and /finance show for the same property and
 * period. The only thing added is the owner's side of expenses - only
 * those explicitly flagged {@code chargeToOwner} reduce the payout:
 * {@code netPayout = ownerAmount - owner-chargeable expenses}.
 */
@Service
@RequiredArgsConstructor
public class OwnerFinancialsService {

    private final PropertyRepository propertyRepository;
    private final ExpenseRepository expenseRepository;
    private final PropertyCommissionReportService commissionReportService;

    /**
     * {@code bhStaysCommission}, {@code ownerAmount} and {@code netPayout}
     * are null while the property has no commission configured.
     */
    public record PropertyFinancials(
            UUID propertyId,
            String propertyName,
            String currency,
            BigDecimal capturedTotal,
            BigDecimal refundedTotal,
            BigDecimal netRevenue,
            BigDecimal commissionableBase,
            BigDecimal commissionPercent,
            boolean commissionConfigured,
            BigDecimal bhStaysCommission,
            BigDecimal ownerAmount,
            BigDecimal unallocatedNetRevenue,
            int unallocatedReservationCount,
            BigDecimal expensesTotal,
            BigDecimal netPayout) {

        public boolean hasCollectedMoney() {
            return capturedTotal.signum() != 0;
        }

        public boolean hasActivity() {
            return hasCollectedMoney() || expensesTotal.signum() != 0;
        }

        /** Collected money that cannot be split yet because the property has no commission. */
        public boolean awaitsCommission() {
            return hasCollectedMoney() && !commissionConfigured;
        }

        /**
         * The commission to settle: the computed one, or 0 for an expense-only
         * row of a property without a percent (nothing was collected to
         * split). Only meaningful when {@link #awaitsCommission()} is false.
         */
        public BigDecimal settledCommission() {
            return bhStaysCommission != null ? bhStaysCommission : PropertyCommissionCalculator.money(BigDecimal.ZERO);
        }

        /** Same rule as {@link #settledCommission()}: net revenue - commission. */
        public BigDecimal settledOwnerAmount() {
            return ownerAmount != null ? ownerAmount : netRevenue;
        }
    }

    /**
     * Adds up one currency's rows the way a statement does. While any row
     * {@link PropertyFinancials#awaitsCommission() awaits a commission} the
     * split is unknown: commission, owner amount and payout are null and
     * {@code unconfiguredNetRevenue} says how much is waiting.
     */
    public static OwnerRevenueLine aggregate(String currency, List<PropertyFinancials> rows) {
        BigDecimal captured = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal owner = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal unconfiguredNet = BigDecimal.ZERO;
        BigDecimal unallocatedNet = BigDecimal.ZERO;
        int unallocatedReservations = 0;
        boolean complete = true;
        for (PropertyFinancials row : rows) {
            captured = captured.add(row.capturedTotal());
            refunded = refunded.add(row.refundedTotal());
            net = net.add(row.netRevenue());
            base = base.add(row.commissionableBase());
            expenses = expenses.add(row.expensesTotal());
            unallocatedNet = unallocatedNet.add(row.unallocatedNetRevenue());
            unallocatedReservations += row.unallocatedReservationCount();
            if (row.awaitsCommission()) {
                complete = false;
                unconfiguredNet = unconfiguredNet.add(row.netRevenue());
            } else {
                commission = commission.add(row.settledCommission());
                owner = owner.add(row.settledOwnerAmount());
            }
        }
        BigDecimal percent = rows.size() == 1 ? rows.get(0).commissionPercent() : null;
        return new OwnerRevenueLine(currency, captured, refunded, net, base, percent,
                complete ? commission : null,
                complete ? owner : null,
                complete ? owner.subtract(expenses) : null,
                expenses, unconfiguredNet, unallocatedNet, unallocatedReservations);
    }

    /** One entry per property per currency with collected money or owner-chargeable expenses in the period. */
    @Transactional(readOnly = true)
    public List<PropertyFinancials> computeForOwner(UUID ownerId, LocalDate from, LocalDate to) {
        List<PropertyCommissionSettings> properties = propertyRepository.findCommissionSettingsByOwnerId(ownerId);
        if (properties.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<PropertyCommissionCurrencyResponse>> revenue =
                commissionReportService.linesForProperties(properties, from, to);
        Map<UUID, Map<String, BigDecimal>> expenses = new HashMap<>();
        for (PropertyCurrencyAmount amount
                : expenseRepository.sumChargeableToOwnerGroupedByPropertyAndCurrency(ownerId,
                        PropertyCommissionReportService.startOf(from), PropertyCommissionReportService.endOf(to))) {
            expenses.computeIfAbsent(amount.propertyId(), id -> new HashMap<>())
                    .merge(amount.currency(), amount.amount(), BigDecimal::add);
        }

        List<PropertyFinancials> result = new ArrayList<>();
        for (PropertyCommissionSettings property : properties) {
            result.addAll(rowsFor(property, revenue.getOrDefault(property.id(), List.of()),
                    expenses.getOrDefault(property.id(), Map.of())));
        }
        return result;
    }

    private List<PropertyFinancials> rowsFor(PropertyCommissionSettings property,
                                             List<PropertyCommissionCurrencyResponse> revenueLines,
                                             Map<String, BigDecimal> expensesByCurrency) {
        Map<String, PropertyCommissionCurrencyResponse> revenueByCurrency = new HashMap<>();
        revenueLines.forEach(line -> revenueByCurrency.put(line.currency(), line));
        Set<String> currencies = new TreeSet<>(revenueByCurrency.keySet());
        currencies.addAll(expensesByCurrency.keySet());

        List<PropertyFinancials> rows = new ArrayList<>();
        for (String currency : currencies) {
            PropertyCommissionCurrencyResponse line = revenueByCurrency.containsKey(currency)
                    ? revenueByCurrency.get(currency)
                    : PropertyCommissionCalculator.empty(currency, property.commissionPercent());
            BigDecimal expensesTotal = PropertyCommissionCalculator.money(
                    expensesByCurrency.getOrDefault(currency, BigDecimal.ZERO));
            PropertyFinancials row = new PropertyFinancials(
                    property.id(), property.name(), currency,
                    line.capturedTotal(), line.refundedTotal(), line.netRevenue(), line.commissionableBase(),
                    line.commissionPercent(), line.commissionConfigured(),
                    line.bhStaysRevenue(), line.ownerAmount(),
                    line.unallocatedNetRevenue(), line.unallocatedReservationCount(),
                    expensesTotal,
                    line.ownerAmount() != null ? line.ownerAmount().subtract(expensesTotal) : null);
            if (row.hasActivity()) {
                rows.add(row);
            }
        }
        return rows;
    }
}
