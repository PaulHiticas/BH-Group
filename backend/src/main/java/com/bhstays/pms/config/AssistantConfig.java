package com.bhstays.pms.config;

import java.net.http.HttpClient;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class AssistantConfig {

    private final AppProperties appProperties;

    @Bean
    public RestClient anthropicRestClient() {
        AppProperties.Assistant assistant = appProperties.getAssistant();
        return anthropicClient(Duration.ofMillis(assistant.getTimeoutMs()));
    }

    /**
     * Same Anthropic endpoint and credentials as the assistant's client, but
     * its own timeout: a pricing analysis is one large request that needs
     * longer than a chat turn, and sharing a client would force both to the
     * same ceiling. Injected by bean name, so the assistant keeps its own.
     */
    @Bean
    public RestClient pricingAiRestClient() {
        return anthropicClient(Duration.ofMillis(appProperties.getPricingAi().getTimeoutMs()));
    }

    private RestClient anthropicClient(Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
                // The API key stays on the assistant config: one Anthropic
                // account, one key, whichever feature is calling.
                .baseUrl(appProperties.getAssistant().getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }
}
