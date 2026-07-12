package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.CleaningTaskStatus;
import com.bhgroup.pms.dto.cleaning.ChecklistItemUpdateRequest;
import com.bhgroup.pms.dto.cleaning.CleaningTaskResponse;
import com.bhgroup.pms.dto.cleaning.CleaningTaskStatusUpdateRequest;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.service.CleaningTaskService;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * Cleaner-facing endpoints, scoped to the authenticated cleaner's own
 * assigned tasks only.
 */
@RestController
@RequestMapping("/api/v1/cleaner/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLEANER')")
@Tag(name = "Cleaner Portal", description = "Cleaner access to their own assigned cleaning tasks")
public class CleanerController {

    private final CleaningTaskService cleaningTaskService;

    @GetMapping
    @Operation(summary = "List my assigned cleaning tasks")
    public ResponseEntity<ApiResponse<PageResponse<CleaningTaskResponse>>> list(
            @RequestParam(required = false) CleaningTaskStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                cleaningTaskService.listMine(currentUserId(), status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my assigned cleaning tasks")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(cleaningTaskService.getMine(currentUserId(), id)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update the status of one of my assigned tasks")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody CleaningTaskStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                cleaningTaskService.updateMyStatus(currentUserId(), id, request)));
    }

    @PatchMapping("/{id}/checklist")
    @Operation(summary = "Check/uncheck a checklist item on one of my assigned tasks")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> updateChecklist(
            @PathVariable UUID id, @Valid @RequestBody ChecklistItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                cleaningTaskService.updateMyChecklistItem(currentUserId(), id, request)));
    }

    @PostMapping(value = "/{id}/photos", consumes = "multipart/form-data")
    @Operation(summary = "Upload a photo to one of my assigned tasks")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> addPhoto(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                cleaningTaskService.addMyPhoto(currentUserId(), id, file, caption)));
    }

    private UUID currentUserId() {
        return SecurityUtils.requireCurrentUserId();
    }
}
