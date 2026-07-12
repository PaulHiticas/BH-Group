package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.MaintenanceStatus;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketResponse;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketStatusUpdateRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Maintenance technician-facing endpoints, scoped to the authenticated
 * technician's own assigned tickets only.
 */
@RestController
@RequestMapping("/api/v1/maintenance/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MAINTENANCE')")
@Tag(name = "Maintenance Portal", description = "Technician access to their own assigned maintenance tickets")
public class MaintenanceController {

    private final MaintenanceTicketService maintenanceTicketService;

    @GetMapping
    @Operation(summary = "List my assigned maintenance tickets")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceTicketResponse>>> list(
            @RequestParam(required = false) MaintenanceStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                maintenanceTicketService.listMine(currentUserId(), status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my assigned maintenance tickets")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(maintenanceTicketService.getMine(currentUserId(), id)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update the status of one of my assigned tickets")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody MaintenanceTicketStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                maintenanceTicketService.updateMyStatus(currentUserId(), id, request)));
    }

    @PostMapping(value = "/{id}/photos", consumes = "multipart/form-data")
    @Operation(summary = "Upload a photo to one of my assigned tickets")
    public ResponseEntity<ApiResponse<MaintenanceTicketResponse>> addPhoto(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                maintenanceTicketService.addMyPhoto(currentUserId(), id, file, caption)));
    }

    private UUID currentUserId() {
        return SecurityUtils.requireCurrentUserId();
    }
}
