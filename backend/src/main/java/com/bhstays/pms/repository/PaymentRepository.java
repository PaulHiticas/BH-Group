package com.bhstays.pms.repository;

import com.bhstays.pms.domain.Payment;
import com.bhstays.pms.domain.PaymentStatus;
import com.bhstays.pms.repository.projection.ReservationPaymentTotals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    String RESERVATION_PAYMENT_TOTALS_SELECT = """
            select new com.bhstays.pms.repository.projection.ReservationPaymentTotals(
                r.property.id, r.id, p.currency, r.currency, r.totalAmount, r.accommodationAmount,
                sum(p.amount), sum(p.refundedAmount))
            from Payment p join p.reservation r
            where p.status in :statuses
              and r.checkInDate >= :from
              and r.checkInDate <= :to
            """;

    String RESERVATION_PAYMENT_TOTALS_GROUP_BY = """
            group by r.property.id, r.id, p.currency, r.currency, r.totalAmount, r.accommodationAmount
            """;

    List<Payment> findByReservationIdOrderByCreatedAtDesc(UUID reservationId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    /** Row-locked: a webhook capturing or expiring a session serialises with any other one for it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.checkoutSessionId = :sessionId")
    Optional<Payment> findByCheckoutSessionIdForUpdate(@Param("sessionId") String checkoutSessionId);

    @Query("""
            select coalesce(sum(p.amount - p.refundedAmount), 0) from Payment p
            where p.reservation.id = :reservationId
              and p.status in :statuses
            """)
    BigDecimal sumNetPaidForReservation(@Param("reservationId") UUID reservationId,
                                         @Param("statuses") Collection<PaymentStatus> statuses);


    /**
     * Captured amount and successful refunds per reservation and payment
     * currency, for reservations checking in within [from, to] (both
     * required - an open period is passed as explicit bounds), across all
     * properties - one query for the whole portfolio. Amounts come from the
     * payment rows themselves (not from webhook events), so a duplicate
     * webhook delivery can never count a capture or a refund twice.
     */
    @Query(RESERVATION_PAYMENT_TOTALS_SELECT + RESERVATION_PAYMENT_TOTALS_GROUP_BY)
    List<ReservationPaymentTotals> sumCapturedByReservation(@Param("statuses") Collection<PaymentStatus> statuses,
                                                             @Param("from") LocalDate from,
                                                             @Param("to") LocalDate to);

    /** {@link #sumCapturedByReservation} restricted to the given properties (must not be empty). */
    @Query(RESERVATION_PAYMENT_TOTALS_SELECT + " and r.property.id in :propertyIds "
            + RESERVATION_PAYMENT_TOTALS_GROUP_BY)
    List<ReservationPaymentTotals> sumCapturedByReservationForProperties(
            @Param("propertyIds") Collection<UUID> propertyIds,
            @Param("statuses") Collection<PaymentStatus> statuses,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
