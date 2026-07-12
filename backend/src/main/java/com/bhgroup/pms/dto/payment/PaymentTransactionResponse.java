package com.bhgroup.pms.dto.payment;

import com.bhgroup.pms.domain.PaymentTransactionStatus;
import com.bhgroup.pms.domain.PaymentTransactionType;
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
