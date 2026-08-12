package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.OwnerStatementLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerStatementLineRepository extends JpaRepository<OwnerStatementLine, UUID> {

    List<OwnerStatementLine> findByStatementIdOrderByPropertyNameAsc(UUID statementId);
}
