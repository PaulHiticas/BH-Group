package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.gdpr.GdprEraseRequest;
import com.bhgroup.pms.dto.gdpr.GdprEraseResultResponse;
import com.bhgroup.pms.dto.gdpr.GdprExportResponse;
import com.bhgroup.pms.dto.gdpr.GdprSearchMatchResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guests aren't user accounts in this system, so data-subject requests
 * are handled by email lookup rather than a user id. Restricted to
 * SUPER_ADMIN given how sensitive and hard to fully undo erasure is.
 */
@RestController
@RequestMapping("/api/v1/admin/gdpr")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "GDPR", description = "Data-subject access and erasure requests for guests and leads")
public class GdprController {

    private final GdprService gdprService;
    private final UserRepository userRepository;

    @GetMapping("/search")
    @Operation(summary = "Find all reservations/leads tied to an email address")
    public ResponseEntity<ApiResponse<List<GdprSearchMatchResponse>>> search(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(gdprService.search(email)));
    }

    @GetMapping("/export")
    @Operation(summary = "Export all personal data tied to an email address")
    public ResponseEntity<ApiResponse<GdprExportResponse>> export(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(gdprService.export(email, currentUser())));
    }

    @PostMapping("/erase")
    @Operation(summary = "Anonymize all personal data tied to an email address")
    public ResponseEntity<ApiResponse<GdprEraseResultResponse>> erase(@Valid @RequestBody GdprEraseRequest request) {
        if (!request.confirm()) {
            throw new BadRequestException("Erasure must be explicitly confirmed");
        }
        return ResponseEntity.ok(ApiResponse.success(
                gdprService.erase(request.email(), currentUser()), "Date anonimizate cu succes"));
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.requireCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
