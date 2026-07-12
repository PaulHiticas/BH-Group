package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.MaintenanceCategory;
import com.bhgroup.pms.domain.MaintenancePriority;
import com.bhgroup.pms.domain.MaintenanceStatus;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketAssignRequest;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketCreateRequest;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketResponse;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketStatusUpdateRequest;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketUpdateRequest;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.service.MaintenanceTicketService;
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
@RequestMapping("/api/v1/maintenance-tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
@Tag(name = "Maintenance Tickets", description = "Admin management of maintenance tickets")
public class MaintenanceTicketController {

    private final MaintenanceTicketService maintenanceTicketService;

    @GetMapping
    @Operation(summary = "List maintenance tickets")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceTicketResponse>>> list(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) MaintenancePriority priority,
            @RequestParam(required = false) MaintenanceCategory category,
            @RequestParam(required = false) UUID assigneeId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                maintenanceTicketService.list(propertyId, status, priority, category, assigneeId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a maintenance ticket by id")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(maintenanceTicketService.get(id)));
    }

    @PostMapping
    @Operation(summary = "Report a maintenance issue")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> create(
            @Valid @RequestBody MaintenanceTicketCreateRequest request) {
        UUID reporterId = SecurityUtils.getCurrentPrincipal().map(p -> p.getId()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(maintenanceTicketService.create(request, reporterId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a maintenance ticket")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody MaintenanceTicketUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(maintenanceTicketService.update(id, request)));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign a maintenance technician")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> assign(
            @PathVariable UUID id, @Valid @RequestBody MaintenanceTicketAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(maintenanceTicketService.assign(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a maintenance ticket's status")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody MaintenanceTicketStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(maintenanceTicketService.updateStatus(id, request)));
    }

    @PostMapping(value = "/{id}/photos", consumes = "multipart/form-data")
    @Operation(summary = "Upload a maintenance ticket photo")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> addPhoto(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(maintenanceTicketService.addPhoto(id, file, caption)));
    }
}
