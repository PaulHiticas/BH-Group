package com.bhstays.pms.payment;

import com.bhstays.pms.domain.PaymentStatus;

/**
 * Outcome of a gateway operation (charge or refund). For synchronous
 * providers (manual, most card-terminal flows) the final status is known
 * immediately. For asynchronous ones (Stripe) a charge comes back
 * PROCESSING and the real outcome arrives later over the provider's
 * signature-verified webhook - see StripeWebhookService.
 */
public record PaymentGatewayResult(
        PaymentStatus status,
        String providerReference,
        String failureReason
) {
    public static PaymentGatewayResult succeeded(String providerReference) {
        return new PaymentGatewayResult(PaymentStatus.SUCCEEDED, providerReference, null);
    }

    public static PaymentGatewayResult failed(String providerReference, String reason) {
        return new PaymentGatewayResult(PaymentStatus.FAILED, providerReference, reason);
    }
}
