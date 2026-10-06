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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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
 * The single source of truth for collected money: only captured payments
 * count (pending, processing, failed, cancelled and expired card attempts
 * never do, and a booking hold has no captured payment), minus successful
 * refunds. Reservations are attributed to the period by check-in date.
 * See {@link PropertyCommissionCalculator} for the formulas.
 *
 * <p>The property report, the dashboard, /finance
 * ({@link FinancialReportService}) and owner statements
 * ({@link OwnerFinancialsService}) all take their per-property, per-currency
 * figures from {@link #linesForAllProperties} / {@link #linesForProperties},
 * so the same property and period can never show different numbers.
 *
 * <p>Each call runs a fixed number of queries - property settings, one
 * aggregate over payments - however many properties and reservations
 * there are.
 */
@Service
@RequiredArgsConstructor
public class PropertyCommissionReportService {

    /** Captured at some point; REFUNDED is kept so its capture and refund both show in the totals. */
    static final Set<PaymentStatus> CAPTURED_STATUSES =
            Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED);

    /** Bounds used for an open period, so the queries never bind a null date. */
    static final LocalDate OPEN_START = LocalDate.of(1900, 1, 1);
    static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PropertyCommissionReportResponse propertyReport(UUID propertyId, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        PropertyCommissionSettings property = propertyRepository.findCommissionSettings(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        List<PropertyCommissionCurrencyResponse> lines =
                linesForProperties(List.of(property), from, to).getOrDefault(property.id(), List.of());

        return new PropertyCommissionReportResponse(
                property.id(), property.name(), from, to,
                property.commissionPercent() != null ? PropertyCommissionCalculator.money(property.commissionPercent()) : null,
                property.commissionPercent() != null,
                lines);
    }

    @Transactional(readOnly = true)
    public CommissionSummaryResponse summary(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        List<PropertyCommissionSettings> properties = propertyRepository.findAllCommissionSettings();
        Map<UUID, List<PropertyCommissionCurrencyResponse>> linesByProperty =
                linesForAllProperties(properties, from, to);

        List<UnconfiguredPropertyResponse> unconfigured = properties.stream()
                .filter(property -> property.commissionPercent() == null)
                .map(property -> new UnconfiguredPropertyResponse(property.id(), property.name()))
                .toList();

        return new CommissionSummaryResponse(from, to, totalsByCurrency(linesByProperty.values()), unconfigured);
    }

    /**
     * Per-property, per-currency lines for every given property, from one
     * portfolio-wide aggregate. Properties with no captured payment in the
     * period have no entry.
     */
    @Transactional(readOnly = true)
    public Map<UUID, List<PropertyCommissionCurrencyResponse>> linesForAllProperties(
            Collection<PropertyCommissionSettings> properties, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        return toLines(properties,
                paymentRepository.sumCapturedByReservation(CAPTURED_STATUSES, startOf(from), endOf(to)));
    }

    /** Same as {@link #linesForAllProperties}, querying only the given properties. */
    @Transactional(readOnly = true)
    public Map<UUID, List<PropertyCommissionCurrencyResponse>> linesForProperties(
            Collection<PropertyCommissionSettings> properties, LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        if (properties.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = properties.stream().map(PropertyCommissionSettings::id).toList();
        return toLines(properties,
                paymentRepository.sumCapturedByReservationForProperties(ids, CAPTURED_STATUSES, startOf(from), endOf(to)));
    }

    /** Portfolio totals, one per currency, sorted by currency. */
    public static List<CommissionSummaryCurrencyTotals> totalsByCurrency(
            Collection<List<PropertyCommissionCurrencyResponse>> linesByProperty) {
        Map<String, List<PropertyCommissionCurrencyResponse>> byCurrency = new TreeMap<>();
        for (List<PropertyCommissionCurrencyResponse> lines : linesByProperty) {
            for (PropertyCommissionCurrencyResponse line : lines) {
                byCurrency.computeIfAbsent(line.currency(), currency -> new ArrayList<>()).add(line);
            }
        }
        return byCurrency.entrySet().stream()
                .map(entry -> PropertyCommissionCalculator.totals(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<UUID, List<PropertyCommissionCurrencyResponse>> toLines(
            Collection<PropertyCommissionSettings> properties, List<ReservationPaymentTotals> rows) {
        Map<UUID, List<ReservationPaymentTotals>> rowsByProperty = rows.stream()
                .collect(Collectors.groupingBy(ReservationPaymentTotals::propertyId, LinkedHashMap::new,
                        Collectors.toList()));

        Map<UUID, List<PropertyCommissionCurrencyResponse>> result = new LinkedHashMap<>();
        for (PropertyCommissionSettings property : properties) {
            List<ReservationPaymentTotals> propertyRows = rowsByProperty.get(property.id());
            if (propertyRows != null) {
                result.put(property.id(),
                        PropertyCommissionCalculator.calculate(property.commissionPercent(), propertyRows));
            }
        }
        return result;
    }

    static LocalDate startOf(LocalDate from) {
        return from != null ? from : OPEN_START;
    }

    static LocalDate endOf(LocalDate to) {
        return to != null ? to : OPEN_END;
    }

    static void validatePeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("The start date must not be after the end date");
        }
    }
}
