package com.bhgroup.pms.payment;

import com.bhgroup.pms.domain.Payment;
import com.bhgroup.pms.domain.PaymentProvider;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Staff record a payment (cash, bank transfer, card terminal) that was
 * already collected offline — there is no external call, no async
 * confirmation, and no webhook. Charging and refunding both succeed
 * synchronously the moment staff log them.
 */
@Component
public class ManualPaymentGateway implements PaymentGateway {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MANUAL;
    }

    @Override
    public PaymentGatewayResult charge(Payment payment) {
        return PaymentGatewayResult.succeeded(null, null);
    }

    @Override
    public PaymentGatewayResult refund(Payment payment, BigDecimal amount, String reason) {
        return PaymentGatewayResult.succeeded(null, null);
    }

    @Override
    public PaymentGatewayResult handleWebhookEvent(String eventType, String payload) {
        throw new UnsupportedOperationException("The manual payment provider does not receive webhooks");
    }
}
