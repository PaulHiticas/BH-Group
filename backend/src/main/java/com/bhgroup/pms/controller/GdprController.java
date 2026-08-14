package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.gdpr.GdprEraseRequest;
import com.bhgroup.pms.dto.gdpr.GdprEraseResultResponse;
import com.bhgroup.pms.dto.gdpr.GdprExportRequest;
import com.bhgroup.pms.dto.gdpr.GdprExportResponse;
import com.bhgroup.pms.dto.gdpr.GdprSearchMatchResponse;
import com.bhgroup.pms.dto.gdpr.GdprSearchRequest;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.service.GdprService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guests aren't user accounts in this system, so data-subject requests
 * are handled by email lookup rather than a user id. Restricted to
 * SUPER_ADMIN given how sensitive and hard to fully undo erasure is.
 *
 * search/export are POST with the email in the body, not GET with it in
 * the query string - a query string routinely ends up in reverse-proxy,
 * CDN, and APM logs the application itself never touches, which would
 * reintroduce the exact PII exposure this feature exists to close.
 */
@RestController
@RequestMapping("/api/v1/admin/gdpr")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "GDPR", description = "Data-subject access and erasure requests for guests and leads")
public class GdprController {

    private final GdprService gdprService;
    private final UserRepository userRepository;

    @PostMapping("/search")
    @Operation(summary = "Find all reservations/leads tied to an email address")
    public ResponseEntity<ApiResponse<List<GdprSearchMatchResponse>>> search(@Valid @RequestBody GdprSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(gdprService.search(request.email())));
    }

    @PostMapping("/export")
    @Operation(summary = "Export all personal data tied to an email address")
    public ResponseEntity<ApiResponse<GdprExportResponse>> export(@Valid @RequestBody GdprExportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(gdprService.export(
                request.email(), request.verificationMethod(), request.verificationNote(), currentUser())));
    }

    @PostMapping("/erase")
    @Operation(summary = "Anonymize all personal data tied to an email address")
    public ResponseEntity<ApiResponse<GdprEraseResultResponse>> erase(@Valid @RequestBody GdprEraseRequest request) {
        if (!request.confirm()) {
            throw new BadRequestException("Erasure must be explicitly confirmed");
        }
        if (!request.email().equalsIgnoreCase(request.confirmationEmail())) {
            throw new BadRequestException("Confirmation email does not match the requested email");
        }
        return ResponseEntity.ok(ApiResponse.success(
                gdprService.erase(request.email(), request.verificationMethod(), request.verificationNote(), currentUser()),
                "Date anonimizate cu succes"));
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
