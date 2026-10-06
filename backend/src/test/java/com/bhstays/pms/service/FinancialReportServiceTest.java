package com.bhstays.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bhstays.pms.common.exception.BadRequestException;
import com.bhstays.pms.dto.report.CommissionSummaryCurrencyTotals;
import com.bhstays.pms.dto.report.FinancialReportCurrencyTotals;
import com.bhstays.pms.dto.report.FinancialReportRowResponse;
import com.bhstays.pms.dto.report.FinancialReportSummaryResponse;
import com.bhstays.pms.repository.ExpenseRepository;
import com.bhstays.pms.repository.PaymentRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.projection.PropertyCommissionSettings;
import com.bhstays.pms.repository.projection.PropertyCurrencyAmount;
import com.bhstays.pms.repository.projection.ReservationPaymentTotals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * /finance runs on the real {@link PropertyCommissionReportService} here -
 * only the repositories are mocked - so these tests exercise the same
 * formula the property report and the dashboard use.
 */
@ExtendWith(MockitoExtension.class)
class FinancialReportServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ExpenseRepository expenseRepository;

    private FinancialReportService financialReportService;
    private PropertyCommissionReportService commissionReportService;

    private final PropertyCommissionSettings cluj = new PropertyCommissionSettings(UUID.randomUUID(),
            "Apartament Cluj", new BigDecimal("20.00"), UUID.randomUUID(), "Maria", "Ionescu");
    private final PropertyCommissionSettings unset = new PropertyCommissionSettings(UUID.randomUUID(),
            "Apartament neconfigurat", null, null, null, null);

    @BeforeEach
    void setUp() {
        commissionReportService = new PropertyCommissionReportService(propertyRepository, paymentRepository);
        financialReportService = new FinancialReportService(propertyRepository, expenseRepository, commissionReportService);
    }

    private static ReservationPaymentTotals paid(PropertyCommissionSettings property, String currency, String total,
                                                 String accommodation, String captured, String refunded) {
        return new ReservationPaymentTotals(property.id(), UUID.randomUUID(), currency, currency,
                new BigDecimal(total), accommodation != null ? new BigDecimal(accommodation) : null,
                new BigDecimal(captured), new BigDecimal(refunded));
    }

    @Test
    void rowsAreTheCommissionReportLinesPlusExpenses() {
        when(propertyRepository.findCommissionSettings(cluj.id())).thenReturn(Optional.of(cluj));
        // 1000 = 800 accommodation + 200 fees, 250 refunded -> net 750, base 600, BH 120, owner 630
        when(paymentRepository.sumCapturedByReservationForProperties(eq(List.of(cluj.id())), any(), any(), any()))
                .thenReturn(List.of(paid(cluj, "RON", "1000.00", "800.00", "1000.00", "250.00")));
        when(expenseRepository.sumGroupedByPropertyAndCurrency(any(), any())).thenReturn(List.of(
                new PropertyCurrencyAmount(cluj.id(), "RON", new BigDecimal("150"))));

        FinancialReportSummaryResponse summary = financialReportService.summary(cluj.id(), null, null);

        assertThat(summary.rows()).hasSize(1);
        FinancialReportRowResponse row = summary.rows().get(0);
        assertThat(row.ownerName()).isEqualTo("Maria Ionescu");
        assertThat(row.capturedTotal()).isEqualByComparingTo("1000.00");
        assertThat(row.refundedTotal()).isEqualByComparingTo("250.00");
        assertThat(row.netRevenue()).isEqualByComparingTo("750.00");
        assertThat(row.commissionableBase()).isEqualByComparingTo("600.00");
        assertThat(row.bhStaysRevenue()).isEqualByComparingTo("120.00");
        assertThat(row.ownerAmount()).isEqualByComparingTo("630.00");
        assertThat(row.expensesTotal()).isEqualByComparingTo("150.00");
        assertThat(row.netProfit()).isEqualByComparingTo("600.00");
        // deprecated fields mirror the new ones - never the old gross-based commission (150.00)
        assertThat(row.grossRevenue()).isEqualByComparingTo(row.netRevenue());
        assertThat(row.commissionAmount()).isEqualByComparingTo(row.bhStaysRevenue());

        // the very same line the property report returns
        var reportLine = commissionReportService.propertyReport(cluj.id(), null, null).currencies().get(0);
        assertThat(reportLine.netRevenue()).isEqualByComparingTo(row.netRevenue());
        assertThat(reportLine.bhStaysRevenue()).isEqualByComparingTo(row.bhStaysRevenue());
        assertThat(reportLine.ownerAmount()).isEqualByComparingTo(row.ownerAmount());
    }

    @Test
    void totalsAreTheDashboardTotalsPerCurrency_neverMixed() {
        when(propertyRepository.findAllCommissionSettings()).thenReturn(List.of(cluj, unset));
        List<ReservationPaymentTotals> captured = List.of(
                paid(cluj, "RON", "500.00", "400.00", "500.00", "0"),
                paid(cluj, "EUR", "200.00", "100.00", "200.00", "0"),
                paid(unset, "RON", "300.00", "300.00", "300.00", "0"));
        when(paymentRepository.sumCapturedByReservation(any(), any(), any())).thenReturn(captured);
        when(expenseRepository.sumGroupedByPropertyAndCurrency(any(), any())).thenReturn(List.of(
                new PropertyCurrencyAmount(cluj.id(), "RON", new BigDecimal("100")),
                new PropertyCurrencyAmount(unset.id(), "USD", new BigDecimal("40"))));

        FinancialReportSummaryResponse summary = financialReportService.summary(null, null, null);

        assertThat(summary.totals()).extracting(FinancialReportCurrencyTotals::currency)
                .containsExactly("EUR", "RON", "USD");
        FinancialReportCurrencyTotals ron = totalsFor(summary, "RON");
        assertThat(ron.revenue().propertiesNetRevenue()).isEqualByComparingTo("800.00");
        assertThat(ron.revenue().bhStaysRevenue()).isEqualByComparingTo("80.00");
        assertThat(ron.revenue().ownersAmount()).isEqualByComparingTo("420.00");
        assertThat(ron.revenue().unconfiguredNetRevenue()).isEqualByComparingTo("300.00");
        assertThat(ron.totalExpenses()).isEqualByComparingTo("100.00");
        assertThat(ron.totalNetProfit()).isEqualByComparingTo("700.00");
        assertThat(ron.totalGrossRevenue()).isEqualByComparingTo("800.00");
        assertThat(ron.totalCommission()).isEqualByComparingTo("80.00");

        // identical to what the dashboard summary reports for the same period
        CommissionSummaryCurrencyTotals dashboardRon = commissionReportService.summary(null, null).totals().stream()
                .filter(t -> t.currency().equals("RON")).findFirst().orElseThrow();
        assertThat(ron.revenue()).isEqualTo(dashboardRon);

        FinancialReportCurrencyTotals usd = totalsFor(summary, "USD");
        assertThat(usd.revenue().propertiesNetRevenue()).isEqualByComparingTo("0.00");
        assertThat(usd.totalNetProfit()).isEqualByComparingTo("-40.00");
        assertThat(totalsFor(summary, "EUR").revenue().bhStaysRevenue()).isEqualByComparingTo("20.00");
    }

    @Test
    void unconfiguredCommissionHasNoSplit() {
        when(propertyRepository.findCommissionSettings(unset.id())).thenReturn(Optional.of(unset));
        when(paymentRepository.sumCapturedByReservationForProperties(any(), any(), any(), any()))
                .thenReturn(List.of(paid(unset, "RON", "400.00", "300.00", "400.00", "0")));
        when(expenseRepository.sumGroupedByPropertyAndCurrency(any(), any())).thenReturn(List.of());

        FinancialReportRowResponse row = financialReportService.summary(unset.id(), null, null).rows().get(0);

        assertThat(row.commissionConfigured()).isFalse();
        assertThat(row.bhStaysRevenue()).isNull();
        assertThat(row.ownerAmount()).isNull();
        assertThat(row.commissionAmount()).isEqualByComparingTo("0");
        assertThat(row.netProfit()).isEqualByComparingTo("400.00");
    }

    @Test
    void historicalReservationWithoutBreakdownIsCountedButNotCommissioned() {
        when(propertyRepository.findCommissionSettings(cluj.id())).thenReturn(Optional.of(cluj));
        when(paymentRepository.sumCapturedByReservationForProperties(any(), any(), any(), any()))
                .thenReturn(List.of(paid(cluj, "RON", "700.00", null, "700.00", "0")));
        when(expenseRepository.sumGroupedByPropertyAndCurrency(any(), any())).thenReturn(List.of());

        FinancialReportRowResponse row = financialReportService.summary(cluj.id(), null, null).rows().get(0);

        assertThat(row.netRevenue()).isEqualByComparingTo("700.00");
        assertThat(row.unallocatedNetRevenue()).isEqualByComparingTo("700.00");
        assertThat(row.unallocatedReservationCount()).isEqualTo(1);
        assertThat(row.bhStaysRevenue()).isEqualByComparingTo("0.00");
        assertThat(row.ownerAmount()).isEqualByComparingTo("700.00");
    }

    @Test
    void propertyWithoutActivityGetsOneEmptyRonRow_andUnknownPropertyNone() {
        when(propertyRepository.findCommissionSettings(cluj.id())).thenReturn(Optional.of(cluj));
        when(paymentRepository.sumCapturedByReservationForProperties(any(), any(), any(), any())).thenReturn(List.of());
        when(expenseRepository.sumGroupedByPropertyAndCurrency(any(), any())).thenReturn(List.of());

        FinancialReportSummaryResponse quiet = financialReportService.summary(cluj.id(), null, null);
        assertThat(quiet.rows()).hasSize(1);
        assertThat(quiet.rows().get(0).currency()).isEqualTo("RON");
        assertThat(quiet.rows().get(0).netRevenue()).isEqualByComparingTo("0");

        UUID missing = UUID.randomUUID();
        when(propertyRepository.findCommissionSettings(missing)).thenReturn(Optional.empty());
        FinancialReportSummaryResponse none = financialReportService.summary(missing, null, null);
        assertThat(none.rows()).isEmpty();
        assertThat(none.totals()).isEmpty();
    }

    @Test
    void reversedPeriodIsRejected() {
        assertThatThrownBy(() -> financialReportService.summary(null, LocalDate.of(2030, 2, 1), LocalDate.of(2030, 1, 1)))
                .isInstanceOf(BadRequestException.class);
    }

    private FinancialReportCurrencyTotals totalsFor(FinancialReportSummaryResponse summary, String currency) {
        return summary.totals().stream().filter(t -> t.currency().equals(currency)).findFirst().orElseThrow();
    }
}
