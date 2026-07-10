package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.dto.publicapi.PublicBookingRequest;
import com.bhgroup.pms.dto.publicapi.PublicBookingUpdateRequest;
import com.bhgroup.pms.dto.publicapi.PublicReservationResponse;
import com.bhgroup.pms.dto.reservation.AvailabilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bhgroup.pms.dto.publicapi.PublicBookingRequest;
import com.bhgroup.pms.dto.publicapi.PublicBookingUpdateRequest;
import com.bhgroup.pms.dto.publicapi.PublicReservationResponse;
import com.bhgroup.pms.dto.reservation.AvailabilityResponse;
import com.bhgroup.pms.service.PublicReservationService;
@RestController
@RequestMapping("/api/v1/public/reservations")
@RequiredArgsConstructor
@Tag(name = "Public Booking Engine", description = "Unauthenticated property search and booking")
public class PublicReservationController {

    private final PublicReservationService publicReservationService;

    @GetMapping("/availability")
    @Operation(summary = "Check property availability for a date range")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> availability(
            @RequestParam UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(ApiResponse.success(
                publicReservationService.availability(propertyId, checkIn, checkOut)));
    }

    @PostMapping
    @Operation(summary = "Create a booking request")
    public ResponseEntity<ApiResponse<PublicReservationResponse>> create(
            @Valid @RequestBody PublicBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                publicReservationService.createBooking(request),
                "Cererea de rezervare a fost trimisă. Verifică emailul pentru detalii."));
    }

    @GetMapping("/manage/{token}")
    @Operation(summary = "Get a reservation by its management token")
    public ResponseEntity<ApiResponse<PublicReservationResponse>> getByToken(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(publicReservationService.getByToken(token)));
    }

    @PutMapping("/manage/{token}")
    @Operation(summary = "Modify a reservation using its management token")
    public ResponseEntity<ApiResponse<PublicReservationResponse>> update(
            @PathVariable String token, @Valid @RequestBody PublicBookingUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                publicReservationService.updateByToken(token, request), "Rezervarea a fost actualizată"));
    }

    @PostMapping("/manage/{token}/cancel")
    @Operation(summary = "Cancel a reservation using its management token")
    public ResponseEntity<ApiResponse<PublicReservationResponse>> cancel(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(
                publicReservationService.cancelByToken(token), "Rezervarea a fost anulată"));
    }
}
