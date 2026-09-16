package com.bhstays.pms.service.mapper;

import com.bhstays.pms.domain.Payment;
import com.bhstays.pms.domain.PaymentTransaction;
import com.bhstays.pms.domain.Refund;
import com.bhstays.pms.dto.payment.PaymentResponse;
import com.bhstays.pms.dto.payment.PaymentTransactionResponse;
import com.bhstays.pms.dto.payment.RefundResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment, List<PaymentTransaction> transactions, List<Refund> refunds) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservation().getId(),
                payment.getProvider(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getRefundedAmount(),
                payment.getProviderPaymentId(),
                payment.getNotes(),
                transactions.stream().map(this::toTransactionResponse).toList(),
                refunds.stream().map(this::toRefundResponse).toList(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }

    public PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getProviderTransactionId(),
                transaction.getFailureReason(),
                transaction.getCreatedAt());
    }

    public RefundResponse toRefundResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(), refund.getAmount(), refund.getReason(), refund.getStatus(), refund.getCreatedAt());
    }
}
