package com.bhstays.pms.dto.payment;

import com.bhstays.pms.domain.PaymentTransactionStatus;
import com.bhstays.pms.domain.PaymentTransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransactionResponse(
        UUID id,
        PaymentTransactionType type,
        PaymentTransactionStatus status,
        BigDecimal amount,
        String providerTransactionId,
        String failureReason,
        Instant createdAt
) {
}
