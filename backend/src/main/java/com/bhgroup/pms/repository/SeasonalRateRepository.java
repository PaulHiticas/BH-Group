package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.SeasonalRate;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonalRateRepository extends JpaRepository<SeasonalRate, UUID> {

    List<SeasonalRate> findByPropertyIdOrderByStartDateAsc(UUID propertyId);

    @Query("""
            select s from SeasonalRate s
            where s.property.id = :propertyId
              and s.startDate <= :to
              and s.endDate >= :from
            """)
    List<SeasonalRate> findOverlappingRange(@Param("propertyId") UUID propertyId,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);
}
