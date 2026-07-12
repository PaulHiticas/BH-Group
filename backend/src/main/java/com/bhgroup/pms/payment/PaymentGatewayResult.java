package com.bhgroup.pms.payment;

import com.bhgroup.pms.domain.PaymentStatus;

/**
 * Outcome of a gateway operation (charge, refund, or webhook event). For
 * synchronous providers (manual, most card-terminal flows) the final status
 * is known immediately. For asynchronous ones (Stripe, Netopia) a charge
 * typically comes back PROCESSING/PENDING and the real outcome arrives
 * later through {@link PaymentGateway#handleWebhookEvent}.
 */
public record PaymentGatewayResult(
        PaymentStatus status,
        String providerReference,
        String rawResponse,
        String failureReason
) {
    public static PaymentGatewayResult succeeded(String providerReference, String rawResponse) {
        return new PaymentGatewayResult(PaymentStatus.SUCCEEDED, providerReference, rawResponse, null);
    }

    public static PaymentGatewayResult failed(String providerReference, String rawResponse, String reason) {
        return new PaymentGatewayResult(PaymentStatus.FAILED, providerReference, rawResponse, reason);
    }
}
