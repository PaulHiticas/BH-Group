package com.bhstays.pms.repository;

import com.bhstays.pms.domain.OwnerStatementLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerStatementLineRepository extends JpaRepository<OwnerStatementLine, UUID> {

    List<OwnerStatementLine> findByStatementIdOrderByPropertyNameAsc(UUID statementId);
}
