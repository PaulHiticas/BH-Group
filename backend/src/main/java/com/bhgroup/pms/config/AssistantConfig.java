package com.bhgroup.pms.config;

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
        Duration timeout = Duration.ofMillis(assistant.getTimeoutMs());

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
                .baseUrl(assistant.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }
}
