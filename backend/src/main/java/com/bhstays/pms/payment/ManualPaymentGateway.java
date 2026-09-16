package com.bhstays.pms.payment;

import com.bhstays.pms.domain.Payment;
import com.bhstays.pms.domain.PaymentProvider;
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
        return PaymentGatewayResult.succeeded(null);
    }

    @Override
    public PaymentGatewayResult refund(Payment payment, BigDecimal amount, String reason) {
        return PaymentGatewayResult.succeeded(null);
    }
}
