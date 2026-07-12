package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.domain.PaymentProvider;
import com.bhgroup.pms.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entry point for payment gateway webhooks (Stripe, Netopia, ...). Public by
 * necessity — the gateway calls it directly, with no user session — so
 * every event is recorded idempotently by (provider, externalEventId)
 * before any processing, and the actual payload shape is fully delegated to
 * that provider's {@link com.bhgroup.pms.payment.PaymentGateway}
 * implementation. No provider is wired in yet (MANUAL doesn't receive
 * webhooks), so this currently has nothing to dispatch to; it exists so a
 * future Stripe/Netopia adapter is a drop-in with no booking-logic changes.
 *
 * TODO once a provider is chosen: verify the gateway's signature header
 * here before any parsing, and adapt id/type extraction to that provider's
 * actual envelope shape (Stripe: top-level "id"/"type"; Netopia differs).
 */
@RestController
@RequestMapping("/api/v1/public/payments/webhook")
@RequiredArgsConstructor
@Tag(name = "Payment Webhooks", description = "Inbound payment gateway webhook events")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{provider}")
    @Operation(summary = "Receive a webhook event from a payment gateway")
    public ResponseEntity<ApiResponse<Void>> receive(@PathVariable PaymentProvider provider,
                                                       @RequestBody String rawPayload) {
        JsonNode node;
        try {
            node = objectMapper.readTree(rawPayload);
        } catch (Exception ex) {
            throw new BadRequestException("Malformed webhook payload");
        }

        String externalEventId = node.path("id").asText(null);
        String eventType = node.path("type").asText(null);
        if (externalEventId == null || eventType == null) {
            throw new BadRequestException("Webhook payload is missing id/type");
        }

        paymentService.handleWebhookEvent(provider, externalEventId, eventType, rawPayload);
        return ResponseEntity.ok(ApiResponse.message("Webhook received"));
    }
}
