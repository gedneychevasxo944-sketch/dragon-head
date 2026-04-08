package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.api.dto.ApiResponse;
import org.dragon.notification.dto.NotificationDTO;
import org.dragon.notification.service.NotificationService;
import org.dragon.user.security.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * NotificationController 通知管理 API
 *
 * <p>提供通知的查询、标记已读等接口。
 * Base URL: /api/v1/notifications
 */
@Tag(name = "Notification", description = "通知管理")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    private static final int DEFAULT_LIMIT = 20;

    /**
     * 获取我的通知列表
     * GET /api/v1/notifications
     */
    @Operation(summary = "获取我的通知列表")
    @GetMapping
    public ApiResponse<List<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "20") int limit) {
        List<NotificationDTO> notifications = notificationService.getNotifications(
                principal.getUserId(), limit);
        return ApiResponse.success(notifications);
    }

    /**
     * 获取未读通知数量
     * GET /api/v1/notifications/unread-count
     */
    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        long count = notificationService.getUnreadCount(principal.getUserId());
        return ApiResponse.success(Map.of("count", count));
    }

    /**
     * 标记单条通知为已读
     * POST /api/v1/notifications/{id}/read
     */
    @Operation(summary = "标记单条通知为已读")
    @PostMapping("/{id}/read")
    public ApiResponse<Map<String, Object>> markAsRead(
            @PathVariable String id) {
        notificationService.markAsRead(id);
        return ApiResponse.success(Map.of("success", true, "message", "已标记为已读"));
    }

    /**
     * 标记所有通知为已读
     * POST /api/v1/notifications/read-all
     */
    @Operation(summary = "标记所有通知为已读")
    @PostMapping("/read-all")
    public ApiResponse<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getUserId());
        return ApiResponse.success(Map.of("success", true, "message", "已全部标记为已读"));
    }

    /**
     * 删除通知
     * DELETE /api/v1/notifications/{id}
     */
    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteNotification(
            @PathVariable String id) {
        notificationService.deleteNotification(id);
        return ApiResponse.success(Map.of("success", true, "message", "通知已删除"));
    }
}
