package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ConflictException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.OwnerStatement;
import com.bhgroup.pms.domain.OwnerStatementStatus;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.ownerstatement.OwnerStatementResponse;
import com.bhgroup.pms.repository.OwnerStatementLineRepository;
import com.bhgroup.pms.repository.OwnerStatementRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.OwnerStatementMapper;
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

@ExtendWith(MockitoExtension.class)
class OwnerStatementServiceTest {

    @Mock private OwnerStatementRepository ownerStatementRepository;
    @Mock private OwnerStatementLineRepository ownerStatementLineRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private OwnerFinancialsService ownerFinancialsService;
    @Mock private AuditService auditService;
    @Mock private OwnerStatementMapper ownerStatementMapper;

    private OwnerStatementService ownerStatementService;
    private User owner;
    private User actor;
    private UUID propertyId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    @BeforeEach
    void setUp() {
        ownerStatementService = new OwnerStatementService(ownerStatementRepository, ownerStatementLineRepository,
                userRepository, propertyRepository, ownerFinancialsService, auditService, ownerStatementMapper);

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setFirstName("Maria");
        owner.setLastName("Ionescu");
        owner.setRole(Role.OWNER);

        actor = new User();
        actor.setId(UUID.randomUUID());

        propertyId = UUID.randomUUID();
        periodStart = LocalDate.of(2026, 7, 1);
        periodEnd = LocalDate.of(2026, 7, 31);

        org.mockito.Mockito.lenient().when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        org.mockito.Mockito.lenient().when(ownerStatementRepository.save(any())).thenAnswer(inv -> {
            OwnerStatement s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });
        org.mockito.Mockito.lenient().when(ownerStatementLineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient().when(ownerStatementMapper.toResponse(any(), any())).thenReturn(mock(OwnerStatementResponse.class));
    }

    @Test
    void generate_computesNetPayoutAndPersistsStatementWithLines() {
        when(ownerFinancialsService.computeForOwner(owner.getId(), periodStart, periodEnd)).thenReturn(List.of(
                new OwnerFinancialsService.PropertyFinancials(propertyId, "Casa Mare", "RON",
                        new BigDecimal("1000.00"), new BigDecimal("200.00"), new BigDecimal("50.00"), new BigDecimal("750.00"))
        ));
        when(ownerStatementRepository.findByOwnerIdAndCurrencyAndPeriodStartAndPeriodEnd(
                owner.getId(), "RON", periodStart, periodEnd)).thenReturn(Optional.empty());
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.empty());

        List<OwnerStatementResponse> result = ownerStatementService.generate(owner.getId(), periodStart, periodEnd, actor);

        assertThat(result).hasSize(1);
        verify(ownerStatementRepository).save(argThatStatement(s ->
                s.getNetPayout().compareTo(new BigDecimal("750.00")) == 0
                        && s.getGrossRevenue().compareTo(new BigDecimal("1000.00")) == 0
                        && s.getStatus() == OwnerStatementStatus.ISSUED));
        verify(ownerStatementLineRepository).save(any());
        verify(auditService).record(eq(AuditAction.OWNER_STATEMENT_GENERATED), any(), any(), any(), any());
    }

    @Test
    void generate_rejectsDuplicatePeriodForSameOwnerAndCurrency() {
        when(ownerFinancialsService.computeForOwner(owner.getId(), periodStart, periodEnd)).thenReturn(List.of(
                new OwnerFinancialsService.PropertyFinancials(propertyId, "Casa Mare", "RON",
                        new BigDecimal("1000.00"), new BigDecimal("200.00"), BigDecimal.ZERO, new BigDecimal("800.00"))
        ));
        when(ownerStatementRepository.findByOwnerIdAndCurrencyAndPeriodStartAndPeriodEnd(
                owner.getId(), "RON", periodStart, periodEnd))
                .thenReturn(Optional.of(new OwnerStatement()));

        assertThatThrownBy(() -> ownerStatementService.generate(owner.getId(), periodStart, periodEnd, actor))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void generate_rejectsWhenNoFinancialActivity() {
        when(ownerFinancialsService.computeForOwner(owner.getId(), periodStart, periodEnd)).thenReturn(List.of(
                new OwnerFinancialsService.PropertyFinancials(propertyId, "Casa Mare", "RON",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        ));

        assertThatThrownBy(() -> ownerStatementService.generate(owner.getId(), periodStart, periodEnd, actor))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void generate_rejectsInvertedPeriod() {
        assertThatThrownBy(() -> ownerStatementService.generate(owner.getId(), periodEnd, periodStart, actor))
                .isInstanceOf(BadRequestException.class);
        verify(ownerFinancialsService, never()).computeForOwner(any(), any(), any());
    }

    @Test
    void markPaid_setsStatusAndPaidAt() {
        OwnerStatement statement = OwnerStatement.builder()
                .owner(owner).currency("RON").netPayout(new BigDecimal("500")).status(OwnerStatementStatus.ISSUED)
                .build();
        statement.setId(UUID.randomUUID());
        when(ownerStatementRepository.findById(statement.getId())).thenReturn(Optional.of(statement));
        when(ownerStatementLineRepository.findByStatementIdOrderByPropertyNameAsc(statement.getId())).thenReturn(List.of());

        ownerStatementService.markPaid(statement.getId(), "OP-123", actor);

        assertThat(statement.getStatus()).isEqualTo(OwnerStatementStatus.PAID);
        assertThat(statement.getPaidAt()).isNotNull();
        assertThat(statement.getPaymentReference()).isEqualTo("OP-123");
        verify(auditService).record(eq(AuditAction.OWNER_STATEMENT_MARKED_PAID), any(), any(), any(), any());
    }

    @Test
    void markPaid_rejectsAlreadyPaidStatement() {
        OwnerStatement statement = OwnerStatement.builder()
                .owner(owner).currency("RON").netPayout(new BigDecimal("500")).status(OwnerStatementStatus.PAID)
                .build();
        statement.setId(UUID.randomUUID());
        when(ownerStatementRepository.findById(statement.getId())).thenReturn(Optional.of(statement));

        assertThatThrownBy(() -> ownerStatementService.markPaid(statement.getId(), null, actor))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getForOwner_throwsNotFoundWhenStatementBelongsToDifferentOwner() {
        User otherOwner = new User();
        otherOwner.setId(UUID.randomUUID());
        OwnerStatement statement = OwnerStatement.builder().owner(otherOwner).currency("RON").build();
        statement.setId(UUID.randomUUID());
        when(ownerStatementRepository.findById(statement.getId())).thenReturn(Optional.of(statement));

        assertThatThrownBy(() -> ownerStatementService.getForOwner(owner.getId(), statement.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private OwnerStatement argThatStatement(java.util.function.Predicate<OwnerStatement> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
