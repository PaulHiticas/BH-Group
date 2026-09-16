package com.bhstays.pms.controller;

import com.bhstays.pms.common.response.ApiResponse;
import com.bhstays.pms.common.response.PageResponse;
import com.bhstays.pms.dto.ownerthread.OwnerThreadCreateRequest;
import com.bhstays.pms.dto.ownerthread.OwnerThreadDetailResponse;
import com.bhstays.pms.dto.ownerthread.OwnerThreadMessageCreateRequest;
import com.bhstays.pms.dto.ownerthread.OwnerThreadMessageResponse;
import com.bhstays.pms.dto.ownerthread.OwnerThreadSummaryResponse;
import com.bhstays.pms.security.SecurityUtils;
import com.bhstays.pms.service.OwnerThreadService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owner-facing contact threads to BH Stays staff — an owner never sees or
 * writes to another owner's thread (a foreign thread id resolves as 404,
 * same isolation rule as {@link OwnerController}).
 */
@RestController
@RequestMapping("/api/v1/owner/threads")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Owner Contact Threads", description = "Owner requests/questions to BH Stays staff, optionally about a specific property")
public class OwnerThreadController {

    private final OwnerThreadService ownerThreadService;

    @GetMapping
    @Operation(summary = "List the current owner's contact threads, most recently active first")
    public ResponseEntity<ApiResponse<PageResponse<OwnerThreadSummaryResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ownerThreadService.listForOwner(currentOwnerId(), pageable)));
    }

    @PostMapping
    @Operation(summary = "Start a new contact thread, optionally about one of the owner's own properties")
    public ResponseEntity<ApiResponse<OwnerThreadDetailResponse>> create(
            @Valid @RequestBody OwnerThreadCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                ownerThreadService.createForOwner(currentOwnerId(), request), "Cererea a fost trimisă"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the current owner's threads with its full message history")
    public ResponseEntity<ApiResponse<OwnerThreadDetailResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(ownerThreadService.getForOwner(currentOwnerId(), id)));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Reply in one of the current owner's threads")
    public ResponseEntity<ApiResponse<OwnerThreadMessageResponse>> addMessage(
            @PathVariable UUID id, @Valid @RequestBody OwnerThreadMessageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                ownerThreadService.addOwnerMessage(currentOwnerId(), id, request)));
    }

    private UUID currentOwnerId() {
        return SecurityUtils.requireCurrentUserId();
    }
}
