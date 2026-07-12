package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.property.id = :propertyId
              and (:from is null or e.expenseDate >= :from)
              and (:to is null or e.expenseDate <= :to)
            """)
    BigDecimal sumForProperty(@Param("propertyId") UUID propertyId,
                               @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(e.amount), 0) from Expense e
            where e.property.id = :propertyId
              and e.chargeToOwner = true
              and (:from is null or e.expenseDate >= :from)
              and (:to is null or e.expenseDate <= :to)
            """)
    BigDecimal sumChargeableToOwnerForProperty(@Param("propertyId") UUID propertyId,
                                                @Param("from") LocalDate from, @Param("to") LocalDate to);
}
