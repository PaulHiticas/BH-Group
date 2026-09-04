package com.bhgroup.pms.controller;

import com.bhgroup.pms.domain.PaymentProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disabled. This used to accept and persist any payload posted here with no
 * signature verification at all - a public, unauthenticated endpoint that
 * takes an unsigned "payment succeeded" event is a forgeable one, and no
 * provider is wired in yet to make that check meaningful (MANUAL doesn't
 * receive webhooks - see {@link com.bhgroup.pms.payment.ManualPaymentGateway}).
 * Every request is now rejected before any parsing or processing happens.
 *
 * This intentionally stays under {@code /api/v1/public/**}: a real gateway
 * calls its webhook with no session of ours to authenticate with, so the
 * fix here isn't to require our own auth - it's the signature check below.
 *
 * Re-enable only once a real provider (Stripe/Netopia) is integrated:
 * verify that provider's signature header first, then restore dispatch to
 * {@link com.bhgroup.pms.service.PaymentService#handleWebhookEvent}, which
 * is untouched and ready to receive it.
 */
@RestController
@RequestMapping("/api/v1/public/payments/webhook")
@Tag(name = "Payment Webhooks", description = "Disabled pending a real, signature-verified provider integration")
public class PaymentWebhookController {

    @PostMapping("/{provider}")
    @Operation(summary = "Disabled - no signature-verified payment provider is wired in yet")
    public ResponseEntity<Void> receive(@PathVariable PaymentProvider provider) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
