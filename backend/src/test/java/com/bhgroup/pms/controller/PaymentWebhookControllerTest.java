package com.bhgroup.pms.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.bhgroup.pms.domain.PaymentProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

/**
 * The webhook endpoint is disabled - no provider is wired in with real
 * signature verification, so it must reject every request outright rather
 * than parse or persist an unsigned payload. See the class javadoc on
 * {@link PaymentWebhookController} for why, and what re-enabling it safely
 * requires.
 */
class PaymentWebhookControllerTest {

    private final PaymentWebhookController controller = new PaymentWebhookController();

    @ParameterizedTest
    @EnumSource(PaymentProvider.class)
    void receive_alwaysRejectsWithNotImplemented_regardlessOfProvider(PaymentProvider provider) {
        var response = controller.receive(provider);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void controller_hasNoDependencies() {
        // If this ever needs a PaymentService/ObjectMapper again to compile,
        // the endpoint is being wired back up to process payloads - that
        // must not happen without adding signature verification first.
        assertThat(PaymentWebhookController.class.getDeclaredFields()).isEmpty();
    }
}
