package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.dto.ical.IcalImportFeedCreateRequest;
import com.bhgroup.pms.dto.ical.IcalImportFeedResponse;
import com.bhgroup.pms.service.IcalExportService;
import com.bhgroup.pms.service.IcalImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/ical")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR')")
@Tag(name = "iCal Sync", description = "Export a property's booked dates to Airbnb/Booking.com and import theirs back")
public class IcalController {

    private final IcalExportService icalExportService;
    private final IcalImportService icalImportService;

    @PostMapping("/export-token")
    @Operation(summary = "Get (creating if needed) the property's .ics export feed token")
    public ResponseEntity<ApiResponse<Map<String, String>>> getOrCreateExportToken(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("token", icalExportService.getOrCreateExportToken(propertyId))));
    }

    @PostMapping("/export-token/regenerate")
    @Operation(summary = "Regenerate the property's .ics export feed token, invalidating the old URL")
    public ResponseEntity<ApiResponse<Map<String, String>>> regenerateExportToken(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("token", icalExportService.regenerateExportToken(propertyId)), "Export link regenerated"));
    }

    @GetMapping("/feeds")
    @Operation(summary = "List the property's registered Airbnb/Booking.com import feeds")
    public ResponseEntity<ApiResponse<List<IcalImportFeedResponse>>> listFeeds(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.success(icalImportService.listFeeds(propertyId)));
    }

    @PostMapping("/feeds")
    @Operation(summary = "Register an external .ics feed to import bookings from")
    public ResponseEntity<ApiResponse<IcalImportFeedResponse>> addFeed(
            @PathVariable UUID propertyId, @Valid @RequestBody IcalImportFeedCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(icalImportService.addFeed(propertyId, request), "Feed added"));
    }

    @PostMapping("/feeds/{feedId}/sync")
    @Operation(summary = "Sync a feed now instead of waiting for the hourly job")
    public ResponseEntity<ApiResponse<IcalImportFeedResponse>> syncFeed(
            @PathVariable UUID propertyId, @PathVariable UUID feedId) {
        return ResponseEntity.ok(ApiResponse.success(icalImportService.syncFeed(propertyId, feedId), "Feed synced"));
    }

    @DeleteMapping("/feeds/{feedId}")
    @Operation(summary = "Remove an external feed and the blocks it created")
    public ResponseEntity<ApiResponse<Void>> deleteFeed(@PathVariable UUID propertyId, @PathVariable UUID feedId) {
        icalImportService.deleteFeed(propertyId, feedId);
        return ResponseEntity.ok(ApiResponse.message("Feed removed"));
    }
}
