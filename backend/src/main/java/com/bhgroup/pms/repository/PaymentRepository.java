package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.Payment;
import com.bhgroup.pms.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByReservationIdOrderByCreatedAtDesc(UUID reservationId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    @Query("""
            select coalesce(sum(p.amount - p.refundedAmount), 0) from Payment p
            where p.reservation.id = :reservationId
              and p.status in :statuses
            """)
    BigDecimal sumNetPaidForReservation(@Param("reservationId") UUID reservationId,
                                         @Param("statuses") Collection<PaymentStatus> statuses);
}
