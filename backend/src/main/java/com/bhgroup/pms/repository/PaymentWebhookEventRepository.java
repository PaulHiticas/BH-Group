package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.PaymentProvider;
import com.bhgroup.pms.domain.PaymentWebhookEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {

    Optional<PaymentWebhookEvent> findByProviderAndExternalEventId(PaymentProvider provider, String externalEventId);
}
