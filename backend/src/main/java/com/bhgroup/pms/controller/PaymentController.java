package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.dto.payment.ManualPaymentCreateRequest;
import com.bhgroup.pms.dto.payment.PaymentResponse;
import com.bhgroup.pms.dto.payment.RefundCreateRequest;
import com.bhgroup.pms.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','ACCOUNTANT')")
@Tag(name = "Payments", description = "Payments collected against reservations and their refunds")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @Operation(summary = "List payments for a reservation")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> list(@RequestParam UUID reservationId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.listForReservation(reservationId)));
    }

    @PostMapping
    @Operation(summary = "Record a payment collected offline (cash, bank transfer, card terminal)")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordManualPayment(
            @Valid @RequestBody ManualPaymentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(paymentService.recordManualPayment(request), "Payment recorded"));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a payment, fully or partially")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable UUID id, @Valid @RequestBody RefundCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.refund(id, request), "Refund processed"));
    }
}
