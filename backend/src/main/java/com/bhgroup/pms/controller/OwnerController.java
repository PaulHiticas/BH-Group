package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.dto.expense.ExpenseResponse;
import com.bhgroup.pms.dto.maintenance.MaintenanceTicketResponse;
import com.bhgroup.pms.dto.owner.OwnerDashboardSummaryResponse;
import com.bhgroup.pms.dto.owner.OwnerPropertyResponse;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.service.OwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owner-facing endpoints. Every method scopes data to the authenticated
 * owner's own properties/reservations — an owner can never read another
 * owner's data, including by guessing/enumerating ids.
 */
@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Owner Portal", description = "Read-only access for property owners to their own properties, reservations and revenue")
public class OwnerController {

    private final OwnerService ownerService;

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Revenue/commission summary and upcoming reservations for the current owner")
    public ResponseEntity<ApiResponse<OwnerDashboardSummaryResponse>> dashboardSummary() {
        return ResponseEntity.ok(ApiResponse.success(ownerService.getMyDashboardSummary(currentOwnerId())));
    }

    @GetMapping("/properties")
    @Operation(summary = "List the current owner's properties")
    public ResponseEntity<ApiResponse<PageResponse<OwnerPropertyResponse>>> listProperties(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ownerService.listMyProperties(currentOwnerId(), pageable)));
    }

    @GetMapping("/properties/{id}")
    @Operation(summary = "Get one of the current owner's properties by id")
    public ResponseEntity<ApiResponse<OwnerPropertyResponse>> getProperty(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(ownerService.getMyProperty(currentOwnerId(), id)));
    }

    @GetMapping("/reservations")
    @Operation(summary = "List reservations for the current owner's properties")
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> listReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerService.listMyReservations(currentOwnerId(), status, from, to, pageable)));
    }

    @GetMapping("/maintenance-tickets")
    @Operation(summary = "List maintenance tickets for the current owner's properties")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceTicketResponse>>> listMaintenanceTickets(
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerService.listMyMaintenanceTickets(currentOwnerId(), pageable)));
    }

    @GetMapping("/expenses")
    @Operation(summary = "List expenses charged to the current owner for their properties")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> listExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                ownerService.listMyExpenses(currentOwnerId(), from, to, pageable)));
    }

    private UUID currentOwnerId() {
        return SecurityUtils.requireCurrentUserId();
    }
}
