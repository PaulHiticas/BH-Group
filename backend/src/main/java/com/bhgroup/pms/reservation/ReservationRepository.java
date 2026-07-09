package com.bhgroup.pms.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID>,
        JpaSpecificationExecutor<Reservation> {

    @Query("""
            select r from Reservation r
            where r.property.id = :propertyId
              and r.status not in :excludedStatuses
              and r.checkInDate < :checkOutDate
              and r.checkOutDate > :checkInDate
              and (:excludeId is null or r.id <> :excludeId)
            """)
    List<Reservation> findOverlapping(@Param("propertyId") UUID propertyId,
                                       @Param("checkInDate") LocalDate checkInDate,
                                       @Param("checkOutDate") LocalDate checkOutDate,
                                       @Param("excludeId") UUID excludeId,
                                       @Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses);

    @Query("""
            select r from Reservation r
            where r.property.id = :propertyId
              and r.status not in :excludedStatuses
              and r.checkInDate < :to
              and r.checkOutDate > :from
            order by r.checkInDate asc
            """)
    List<Reservation> findCalendarEntries(@Param("propertyId") UUID propertyId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to,
                                           @Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses);

    long countByPropertyIdAndStatus(UUID propertyId, ReservationStatus status);

    long countByStatus(ReservationStatus status);

    @Query("select coalesce(sum(r.totalAmount), 0) from Reservation r where r.status not in :excludedStatuses")
    BigDecimal sumTotalRevenue(@Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses);

    @Query("""
            select r from Reservation r
            where r.status not in :excludedStatuses
              and r.checkInDate >= :from
            order by r.checkInDate asc
            """)
    List<Reservation> findUpcoming(@Param("from") LocalDate from,
                                    @Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses,
                                    Pageable pageable);

    Optional<Reservation> findByManagementToken(String managementToken);
}
