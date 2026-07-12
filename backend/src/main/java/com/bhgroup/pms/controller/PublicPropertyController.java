package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.Facility;
import com.bhgroup.pms.dto.publicapi.PublicCalendarEntryResponse;
import com.bhgroup.pms.dto.publicapi.PublicPropertyResponse;
import com.bhgroup.pms.dto.publicapi.PublicPropertySummaryResponse;
import com.bhgroup.pms.service.IcalExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bhgroup.pms.dto.publicapi.PublicPropertyResponse;
import com.bhgroup.pms.dto.publicapi.PublicPropertySummaryResponse;
import com.bhgroup.pms.service.PublicPropertyService;
@RestController
@RequestMapping("/api/v1/public/properties")
@RequiredArgsConstructor
@Tag(name = "Public Booking Engine", description = "Unauthenticated property search and booking")
public class PublicPropertyController {

    private final PublicPropertyService publicPropertyService;
    private final IcalExportService icalExportService;

    @GetMapping
    @Operation(summary = "Search publicly bookable properties")
    public ResponseEntity<ApiResponse<PageResponse<PublicPropertySummaryResponse>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Set<Facility> facilities,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                publicPropertyService.search(
                        search, guests, bedrooms, minPrice, maxPrice, facilities, checkIn, checkOut, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a publicly bookable property by id")
    public ResponseEntity<ApiResponse<PublicPropertyResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(publicPropertyService.get(id)));
    }

    @GetMapping("/{id}/calendar")
    @Operation(summary = "Get booked date ranges for a property (no guest details) so guests can see availability")
    public ResponseEntity<ApiResponse<List<PublicCalendarEntryResponse>>> calendar(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(publicPropertyService.calendar(id, from, to)));
    }

    @GetMapping("/{id}/calendar.ics")
    @Operation(summary = "Token-authenticated .ics feed of booked dates, for importing into Airbnb/Booking.com")
    public ResponseEntity<String> calendarIcs(@PathVariable UUID id, @RequestParam String token) {
        String feed = icalExportService.generateFeed(id, token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"calendar.ics\"")
                .body(feed);
    }
}
