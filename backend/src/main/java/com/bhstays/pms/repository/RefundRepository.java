package com.bhstays.pms.repository;

import com.bhstays.pms.domain.Refund;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
}
