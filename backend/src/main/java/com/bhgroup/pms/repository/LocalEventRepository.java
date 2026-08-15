package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.LocalEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocalEventRepository extends JpaRepository<LocalEvent, UUID> {

    List<LocalEvent> findByPropertyIdOrderByStartDateAsc(UUID propertyId);

    @Query("select e from LocalEvent e where e.city is not null and lower(trim(e.city)) = :normalizedCity order by e.startDate asc")
    List<LocalEvent> findByNormalizedCity(@Param("normalizedCity") String normalizedCity);

    /**
     * Every event that could affect this property's price over
     * [from, toInclusive]: either a property-specific override, or a
     * city-wide event whose city matches the property's own city
     * case-insensitively and trimmed. Fetched once per quote/breakdown
     * call and matched per-night in memory - never one query per night.
     */
    @Query("""
            select e from LocalEvent e
            where (e.property.id = :propertyId
                   or (e.city is not null and lower(trim(e.city)) = :normalizedCity))
              and e.startDate <= :toInclusive
              and e.endDate >= :from
            """)
    List<LocalEvent> findRelevantForPricing(@Param("propertyId") UUID propertyId,
                                             @Param("normalizedCity") String normalizedCity,
                                             @Param("from") LocalDate from,
                                             @Param("toInclusive") LocalDate toInclusive);
}
