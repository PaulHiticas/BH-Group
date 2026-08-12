package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.OwnerStatement;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OwnerStatementRepository extends JpaRepository<OwnerStatement, UUID>,
        JpaSpecificationExecutor<OwnerStatement> {

    Optional<OwnerStatement> findByOwnerIdAndCurrencyAndPeriodStartAndPeriodEnd(
            UUID ownerId, String currency, LocalDate periodStart, LocalDate periodEnd);
}
