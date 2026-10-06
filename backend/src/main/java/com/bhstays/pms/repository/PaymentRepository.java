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
              and (cast(:from as java.time.LocalDate) is null or r.checkInDate >= cast(:from as java.time.LocalDate))
              and (cast(:to as java.time.LocalDate) is null or r.checkInDate <= cast(:to as java.time.LocalDate))
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
     * Net captured revenue (amount minus successful refunds) for a
     * property's reservations checking in within [from, to], grouped by
     * currency so amounts in different currencies are never summed
     * together. Each row is {@code [String currency, BigDecimal netAmount]}.
     */
    @Query("""
            select p.currency, coalesce(sum(p.amount - p.refundedAmount), 0)
            from Payment p
            where p.reservation.property.id = :propertyId
              and p.status in :statuses
              and (cast(:from as java.time.LocalDate) is null or p.reservation.checkInDate >= cast(:from as java.time.LocalDate))
              and (cast(:to as java.time.LocalDate) is null or p.reservation.checkInDate <= cast(:to as java.time.LocalDate))
            group by p.currency
            """)
    List<Object[]> sumNetPaidByPropertyGroupedByCurrency(@Param("propertyId") UUID propertyId,
                                                           @Param("statuses") Collection<PaymentStatus> statuses,
                                                           @Param("from") LocalDate from,
                                                           @Param("to") LocalDate to);

    /**
     * Captured amount and successful refunds per reservation and payment
     * currency, for reservations checking in within [from, to], across all
     * properties - one query for the whole portfolio. Amounts come from the
     * payment rows themselves (not from webhook events), so a duplicate
     * webhook delivery can never count a capture or a refund twice.
     */
    @Query(RESERVATION_PAYMENT_TOTALS_SELECT + RESERVATION_PAYMENT_TOTALS_GROUP_BY)
    List<ReservationPaymentTotals> sumCapturedByReservation(@Param("statuses") Collection<PaymentStatus> statuses,
                                                             @Param("from") LocalDate from,
                                                             @Param("to") LocalDate to);

    /** {@link #sumCapturedByReservation} restricted to one property. */
    @Query(RESERVATION_PAYMENT_TOTALS_SELECT + " and r.property.id = :propertyId "
            + RESERVATION_PAYMENT_TOTALS_GROUP_BY)
    List<ReservationPaymentTotals> sumCapturedByReservationForProperty(@Param("propertyId") UUID propertyId,
                                                                        @Param("statuses") Collection<PaymentStatus> statuses,
                                                                        @Param("from") LocalDate from,
                                                                        @Param("to") LocalDate to);
}
