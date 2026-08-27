package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.dto.assistant.AssistantChatMessageResponse;
import com.bhgroup.pms.dto.assistant.AssistantChatRequest;
import com.bhgroup.pms.dto.assistant.AssistantChatResponse;
import com.bhgroup.pms.dto.assistant.AssistantHandoffRequest;
import com.bhgroup.pms.dto.assistant.AssistantHandoffResponse;
import com.bhgroup.pms.service.AssistantChatService;
import com.bhgroup.pms.service.AssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (unauthenticated) FAQ chat + human-handoff endpoints - see
 * SecurityConfig's PUBLIC_ENDPOINTS and RateLimitingFilter's rules for
 * these paths. The staff-facing side of a handoff chat lives in
 * AdminAssistantChatController (/api/v1/assistant-chats, authenticated).
 */
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Public FAQ chat assistant + human handoff")
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantChatService assistantChatService;

    @PostMapping("/chat")
    @Operation(summary = "Chat with the FAQ assistant")
    public ResponseEntity<ApiResponse<AssistantChatResponse>> chat(@Valid @RequestBody AssistantChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assistantService.chat(request.messages())));
    }

    @PostMapping("/handoff")
    @Operation(summary = "Escalate the current conversation to a human colleague")
    public ResponseEntity<ApiResponse<AssistantHandoffResponse>> handoff(
            @Valid @RequestBody AssistantHandoffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(assistantChatService.createHandoff(request)));
    }

    @GetMapping("/chat/{publicToken}/messages")
    @Operation(summary = "Poll a visitor's handoff chat for staff replies")
    public ResponseEntity<ApiResponse<List<AssistantChatMessageResponse>>> getHandoffMessages(
            @PathVariable String publicToken) {
        return ResponseEntity.ok(ApiResponse.success(assistantChatService.getMessagesByToken(publicToken)));
    }
}
