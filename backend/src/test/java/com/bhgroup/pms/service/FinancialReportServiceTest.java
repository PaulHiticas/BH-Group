package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.domain.PaymentStatus;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.dto.report.FinancialReportCurrencyTotals;
import com.bhgroup.pms.dto.report.FinancialReportRowResponse;
import com.bhgroup.pms.dto.report.FinancialReportSummaryResponse;
import com.bhgroup.pms.repository.ExpenseRepository;
import com.bhgroup.pms.repository.PaymentRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceTest {

    private static final Set<PaymentStatus> NET_PAID_STATUSES =
            Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED);

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    private FinancialReportService financialReportService;

    @BeforeEach
    void setUp() {
        financialReportService = new FinancialReportService(propertyRepository, paymentRepository, expenseRepository);
    }

    @Test
    void summary_revenueComesFromNetCapturedPayments_notReservationTotalAmount() {
        Property property = propertyWithCommission("Apartament Cluj", "20");

        // Two SUCCEEDED payments net 500 + 300 = 800 (the repository query already
        // excludes PENDING/FAILED/CANCELLED payments and nets out refunds - this
        // asserts the service only ever asks for the statuses that mean "captured").
        when(paymentRepository.sumNetPaidByPropertyGroupedByCurrency(eq(property.getId()), eq(NET_PAID_STATUSES), isNull(), isNull()))
                .thenReturn(List.<Object[]>of(new Object[] {"RON", new BigDecimal("800")}));
        when(expenseRepository.sumForPropertyGroupedByCurrency(eq(property.getId()), isNull(), isNull()))
                .thenReturn(List.<Object[]>of(new Object[] {"RON", new BigDecimal("150")}));

        FinancialReportSummaryResponse summary = financialReportService.summary(property.getId(), null, null);

        assertThat(summary.rows()).hasSize(1);
        FinancialReportRowResponse row = summary.rows().get(0);
        assertThat(row.currency()).isEqualTo("RON");
        assertThat(row.grossRevenue()).isEqualByComparingTo("800");
        assertThat(row.commissionAmount()).isEqualByComparingTo("160.00");
        assertThat(row.expensesTotal()).isEqualByComparingTo("150");
        // net profit = net captured revenue - expenses (commission is BH Group's own cut, not a cash outflow tracked here)
        assertThat(row.netProfit()).isEqualByComparingTo("650");

        assertThat(summary.totals()).hasSize(1);
        FinancialReportCurrencyTotals totals = summary.totals().get(0);
        assertThat(totals.currency()).isEqualTo("RON");
        assertThat(totals.totalGrossRevenue()).isEqualByComparingTo("800");
        assertThat(totals.totalNetProfit()).isEqualByComparingTo("650");
    }

    @Test
    void summary_differentCurrenciesProduceSeparateRowsAndTotals_neverSummedTogether() {
        Property property = propertyWithCommission("Apartament dual-currency", "10");

        when(paymentRepository.sumNetPaidByPropertyGroupedByCurrency(eq(property.getId()), eq(NET_PAID_STATUSES), isNull(), isNull()))
                .thenReturn(List.of(
                        new Object[] {"RON", new BigDecimal("1000")},
                        new Object[] {"EUR", new BigDecimal("200")}));
        when(expenseRepository.sumForPropertyGroupedByCurrency(eq(property.getId()), isNull(), isNull()))
                .thenReturn(List.<Object[]>of(new Object[] {"RON", new BigDecimal("100")}));

        FinancialReportSummaryResponse summary = financialReportService.summary(property.getId(), null, null);

        assertThat(summary.rows()).hasSize(2);
        assertThat(summary.totals()).hasSize(2);

        FinancialReportCurrencyTotals ronTotals = totalsFor(summary, "RON");
        assertThat(ronTotals.totalGrossRevenue()).isEqualByComparingTo("1000");
        assertThat(ronTotals.totalExpenses()).isEqualByComparingTo("100");

        FinancialReportCurrencyTotals eurTotals = totalsFor(summary, "EUR");
        assertThat(eurTotals.totalGrossRevenue()).isEqualByComparingTo("200");
        assertThat(eurTotals.totalExpenses()).isEqualByComparingTo("0");

        // the two currencies must never be added into a single figure
        assertThat(summary.totals().stream().map(FinancialReportCurrencyTotals::totalGrossRevenue))
                .doesNotContain(new BigDecimal("1200"));
    }

    @Test
    void summary_zeroCommissionWhenPropertyHasNoCommissionPercentConfigured() {
        Property property = Property.builder().name("No commission").status(PropertyStatus.ACTIVE).build();
        property.setId(UUID.randomUUID());

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(paymentRepository.sumNetPaidByPropertyGroupedByCurrency(eq(property.getId()), eq(NET_PAID_STATUSES), isNull(), isNull()))
                .thenReturn(List.<Object[]>of(new Object[] {"RON", new BigDecimal("400")}));
        when(expenseRepository.sumForPropertyGroupedByCurrency(eq(property.getId()), isNull(), isNull()))
                .thenReturn(List.of());

        FinancialReportSummaryResponse summary = financialReportService.summary(property.getId(), null, null);

        assertThat(summary.rows().get(0).commissionAmount()).isEqualByComparingTo("0");
        assertThat(summary.rows().get(0).netProfit()).isEqualByComparingTo("400");
    }

    @Test
    void summary_emptyRowsWhenPropertyNotFound() {
        UUID missingId = UUID.randomUUID();
        when(propertyRepository.findById(missingId)).thenReturn(Optional.empty());

        FinancialReportSummaryResponse summary = financialReportService.summary(missingId, null, null);

        assertThat(summary.rows()).isEmpty();
        assertThat(summary.totals()).isEmpty();
    }

    @Test
    void summary_defaultsToRonWithZeroAmountsWhenPropertyHasNoActivityInPeriod() {
        Property property = Property.builder().name("Quiet property").status(PropertyStatus.ACTIVE).build();
        property.setId(UUID.randomUUID());

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(paymentRepository.sumNetPaidByPropertyGroupedByCurrency(any(), any(), any(), any())).thenReturn(List.of());
        when(expenseRepository.sumForPropertyGroupedByCurrency(any(), any(), any())).thenReturn(List.of());

        FinancialReportSummaryResponse summary = financialReportService.summary(property.getId(), null, null);

        assertThat(summary.rows()).hasSize(1);
        assertThat(summary.rows().get(0).currency()).isEqualTo("RON");
        assertThat(summary.rows().get(0).grossRevenue()).isEqualByComparingTo("0");
    }

    private FinancialReportCurrencyTotals totalsFor(FinancialReportSummaryResponse summary, String currency) {
        return summary.totals().stream()
                .filter(t -> t.currency().equals(currency))
                .findFirst()
                .orElseThrow();
    }

    private Property propertyWithCommission(String name, String commissionPercent) {
        Property property = Property.builder()
                .name(name)
                .status(PropertyStatus.ACTIVE)
                .commissionPercent(new BigDecimal(commissionPercent))
                .build();
        property.setId(UUID.randomUUID());
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        return property;
    }
}
