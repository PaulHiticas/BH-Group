package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.dto.assistant.AssistantChatRequest;
import com.bhgroup.pms.dto.assistant.AssistantChatResponse;
import com.bhgroup.pms.service.AssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (unauthenticated) FAQ chat endpoint - see SecurityConfig's
 * PUBLIC_ENDPOINTS and RateLimitingFilter's rule for this path.
 */
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Public FAQ chat assistant")
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    @Operation(summary = "Chat with the FAQ assistant")
    public ResponseEntity<ApiResponse<AssistantChatResponse>> chat(@Valid @RequestBody AssistantChatRequest request) {
        String reply = assistantService.chat(request.messages());
        return ResponseEntity.ok(ApiResponse.success(new AssistantChatResponse(reply)));
    }
}
