package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.dto.messaging.MessageCreateRequest;
import com.bhgroup.pms.dto.messaging.MessageResponse;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations/{reservationId}/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
@Tag(name = "Messages", description = "Per-reservation guest/staff message thread")
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    @Operation(summary = "List all messages for a reservation")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> list(@PathVariable UUID reservationId) {
        messageService.markThreadReadByStaff(reservationId);
        return ResponseEntity.ok(ApiResponse.success(messageService.listForReservation(reservationId)));
    }

    @PostMapping
    @Operation(summary = "Send a staff message to the guest")
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @PathVariable UUID reservationId, @Valid @RequestBody MessageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                messageService.sendStaffMessage(reservationId, SecurityUtils.requireCurrentUserId(), request.body())));
    }
}
