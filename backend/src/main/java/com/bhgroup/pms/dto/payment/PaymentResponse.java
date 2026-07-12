package com.bhgroup.pms.dto.payment;

import com.bhgroup.pms.domain.PaymentMethod;
import com.bhgroup.pms.domain.PaymentProvider;
import com.bhgroup.pms.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID reservationId,
        PaymentProvider provider,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        BigDecimal refundedAmount,
        String providerPaymentId,
        String notes,
        List<PaymentTransactionResponse> transactions,
        List<RefundResponse> refunds,
        Instant createdAt,
        Instant updatedAt
) {
}
