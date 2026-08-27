package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.AssistantChatStatus;
import com.bhgroup.pms.dto.assistant.AssistantChatDetailResponse;
import com.bhgroup.pms.dto.assistant.AssistantChatMessageResponse;
import com.bhgroup.pms.dto.assistant.AssistantChatReplyRequest;
import com.bhgroup.pms.dto.assistant.AssistantChatSummaryResponse;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.service.AssistantChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Staff inbox for AI-assistant human handoff chats. */
@RestController
@RequestMapping("/api/v1/assistant-chats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
@Tag(name = "Assistant Chats (staff)", description = "Staff inbox for AI-assistant human handoff chats")
public class AdminAssistantChatController {

    private final AssistantChatService assistantChatService;

    @GetMapping
    @Operation(summary = "List handoff chats, optionally filtered by status, most recently active first")
    public ResponseEntity<ApiResponse<PageResponse<AssistantChatSummaryResponse>>> list(
            @RequestParam(required = false) AssistantChatStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(assistantChatService.listForStaff(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a handoff chat with its full message history")
    public ResponseEntity<ApiResponse<AssistantChatDetailResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(assistantChatService.getForStaff(id)));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Reply to a visitor in a handoff chat")
    public ResponseEntity<ApiResponse<AssistantChatMessageResponse>> addMessage(
            @PathVariable UUID id, @Valid @RequestBody AssistantChatReplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                assistantChatService.addStaffMessage(id, SecurityUtils.requireCurrentUserId(), request)));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Mark a handoff chat as resolved")
    public ResponseEntity<ApiResponse<Void>> resolve(@PathVariable UUID id) {
        assistantChatService.resolve(id);
        return ResponseEntity.ok(ApiResponse.message("Conversația a fost marcată ca rezolvată"));
    }
}
