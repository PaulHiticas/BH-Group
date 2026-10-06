package com.bhstays.pms.service;

import com.bhstays.pms.common.exception.BadRequestException;
import com.bhstays.pms.common.exception.ResourceNotFoundException;
import com.bhstays.pms.domain.PaymentStatus;
import com.bhstays.pms.dto.report.CommissionSummaryCurrencyTotals;
import com.bhstays.pms.dto.report.CommissionSummaryResponse;
import com.bhstays.pms.dto.report.PropertyCommissionCurrencyResponse;
import com.bhstays.pms.dto.report.PropertyCommissionReportResponse;
import com.bhstays.pms.dto.report.UnconfiguredPropertyResponse;
import com.bhstays.pms.repository.PaymentRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.projection.PropertyCommissionSettings;
import com.bhstays.pms.repository.projection.ReservationPaymentTotals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commission reporting on money actually collected: only captured payments
 * count (pending, failed, cancelled and expired card attempts never do,
 * and a booking hold has no captured payment), minus successful refunds.
 * Reservations are attributed to the period by check-in date, the same
 * rule as the existing financial report and owner statements. See
 * {@link PropertyCommissionCalculator} for the formulas.
 *
 * <p>Each report runs a fixed number of queries - one for the property
 * settings, one aggregate over payments - however many properties and
 * reservations there are.
 */
@Service
@RequiredArgsConstructor
public class PropertyCommissionReportService {

    /** Captured at some point; REFUNDED is kept so its capture and refund both show in the totals. */
    static final Set<PaymentStatus> CAPTURED_STATUSES =
            Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED);

    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PropertyCommissionReportResponse propertyReport(UUID propertyId, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        PropertyCommissionSettings property = propertyRepository.findCommissionSettings(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        List<ReservationPaymentTotals> rows = paymentRepository
                .sumCapturedByReservationForProperty(propertyId, CAPTURED_STATUSES, from, to);

        return new PropertyCommissionReportResponse(
                property.id(), property.name(), from, to,
                property.commissionPercent() != null ? PropertyCommissionCalculator.money(property.commissionPercent()) : null,
                property.commissionPercent() != null,
                PropertyCommissionCalculator.calculate(property.commissionPercent(), rows));
    }

    @Transactional(readOnly = true)
    public CommissionSummaryResponse summary(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        List<PropertyCommissionSettings> properties = propertyRepository.findAllCommissionSettings();
        Map<UUID, List<ReservationPaymentTotals>> rowsByProperty = paymentRepository
                .sumCapturedByReservation(CAPTURED_STATUSES, from, to).stream()
                .collect(Collectors.groupingBy(ReservationPaymentTotals::propertyId, LinkedHashMap::new,
                        Collectors.toList()));

        Map<String, TotalsAccumulator> totalsByCurrency = new TreeMap<>();
        List<UnconfiguredPropertyResponse> unconfigured = new ArrayList<>();
        for (PropertyCommissionSettings property : properties) {
            if (property.commissionPercent() == null) {
                unconfigured.add(new UnconfiguredPropertyResponse(property.id(), property.name()));
            }
            List<ReservationPaymentTotals> rows = rowsByProperty.get(property.id());
            if (rows == null) {
                continue;
            }
            for (PropertyCommissionCurrencyResponse line
                    : PropertyCommissionCalculator.calculate(property.commissionPercent(), rows)) {
                totalsByCurrency.computeIfAbsent(line.currency(), currency -> new TotalsAccumulator()).add(line);
            }
        }

        List<CommissionSummaryCurrencyTotals> totals = totalsByCurrency.entrySet().stream()
                .map(entry -> entry.getValue().toTotals(entry.getKey()))
                .toList();
        return new CommissionSummaryResponse(from, to, totals, unconfigured);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("The start date must not be after the end date");
        }
    }

    private static final class TotalsAccumulator {
        private BigDecimal net = BigDecimal.ZERO;
        private BigDecimal bhStays = BigDecimal.ZERO;
        private BigDecimal owners = BigDecimal.ZERO;
        private BigDecimal unconfiguredNet = BigDecimal.ZERO;
        private BigDecimal unallocatedNet = BigDecimal.ZERO;
        private int properties;
        private int unconfiguredProperties;

        void add(PropertyCommissionCurrencyResponse line) {
            properties++;
            net = net.add(line.netRevenue());
            unallocatedNet = unallocatedNet.add(line.unallocatedNetRevenue());
            if (line.commissionConfigured()) {
                bhStays = bhStays.add(line.bhStaysRevenue());
                owners = owners.add(line.ownerAmount());
            } else {
                unconfiguredProperties++;
                unconfiguredNet = unconfiguredNet.add(line.netRevenue());
            }
        }

        CommissionSummaryCurrencyTotals toTotals(String currency) {
            return new CommissionSummaryCurrencyTotals(currency,
                    PropertyCommissionCalculator.money(net),
                    PropertyCommissionCalculator.money(bhStays),
                    PropertyCommissionCalculator.money(owners),
                    properties,
                    unconfiguredProperties,
                    PropertyCommissionCalculator.money(unconfiguredNet),
                    PropertyCommissionCalculator.money(unallocatedNet));
        }
    }
}
