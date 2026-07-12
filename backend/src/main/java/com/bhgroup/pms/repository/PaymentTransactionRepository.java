package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.PaymentTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
}
