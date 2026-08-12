package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.repository.ExpenseRepository;
import com.bhgroup.pms.repository.PaymentRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerFinancialsServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ExpenseRepository expenseRepository;

    private OwnerFinancialsService ownerFinancialsService;
    private UUID ownerId;
    private Property property;

    @BeforeEach
    void setUp() {
        ownerFinancialsService = new OwnerFinancialsService(propertyRepository, paymentRepository, expenseRepository);
        ownerId = UUID.randomUUID();
        property = Property.builder().name("Casa Mare").commissionPercent(new BigDecimal("20")).build();
        property.setId(UUID.randomUUID());
        when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(property));
    }

    @Test
    void computeForOwner_netPayoutSubtractsCommissionAndOwnerChargeableExpensesOnly() {
        when(paymentRepository.sumNetPaidByPropertyGroupedByCurrency(eq(property.getId()), any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"RON", new BigDecimal("1000.00")}));
        when(expenseRepository.sumChargeableToOwnerForPropertyGroupedByCurrency(eq(property.getId()), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"RON", new BigDecimal("100.00")}));

        var results = ownerFinancialsService.computeForOwner(ownerId, null, null);

        assertThat(results).hasSize(1);
        var row = results.get(0);
        assertThat(row.currency()).isEqualTo("RON");
        assertThat(row.grossRevenue()).isEqualByComparingTo("1000.00");
        assertThat(row.commissionAmount()).isEqualByComparingTo("200.00");
        assertThat(row.expensesTotal()).isEqualByComparingTo("100.00");
        // 1000 - 200 (20% commission) - 100 (owner-chargeable expenses) = 700
        assertThat(row.netPayout()).isEqualByComparingTo("700.00");
    }

    @Test
    void computeForOwner_skipsPropertyWithNoActivity() {
        when(paymentRepository.sumNetPaidByPropertyGroupedByCurrency(eq(property.getId()), any(), any(), any()))
                .thenReturn(List.of());
        when(expenseRepository.sumChargeableToOwnerForPropertyGroupedByCurrency(eq(property.getId()), any(), any()))
                .thenReturn(List.of());

        var results = ownerFinancialsService.computeForOwner(ownerId, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).grossRevenue()).isEqualByComparingTo("0");
        assertThat(results.get(0).netPayout()).isEqualByComparingTo("0");
    }
}
