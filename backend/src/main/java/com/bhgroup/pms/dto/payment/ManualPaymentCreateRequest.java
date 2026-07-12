package com.bhgroup.pms.dto.payment;

import com.bhgroup.pms.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ManualPaymentCreateRequest(

        @NotNull(message = "Reservation is required")
        UUID reservationId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Method is required")
        PaymentMethod method,

        String notes
) {
}
