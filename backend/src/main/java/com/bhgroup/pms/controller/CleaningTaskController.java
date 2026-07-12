package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.CleaningTaskStatus;
import com.bhgroup.pms.dto.cleaning.ChecklistItemUpdateRequest;
import com.bhgroup.pms.dto.cleaning.CleaningTaskAssignRequest;
import com.bhgroup.pms.dto.cleaning.CleaningTaskCreateRequest;
import com.bhgroup.pms.dto.cleaning.CleaningTaskResponse;
import com.bhgroup.pms.dto.cleaning.CleaningTaskStatusUpdateRequest;
import com.bhgroup.pms.dto.cleaning.CleaningTaskUpdateRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/cleaning-tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
@Tag(name = "Cleaning Tasks", description = "Admin management of cleaning tasks")
public class CleaningTaskController {

    private final CleaningTaskService cleaningTaskService;

    @GetMapping
    @Operation(summary = "List cleaning tasks")
    public ResponseEntity<ApiResponse<PageResponse<CleaningTaskResponse>>> list(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) CleaningTaskStatus status,
            @RequestParam(required = false) UUID cleanerId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(cleaningTaskService.list(propertyId, status, cleanerId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a cleaning task by id")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(cleaningTaskService.get(id)));
    }

    @PostMapping
    @Operation(summary = "Manually create a cleaning task")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> create(
            @Valid @RequestBody CleaningTaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(cleaningTaskService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a cleaning task's schedule, notes, cost")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CleaningTaskUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cleaningTaskService.update(id, request)));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign or reassign a cleaner")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> assign(
            @PathVariable UUID id, @Valid @RequestBody CleaningTaskAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cleaningTaskService.assign(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a cleaning task's status")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody CleaningTaskStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cleaningTaskService.updateStatus(id, request)));
    }

    @PatchMapping("/{id}/checklist")
    @Operation(summary = "Check/uncheck a checklist item")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> updateChecklist(
            @PathVariable UUID id, @Valid @RequestBody ChecklistItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cleaningTaskService.updateChecklistItem(id, request)));
    }

    @PostMapping(value = "/{id}/photos", consumes = "multipart/form-data")
    @Operation(summary = "Upload a cleaning task photo")
    public ResponseEntity<ApiResponse<CleaningTaskResponse>> addPhoto(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cleaningTaskService.addPhoto(id, file, caption)));
    }
}
