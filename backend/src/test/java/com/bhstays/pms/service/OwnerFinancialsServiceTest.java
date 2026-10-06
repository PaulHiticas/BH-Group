package com.bhstays.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bhstays.pms.dto.owner.OwnerRevenueLine;
import com.bhstays.pms.repository.ExpenseRepository;
import com.bhstays.pms.repository.PaymentRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.projection.PropertyCommissionSettings;
import com.bhstays.pms.repository.projection.PropertyCurrencyAmount;
import com.bhstays.pms.repository.projection.ReservationPaymentTotals;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Runs on the real shared calculation; only the repositories are mocked. */
@ExtendWith(MockitoExtension.class)
class OwnerFinancialsServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ExpenseRepository expenseRepository;

    private OwnerFinancialsService ownerFinancialsService;
    private final UUID ownerId = UUID.randomUUID();
    private final PropertyCommissionSettings casaMare = new PropertyCommissionSettings(UUID.randomUUID(),
            "Casa Mare", new BigDecimal("20.00"), ownerId, "Maria", "Ionescu");
    private final PropertyCommissionSettings unset = new PropertyCommissionSettings(UUID.randomUUID(),
            "Casa Mică", null, ownerId, "Maria", "Ionescu");

    @BeforeEach
    void setUp() {
        ownerFinancialsService = new OwnerFinancialsService(propertyRepository, expenseRepository,
                new PropertyCommissionReportService(propertyRepository, paymentRepository));
    }

    private ReservationPaymentTotals paid(PropertyCommissionSettings property, String currency, String total,
                                          String accommodation, String captured, String refunded) {
        return new ReservationPaymentTotals(property.id(), UUID.randomUUID(), currency, currency,
                new BigDecimal(total), new BigDecimal(accommodation), new BigDecimal(captured), new BigDecimal(refunded));
    }

    @Test
    void payoutIsOwnerAmountMinusOwnerChargeableExpenses_commissionOnAccommodationOnly() {
        when(propertyRepository.findCommissionSettingsByOwnerId(ownerId)).thenReturn(List.of(casaMare));
        // 1000 = 800 accommodation + 200 cleaning/extra guest; 20% of 800 = 160 (not 200 on the gross)
        when(paymentRepository.sumCapturedByReservationForProperties(eq(List.of(casaMare.id())), any(), any(), any()))
                .thenReturn(List.of(paid(casaMare, "RON", "1000.00", "800.00", "1000.00", "0")));
        when(expenseRepository.sumChargeableToOwnerGroupedByPropertyAndCurrency(eq(ownerId), any(), any()))
                .thenReturn(List.of(new PropertyCurrencyAmount(casaMare.id(), "RON", new BigDecimal("100.00"))));

        var rows = ownerFinancialsService.computeForOwner(ownerId, null, null);

        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.netRevenue()).isEqualByComparingTo("1000.00");
        assertThat(row.commissionableBase()).isEqualByComparingTo("800.00");
        assertThat(row.bhStaysCommission()).isEqualByComparingTo("160.00");
        assertThat(row.ownerAmount()).isEqualByComparingTo("840.00");
        assertThat(row.expensesTotal()).isEqualByComparingTo("100.00");
        assertThat(row.netPayout()).isEqualByComparingTo("740.00");
    }

    @Test
    void propertiesWithoutActivityAreLeftOut_andCurrenciesStaySeparate() {
        PropertyCommissionSettings idle = new PropertyCommissionSettings(UUID.randomUUID(), "Fără activitate",
                new BigDecimal("10.00"), ownerId, "Maria", "Ionescu");
        when(propertyRepository.findCommissionSettingsByOwnerId(ownerId)).thenReturn(List.of(casaMare, idle));
        when(paymentRepository.sumCapturedByReservationForProperties(any(), any(), any(), any())).thenReturn(List.of(
                paid(casaMare, "RON", "500.00", "400.00", "500.00", "0"),
                paid(casaMare, "EUR", "100.00", "100.00", "100.00", "0")));
        when(expenseRepository.sumChargeableToOwnerGroupedByPropertyAndCurrency(any(), any(), any())).thenReturn(List.of());

        var rows = ownerFinancialsService.computeForOwner(ownerId, null, null);

        assertThat(rows).extracting(OwnerFinancialsService.PropertyFinancials::currency).containsExactly("EUR", "RON");
        assertThat(rows).extracting(OwnerFinancialsService.PropertyFinancials::propertyId).containsOnly(casaMare.id());
    }

    @Test
    void aggregate_hasNoSplitWhileAPropertyWithCollectedMoneyAwaitsItsCommission() {
        when(propertyRepository.findCommissionSettingsByOwnerId(ownerId)).thenReturn(List.of(casaMare, unset));
        when(paymentRepository.sumCapturedByReservationForProperties(any(), any(), any(), any())).thenReturn(List.of(
                paid(casaMare, "RON", "500.00", "400.00", "500.00", "0"),
                paid(unset, "RON", "300.00", "300.00", "300.00", "0")));
        when(expenseRepository.sumChargeableToOwnerGroupedByPropertyAndCurrency(any(), any(), any())).thenReturn(List.of());

        OwnerRevenueLine ron = OwnerFinancialsService.aggregate("RON",
                ownerFinancialsService.computeForOwner(ownerId, null, null));

        assertThat(ron.netRevenue()).isEqualByComparingTo("800.00");
        assertThat(ron.unconfiguredNetRevenue()).isEqualByComparingTo("300.00");
        assertThat(ron.bhStaysCommission()).isNull();
        assertThat(ron.ownerAmount()).isNull();
        assertThat(ron.netPayout()).isNull();
    }

    @Test
    void aggregate_expenseOnlyPropertyWithoutPercentHasNothingToSplit() {
        when(propertyRepository.findCommissionSettingsByOwnerId(ownerId)).thenReturn(List.of(casaMare, unset));
        when(paymentRepository.sumCapturedByReservationForProperties(any(), any(), any(), any())).thenReturn(List.of(
                paid(casaMare, "RON", "500.00", "400.00", "500.00", "0")));
        when(expenseRepository.sumChargeableToOwnerGroupedByPropertyAndCurrency(any(), any(), any())).thenReturn(List.of(
                new PropertyCurrencyAmount(unset.id(), "RON", new BigDecimal("30.00"))));

        OwnerRevenueLine ron = OwnerFinancialsService.aggregate("RON",
                ownerFinancialsService.computeForOwner(ownerId, null, null));

        assertThat(ron.bhStaysCommission()).isEqualByComparingTo("80.00");
        assertThat(ron.ownerAmount()).isEqualByComparingTo("420.00");
        assertThat(ron.expensesTotal()).isEqualByComparingTo("30.00");
        assertThat(ron.netPayout()).isEqualByComparingTo("390.00");
    }

    @Test
    void ownerWithoutPropertiesHasNoRows() {
        when(propertyRepository.findCommissionSettingsByOwnerId(ownerId)).thenReturn(List.of());

        assertThat(ownerFinancialsService.computeForOwner(ownerId, null, null)).isEmpty();
    }
}
