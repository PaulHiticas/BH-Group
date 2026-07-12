package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.PropertyStatus;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.dto.report.FinancialReportRowResponse;
import com.bhgroup.pms.dto.report.FinancialReportSummaryResponse;
import com.bhgroup.pms.repository.ExpenseRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceTest {

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    private FinancialReportService financialReportService;

    @BeforeEach
    void setUp() {
        financialReportService = new FinancialReportService(propertyRepository, reservationRepository, expenseRepository);
    }

    @Test
    void summary_computesCommissionAndNetProfitForASingleProperty() {
        Property property = Property.builder()
                .name("Apartament Cluj")
                .status(PropertyStatus.ACTIVE)
                .commissionPercent(new BigDecimal("20"))
                .build();
        property.setId(UUID.randomUUID());

        Reservation booking1 = reservationWithAmount(property, new BigDecimal("500"));
        Reservation booking2 = reservationWithAmount(property, new BigDecimal("300"));

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(reservationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(booking1, booking2));
        when(expenseRepository.sumForProperty(eq(property.getId()), isNull(), isNull()))
                .thenReturn(new BigDecimal("150"));

        FinancialReportSummaryResponse summary = financialReportService.summary(property.getId(), null, null);

        assertThat(summary.rows()).hasSize(1);
        FinancialReportRowResponse row = summary.rows().get(0);

        // gross revenue: 500 + 300 = 800
        assertThat(row.grossRevenue()).isEqualByComparingTo("800");
        // commission: 800 * 20% = 160
        assertThat(row.commissionAmount()).isEqualByComparingTo("160.00");
        assertThat(row.expensesTotal()).isEqualByComparingTo("150");
        // net profit = gross revenue - expenses (commission is BH Group's own cut, not a cash outflow tracked here)
        assertThat(row.netProfit()).isEqualByComparingTo("650");

        assertThat(summary.totalGrossRevenue()).isEqualByComparingTo("800");
        assertThat(summary.totalCommission()).isEqualByComparingTo("160.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("150");
        assertThat(summary.totalNetProfit()).isEqualByComparingTo("650");
    }

    @Test
    void summary_zeroCommissionWhenPropertyHasNoCommissionPercentConfigured() {
        Property property = Property.builder().name("No commission").status(PropertyStatus.ACTIVE).build();
        property.setId(UUID.randomUUID());

        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(reservationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(reservationWithAmount(property, new BigDecimal("400"))));
        when(expenseRepository.sumForProperty(eq(property.getId()), isNull(), isNull()))
                .thenReturn(BigDecimal.ZERO);

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
        assertThat(summary.totalGrossRevenue()).isEqualByComparingTo("0");
    }

    private Reservation reservationWithAmount(Property property, BigDecimal amount) {
        Reservation reservation = Reservation.builder()
                .property(property)
                .guestFirstName("Guest")
                .guestLastName("Test")
                .totalAmount(amount)
                .currency("RON")
                .build();
        reservation.setId(UUID.randomUUID());
        return reservation;
    }
}
