package com.bhstays.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bhstays.pms.common.exception.BadRequestException;
import com.bhstays.pms.common.exception.ResourceNotFoundException;
import com.bhstays.pms.domain.PaymentStatus;
import com.bhstays.pms.dto.report.CommissionSummaryCurrencyTotals;
import com.bhstays.pms.dto.report.CommissionSummaryResponse;
import com.bhstays.pms.dto.report.PropertyCommissionReportResponse;
import com.bhstays.pms.dto.report.UnconfiguredPropertyResponse;
import com.bhstays.pms.repository.PaymentRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.projection.PropertyCommissionSettings;
import com.bhstays.pms.repository.projection.ReservationPaymentTotals;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class PropertyCommissionReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2030, 5, 1);
    private static final LocalDate TO = LocalDate.of(2030, 5, 31);

    @Mock private PropertyRepository propertyRepository;
    @Mock private PaymentRepository paymentRepository;

    private PropertyCommissionReportService service;

    private final PropertyCommissionSettings twenty =
            new PropertyCommissionSettings(UUID.randomUUID(), "Apartament 20%", new BigDecimal("20.00"));
    private final PropertyCommissionSettings ten =
            new PropertyCommissionSettings(UUID.randomUUID(), "Apartament 10%", new BigDecimal("10.00"));
    private final PropertyCommissionSettings unconfigured =
            new PropertyCommissionSettings(UUID.randomUUID(), "Apartament fără comision", null);
    private final PropertyCommissionSettings idle =
            new PropertyCommissionSettings(UUID.randomUUID(), "Apartament fără încasări", null);

    @BeforeEach
    void setUp() {
        service = new PropertyCommissionReportService(propertyRepository, paymentRepository);
    }

    private static ReservationPaymentTotals paid(PropertyCommissionSettings property, String currency, String total,
                                                 String accommodation, String captured, String refunded) {
        return new ReservationPaymentTotals(property.id(), UUID.randomUUID(), currency, currency,
                new BigDecimal(total), new BigDecimal(accommodation), new BigDecimal(captured), new BigDecimal(refunded));
    }

    @Test
    void summary_appliesEachPropertysOwnPercentAndKeepsCurrenciesApart() {
        when(propertyRepository.findAllCommissionSettings()).thenReturn(List.of(twenty, ten, unconfigured, idle));
        when(paymentRepository.sumCapturedByReservation(any(), eq(FROM), eq(TO))).thenReturn(List.of(
                paid(twenty, "RON", "500.00", "400.00", "500.00", "0"),        // BH 80, owner 420
                paid(ten, "RON", "1000.00", "800.00", "1000.00", "500.00"),    // net 500, base 400, BH 40, owner 460
                paid(ten, "EUR", "200.00", "150.00", "200.00", "0"),          // BH 15, owner 185
                paid(unconfigured, "RON", "300.00", "250.00", "300.00", "0")  // no split
        ));

        CommissionSummaryResponse summary = service.summary(FROM, TO);

        assertThat(summary.totals()).extracting(CommissionSummaryCurrencyTotals::currency).containsExactly("EUR", "RON");
        CommissionSummaryCurrencyTotals eur = summary.totals().get(0);
        assertThat(eur.propertiesNetRevenue()).isEqualByComparingTo("200.00");
        assertThat(eur.bhStaysRevenue()).isEqualByComparingTo("15.00");
        assertThat(eur.ownersAmount()).isEqualByComparingTo("185.00");
        assertThat(eur.includedPropertyCount()).isEqualTo(1);
        assertThat(eur.unconfiguredPropertyCount()).isZero();

        CommissionSummaryCurrencyTotals ron = summary.totals().get(1);
        assertThat(ron.propertiesNetRevenue()).isEqualByComparingTo("1300.00");
        assertThat(ron.bhStaysRevenue()).isEqualByComparingTo("120.00");
        assertThat(ron.ownersAmount()).isEqualByComparingTo("880.00");
        assertThat(ron.includedPropertyCount()).isEqualTo(3);
        assertThat(ron.unconfiguredPropertyCount()).isEqualTo(1);
        assertThat(ron.unconfiguredNetRevenue()).isEqualByComparingTo("300.00");
        // reconciles: net = BH Stays + owners + not-yet-configured
        assertThat(ron.bhStaysRevenue().add(ron.ownersAmount()).add(ron.unconfiguredNetRevenue()))
                .isEqualByComparingTo(ron.propertiesNetRevenue());

        assertThat(summary.unconfiguredProperties()).extracting(UnconfiguredPropertyResponse::propertyId)
                .containsExactly(unconfigured.id(), idle.id());
    }

    @Test
    void summary_onlyQueriesCapturedPaymentStatuses() {
        when(propertyRepository.findAllCommissionSettings()).thenReturn(List.of());
        when(paymentRepository.sumCapturedByReservation(any(), any(), any())).thenReturn(List.of());

        service.summary(null, null);

        verify(paymentRepository).sumCapturedByReservation(
                eq(Set.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED)),
                eq(null), eq(null));
    }

    @Test
    void propertyReport_returnsOneLinePerCurrency() {
        when(propertyRepository.findCommissionSettings(ten.id())).thenReturn(Optional.of(ten));
        when(paymentRepository.sumCapturedByReservationForProperty(eq(ten.id()), any(), eq(FROM), eq(TO)))
                .thenReturn(List.of(
                        paid(ten, "RON", "1000.00", "800.00", "1000.00", "0"),
                        paid(ten, "EUR", "200.00", "150.00", "200.00", "0")));

        PropertyCommissionReportResponse report = service.propertyReport(ten.id(), FROM, TO);

        assertThat(report.commissionConfigured()).isTrue();
        assertThat(report.commissionPercent()).isEqualByComparingTo("10.00");
        assertThat(report.currencies()).hasSize(2);
        assertThat(report.currencies().get(1).bhStaysRevenue()).isEqualByComparingTo("80.00");
    }

    @Test
    void propertyReport_unknownPropertyIsNotFound() {
        UUID missing = UUID.randomUUID();
        when(propertyRepository.findCommissionSettings(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.propertyReport(missing, FROM, TO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reversedPeriod_isRejected() {
        assertThatThrownBy(() -> service.summary(TO, FROM)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.propertyReport(ten.id(), TO, FROM)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(paymentRepository);
    }
}
