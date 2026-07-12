package com.bhgroup.pms.payment;

import com.bhgroup.pms.domain.Payment;
import com.bhgroup.pms.domain.PaymentProvider;
import java.math.BigDecimal;

/**
 * A payment provider adapter. {@link ManualPaymentGateway} is the only
 * implementation today (staff logging cash/bank-transfer/terminal payments
 * collected offline). Adding real online payments later — Stripe, Netopia —
 * means writing one more class that implements this interface and wiring
 * its API keys/webhook secret; {@link com.bhgroup.pms.service.PaymentService}
 * and the booking flow never change.
 */
public interface PaymentGateway {

    PaymentProvider getProvider();

    /**
     * Attempts to charge for the given payment. For synchronous providers
     * (manual) the returned status is final. For asynchronous providers the
     * real outcome arrives later via {@link #handleWebhookEvent}.
     */
    PaymentGatewayResult charge(Payment payment);

    /**
     * Attempts to refund {@code amount} of the given payment.
     */
    PaymentGatewayResult refund(Payment payment, BigDecimal amount, String reason);

    /**
     * Interprets a raw webhook payload already verified/deserialized by the
     * caller. Returns a result whose {@code providerReference} matches a
     * {@code Payment.providerPaymentId} so the caller can update it, or a
     * result with a {@code null providerReference} for events that don't
     * change any payment's status (should be recorded but otherwise ignored).
     */
    PaymentGatewayResult handleWebhookEvent(String eventType, String payload);
}
