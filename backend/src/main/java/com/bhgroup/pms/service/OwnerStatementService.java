package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ConflictException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.AuditAction;
import com.bhgroup.pms.domain.OwnerStatement;
import com.bhgroup.pms.domain.OwnerStatementLine;
import com.bhgroup.pms.domain.OwnerStatementStatus;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.ownerstatement.OwnerStatementResponse;
import com.bhgroup.pms.dto.ownerstatement.OwnerStatementSummaryResponse;
import com.bhgroup.pms.repository.OwnerStatementLineRepository;
import com.bhgroup.pms.repository.OwnerStatementRepository;
import com.bhgroup.pms.repository.OwnerStatementSpecifications;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.OwnerStatementMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generation is a one-shot, explicit admin action - not a live recomputed
 * view - because a statement is a record of what was owed for a period,
 * and that must stay stable even if payments/expenses for the period are
 * edited afterwards. Re-generating the exact same owner+currency+period is
 * rejected (see {@link #generate}) rather than silently producing a second,
 * conflicting record.
 */
@Service
@RequiredArgsConstructor
public class OwnerStatementService {

    private final OwnerStatementRepository ownerStatementRepository;
    private final OwnerStatementLineRepository ownerStatementLineRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final OwnerFinancialsService ownerFinancialsService;
    private final AuditService auditService;
    private final OwnerStatementMapper ownerStatementMapper;

    @Transactional
    public List<OwnerStatementResponse> generate(UUID ownerId, LocalDate periodStart, LocalDate periodEnd, User actor) {
        if (periodEnd.isBefore(periodStart)) {
            throw new BadRequestException("Perioada de sfârșit trebuie să fie după perioada de început");
        }
        User owner = userRepository.findById(ownerId)
                .filter(u -> u.getRole() == Role.OWNER)
                .orElseThrow(() -> new BadRequestException("Proprietarul nu a fost găsit"));

        var financials = ownerFinancialsService.computeForOwner(ownerId, periodStart, periodEnd);
        Map<String, List<OwnerFinancialsService.PropertyFinancials>> byCurrency = financials.stream()
                .filter(f -> f.grossRevenue().signum() != 0 || f.expensesTotal().signum() != 0)
                .collect(Collectors.groupingBy(OwnerFinancialsService.PropertyFinancials::currency, LinkedHashMap::new, Collectors.toList()));

        if (byCurrency.isEmpty()) {
            throw new BadRequestException("Nicio activitate financiară găsită pentru acest proprietar în perioada selectată");
        }

        List<OwnerStatementResponse> results = new java.util.ArrayList<>();
        for (var entry : byCurrency.entrySet()) {
            String currency = entry.getKey();
            List<OwnerFinancialsService.PropertyFinancials> rows = entry.getValue();

            if (ownerStatementRepository.findByOwnerIdAndCurrencyAndPeriodStartAndPeriodEnd(
                    ownerId, currency, periodStart, periodEnd).isPresent()) {
                throw new ConflictException(
                        "Există deja un decont " + currency + " pentru " + owner.getFirstName() + " " + owner.getLastName()
                                + " în perioada " + periodStart + " - " + periodEnd);
            }

            BigDecimal grossRevenue = sum(rows, OwnerFinancialsService.PropertyFinancials::grossRevenue);
            BigDecimal commissionAmount = sum(rows, OwnerFinancialsService.PropertyFinancials::commissionAmount);
            BigDecimal expensesTotal = sum(rows, OwnerFinancialsService.PropertyFinancials::expensesTotal);
            BigDecimal netPayout = grossRevenue.subtract(commissionAmount).subtract(expensesTotal);

            OwnerStatement statement = OwnerStatement.builder()
                    .owner(owner)
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .currency(currency)
                    .grossRevenue(grossRevenue)
                    .commissionAmount(commissionAmount)
                    .expensesTotal(expensesTotal)
                    .netPayout(netPayout)
                    .status(OwnerStatementStatus.ISSUED)
                    .generatedBy(actor)
                    .build();
            statement = ownerStatementRepository.save(statement);

            List<OwnerStatementLine> lines = new java.util.ArrayList<>();
            for (var row : rows) {
                Property property = propertyRepository.findById(row.propertyId()).orElse(null);
                lines.add(ownerStatementLineRepository.save(OwnerStatementLine.builder()
                        .statement(statement)
                        .property(property)
                        .propertyName(row.propertyName())
                        .grossRevenue(row.grossRevenue())
                        .commissionAmount(row.commissionAmount())
                        .expensesTotal(row.expensesTotal())
                        .netAmount(row.netPayout())
                        .build()));
            }

            auditService.record(AuditAction.OWNER_STATEMENT_GENERATED, actor,
                    "Decont " + currency + " generat pentru " + owner.getFirstName() + " " + owner.getLastName()
                            + " (" + periodStart + " - " + periodEnd + "), net " + netPayout + " " + currency,
                    null, null);

            results.add(ownerStatementMapper.toResponse(statement, lines));
        }
        return results;
    }

    @Transactional
    public OwnerStatementResponse markPaid(UUID statementId, String paymentReference, User actor) {
        OwnerStatement statement = findOrThrow(statementId);
        if (statement.getStatus() == OwnerStatementStatus.PAID) {
            throw new BadRequestException("Decontul este deja marcat ca plătit");
        }
        statement.setStatus(OwnerStatementStatus.PAID);
        statement.setPaidAt(Instant.now());
        statement.setPaymentReference(paymentReference);
        statement = ownerStatementRepository.save(statement);

        auditService.record(AuditAction.OWNER_STATEMENT_MARKED_PAID, actor,
                "Decont " + statement.getId() + " (" + statement.getOwner().getFirstName() + " "
                        + statement.getOwner().getLastName() + ") marcat ca plătit, " + statement.getNetPayout()
                        + " " + statement.getCurrency(),
                null, null);

        return ownerStatementMapper.toResponse(statement, linesFor(statement.getId()));
    }

    @Transactional(readOnly = true)
    public OwnerStatementResponse get(UUID id) {
        OwnerStatement statement = findOrThrow(id);
        return ownerStatementMapper.toResponse(statement, linesFor(id));
    }

    @Transactional(readOnly = true)
    public OwnerStatementResponse getForOwner(UUID ownerId, UUID id) {
        OwnerStatement statement = ownerStatementRepository.findById(id)
                .filter(s -> s.getOwner().getId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("Statement not found"));
        return ownerStatementMapper.toResponse(statement, linesFor(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OwnerStatementSummaryResponse> list(UUID ownerId, OwnerStatementStatus status, Pageable pageable) {
        Specification<OwnerStatement> spec = OwnerStatementSpecifications.combine(
                OwnerStatementSpecifications.hasOwner(ownerId),
                OwnerStatementSpecifications.hasStatus(status));
        return PageResponse.of(ownerStatementRepository.findAll(spec, pageable), ownerStatementMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<OwnerStatementSummaryResponse> listForOwner(UUID ownerId, Pageable pageable) {
        Specification<OwnerStatement> spec = OwnerStatementSpecifications.hasOwner(ownerId);
        return PageResponse.of(ownerStatementRepository.findAll(spec, pageable), ownerStatementMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public List<List<String>> exportRows(UUID ownerId, OwnerStatementStatus status) {
        Specification<OwnerStatement> spec = OwnerStatementSpecifications.combine(
                OwnerStatementSpecifications.hasOwner(ownerId),
                OwnerStatementSpecifications.hasStatus(status));
        return ownerStatementRepository.findAll(spec).stream()
                .map(s -> List.of(
                        s.getOwner().getFirstName() + " " + s.getOwner().getLastName(),
                        s.getPeriodStart().toString(),
                        s.getPeriodEnd().toString(),
                        s.getGrossRevenue().toString(),
                        s.getCommissionAmount().toString(),
                        s.getExpensesTotal().toString(),
                        s.getNetPayout().toString(),
                        s.getCurrency(),
                        s.getStatus().toString(),
                        s.getPaidAt() != null ? s.getPaidAt().toString() : ""
                ))
                .toList();
    }

    private OwnerStatement findOrThrow(UUID id) {
        return ownerStatementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Statement not found"));
    }

    private List<OwnerStatementLine> linesFor(UUID statementId) {
        return ownerStatementLineRepository.findByStatementIdOrderByPropertyNameAsc(statementId);
    }

    private BigDecimal sum(List<OwnerFinancialsService.PropertyFinancials> rows,
                            java.util.function.Function<OwnerFinancialsService.PropertyFinancials, BigDecimal> extractor) {
        return rows.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
