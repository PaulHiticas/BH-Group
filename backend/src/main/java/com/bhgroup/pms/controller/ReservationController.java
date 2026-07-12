package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.csv.CsvWriter;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.dto.reservation.AccessCodeUpdateRequest;
import com.bhgroup.pms.dto.reservation.AvailabilityResponse;
import com.bhgroup.pms.dto.reservation.CalendarEntryResponse;
import com.bhgroup.pms.dto.reservation.CancellationQuoteResponse;
import com.bhgroup.pms.dto.reservation.ReservationCreateRequest;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
import com.bhgroup.pms.dto.reservation.ReservationStatusUpdateRequest;
import com.bhgroup.pms.dto.reservation.ReservationUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.dto.reservation.AvailabilityResponse;
import com.bhgroup.pms.dto.reservation.CalendarEntryResponse;
import com.bhgroup.pms.dto.reservation.ReservationCreateRequest;
import com.bhgroup.pms.dto.reservation.ReservationResponse;
import com.bhgroup.pms.dto.reservation.ReservationStatusUpdateRequest;
import com.bhgroup.pms.dto.reservation.ReservationUpdateRequest;
import com.bhgroup.pms.service.ReservationService;
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reservation management, calendar and availability")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','ACCOUNTANT','SUPPORT_AGENT')")
    @Operation(summary = "List reservations with search and filters")
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> list(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.list(propertyId, status, search, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','ACCOUNTANT','SUPPORT_AGENT')")
    @Operation(summary = "Get a reservation by id")
    public ResponseEntity<ApiResponse<ReservationResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.get(id)));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Export reservations matching the given filters as CSV")
    public void export(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws IOException {
        CsvWriter.write(response, "rezervari.csv",
                List.of("Oaspete", "Email", "Telefon", "Proprietate", "Check-in", "Check-out",
                        "Nr. oaspeți", "Status", "Sursă", "Sumă totală", "Monedă", "Note"),
                reservationService.exportRows(propertyId, status, search, from, to));
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
    @Operation(summary = "Get calendar entries for a property in a date range")
    public ResponseEntity<ApiResponse<List<CalendarEntryResponse>>> calendar(
            @RequestParam UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.calendar(propertyId, from, to)));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
    @Operation(summary = "Check property availability for a date range")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> availability(
            @RequestParam UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.availability(propertyId, checkIn, checkOut)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
    @Operation(summary = "Create a reservation")
    public ResponseEntity<ApiResponse<ReservationResponse>> create(
            @Valid @RequestBody ReservationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reservationService.create(request), "Reservation created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
    @Operation(summary = "Update a reservation")
    public ResponseEntity<ApiResponse<ReservationResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ReservationUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(reservationService.update(id, request), "Reservation updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
    @Operation(summary = "Transition a reservation's status")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody ReservationStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.updateStatus(id, request), "Status updated"));
    }

    @PatchMapping("/{id}/access-code")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
    @Operation(summary = "Set or clear the guest access code (smart lock PIN / lockbox code)")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateAccessCode(
            @PathVariable UUID id, @Valid @RequestBody AccessCodeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.updateAccessCode(id, request), "Access code updated"));
    }

    @GetMapping("/{id}/cancellation-quote")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
    @Operation(summary = "Preview the refund a cancellation would trigger right now, per the property's cancellation policy")
    public ResponseEntity<ApiResponse<CancellationQuoteResponse>> cancellationQuote(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.cancellationQuote(id)));
    }

    @PostMapping("/{id}/send-checkin-instructions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
    @Operation(summary = "Email check-in instructions (address, time, access code) to the guest now")
    public ResponseEntity<ApiResponse<ReservationResponse>> sendCheckinInstructions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                reservationService.sendCheckinInstructionsNow(id), "Check-in instructions sent"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
    @Operation(summary = "Delete a reservation")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        reservationService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Reservation deleted successfully"));
    }
}
