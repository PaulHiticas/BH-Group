package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.OwnerThreadStatus;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadDetailResponse;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadMessageCreateRequest;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadMessageResponse;
import com.bhgroup.pms.dto.ownerthread.OwnerThreadSummaryResponse;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.service.OwnerThreadService;
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

/** Staff inbox for owner contact threads across every owner/property. */
@RestController
@RequestMapping("/api/v1/owner-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
@Tag(name = "Owner Requests (staff)", description = "Staff inbox for owner contact threads")
public class AdminOwnerRequestController {

    private final OwnerThreadService ownerThreadService;

    @GetMapping
    @Operation(summary = "List owner contact threads, optionally filtered by status, most recently active first")
    public ResponseEntity<ApiResponse<PageResponse<OwnerThreadSummaryResponse>>> list(
            @RequestParam(required = false) OwnerThreadStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ownerThreadService.listForStaff(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an owner contact thread with its full message history")
    public ResponseEntity<ApiResponse<OwnerThreadDetailResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(ownerThreadService.getForStaff(id)));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Reply to an owner in one of their contact threads")
    public ResponseEntity<ApiResponse<OwnerThreadMessageResponse>> addMessage(
            @PathVariable UUID id, @Valid @RequestBody OwnerThreadMessageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                ownerThreadService.addStaffMessage(id, SecurityUtils.requireCurrentUserId(), request)));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Mark an owner contact thread as resolved")
    public ResponseEntity<ApiResponse<Void>> resolve(@PathVariable UUID id) {
        ownerThreadService.resolve(id);
        return ResponseEntity.ok(ApiResponse.message("Cererea a fost marcată ca rezolvată"));
    }
}
