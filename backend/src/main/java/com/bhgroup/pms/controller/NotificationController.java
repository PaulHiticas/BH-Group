package com.bhgroup.pms.controller;

import com.bhgroup.pms.common.response.ApiResponse;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.dto.notification.NotificationResponse;
import com.bhgroup.pms.security.SecurityUtils;
import com.bhgroup.pms.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMINISTRATOR','SUPPORT_AGENT')")
@Tag(name = "Notifications", description = "In-app notifications for the authenticated staff member")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List my notifications")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.list(currentUserId(), unreadOnly, pageable)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get my unread notification count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", notificationService.unreadCount(currentUserId()))));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable UUID id) {
        notificationService.markRead(currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.message("Notification marked as read"));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all of my notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead(currentUserId());
        return ResponseEntity.ok(ApiResponse.message("All notifications marked as read"));
    }

    private UUID currentUserId() {
        return SecurityUtils.requireCurrentUserId();
    }
}
