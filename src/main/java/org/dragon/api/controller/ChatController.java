package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.application.ChatApplication;
import org.dragon.api.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * ChatController AI 对话模块 API
 *
 * <p>对应前端 /chat 页面，包含发送消息、获取会话历史、查询 AI 连接状态等接口。
 * Base URL: /api/v1/chat
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Chat", description = "AI 对话模块：发送消息、会话历史、连接状态")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatController {

    private final ChatApplication chatApplication;

    // ==================== 32. Chat（AI 对话）====================

    /**
     * 32.1 发送消息
     * POST /api/v1/chat/messages
     */
    @Operation(summary = "向 AI 发送消息并获取回复")
    @PostMapping("/messages")
    public ApiResponse<Map<String, Object>> sendMessage(@RequestBody SendMessageRequest request) {
        Map<String, Object> result = chatApplication.sendMessage(
                request.getSessionId(),
                request.getMessage(),
                request.getCharacterId());
        return ApiResponse.success(result);
    }

    /**
     * 32.2 获取会话历史
     * GET /api/v1/chat/sessions/:sessionId/messages
     */
    @Operation(summary = "获取会话历史消息")
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<Map<String, Object>>> getSessionMessages(
            @PathVariable String sessionId) {
        List<Map<String, Object>> messages = chatApplication.getSessionMessages(sessionId);
        return ApiResponse.success(messages);
    }

    /**
     * 32.3 获取 AI 连接状态
     * GET /api/v1/chat/status
     */
    @Operation(summary = "获取 AI 连接状态")
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getConnectionStatus() {
        Map<String, Object> status = chatApplication.getConnectionStatus();
        return ApiResponse.success(status);
    }

    // ==================== 请求体 DTO ====================

    /** 发送消息请求 */
    @Data
    public static class SendMessageRequest {
        /** 会话 ID（可选，不传则自动创建） */
        private String sessionId;
        /** 用户消息内容 */
        private String message;
        /** 指定角色 ID（可选） */
        private String characterId;
    }
}
