package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.config.AppProperties;
import com.bhgroup.pms.dto.assistant.AssistantMessageRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AppProperties appProperties;
    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getAssistant().setApiKey("test-key");
        appProperties.getAssistant().setModel("claude-haiku-4-5-20251001");
        appProperties.getAssistant().setBaseUrl("https://api.anthropic.com");
        appProperties.getAssistant().setMaxTokens(400);
        appProperties.getAssistant().setMaxHistoryMessages(16);
        appProperties.getAssistant().setTimeoutMs(15000);

        assistantService = new AssistantService(restClient, appProperties);
    }

    private void mockRestClientChain() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(AssistantService.AnthropicRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void chat_returnsTheAssistantsAnswerOnSuccess() {
        mockRestClientChain();
        var mockResponse = new AssistantService.AnthropicResponse(
                List.of(new AssistantService.AnthropicContentBlock("text", "Check-in-ul variază pe proprietate.")));
        when(responseSpec.body(AssistantService.AnthropicResponse.class)).thenReturn(mockResponse);

        String reply = assistantService.chat(List.of(new AssistantMessageRequest("user", "Care e ora de check-in?")));

        assertThat(reply).isEqualTo("Check-in-ul variază pe proprietate.");
    }

    @Test
    void chat_returnsContactFallbackWhenTheApiCallThrows() {
        mockRestClientChain();
        when(responseSpec.body(AssistantService.AnthropicResponse.class))
                .thenThrow(new RuntimeException("boom"));

        String reply = assistantService.chat(List.of(new AssistantMessageRequest("user", "Salut")));

        assertThat(reply).contains("Nu pot prelua acum răspunsul");
    }

    @Test
    void chat_returnsContactFallbackWhenTheApiKeyIsNotConfigured_noApiCallAttempted() {
        appProperties.getAssistant().setApiKey("");

        String reply = assistantService.chat(List.of(new AssistantMessageRequest("user", "Salut")));

        assertThat(reply).contains("Nu pot prelua acum răspunsul");
    }

    @Test
    void chat_trimsHistoryToTheConfiguredMaxBeforeCallingTheApi() {
        appProperties.getAssistant().setMaxHistoryMessages(2);
        mockRestClientChain();
        var mockResponse = new AssistantService.AnthropicResponse(
                List.of(new AssistantService.AnthropicContentBlock("text", "ok")));
        when(responseSpec.body(AssistantService.AnthropicResponse.class)).thenReturn(mockResponse);

        List<AssistantMessageRequest> history = List.of(
                new AssistantMessageRequest("user", "one"),
                new AssistantMessageRequest("assistant", "two"),
                new AssistantMessageRequest("user", "three")
        );

        assistantService.chat(history);

        ArgumentCaptor<AssistantService.AnthropicRequest> captor =
                ArgumentCaptor.forClass(AssistantService.AnthropicRequest.class);
        verify(requestBodySpec).body(captor.capture());
        assertThat(captor.getValue().messages()).hasSize(2);
        assertThat(captor.getValue().messages().get(0).content()).isEqualTo("two");
        assertThat(captor.getValue().messages().get(1).content()).isEqualTo("three");
    }

    @Test
    void chat_fallsBackToTheGenericContactPointerWhenNoContactInfoIsConfigured() {
        appProperties.getAssistant().setApiKey("");
        appProperties.getContact().setEmail(null);
        appProperties.getContact().setPhone(null);

        String reply = assistantService.chat(List.of(new AssistantMessageRequest("user", "Salut")));

        assertThat(reply).contains("formularul de contact");
    }
}
