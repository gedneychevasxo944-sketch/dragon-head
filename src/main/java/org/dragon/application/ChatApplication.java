package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatApplication Chat 模块应用服务
 *
 * <p>对应前端 /chat 页面，聚合 AI 对话、会话历史、连接状态等业务逻辑。
 * 对话消息使用内存存储，会话历史在服务重启后不保留。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatApplication {

    private final CharacterRegistry characterRegistry;

    /** 会话消息存储：sessionId -> 消息列表 */
    private final Map<String, List<Map<String, Object>>> sessionMessages = new ConcurrentHashMap<>();

    // ==================== 对话 ====================

    /**
     * 发送消息并获取 AI 回复。
     *
     * @param sessionId   会话 ID（可选，为空时自动生成）
     * @param message     用户消息
     * @param characterId 指定角色 ID（可选）
     * @return 回复消息
     */
    public Map<String, Object> sendMessage(String sessionId, String message, String characterId) {
        String sid = sessionId != null && !sessionId.isBlank() ? sessionId : UUID.randomUUID().toString();
        String messageId = UUID.randomUUID().toString();
        String timestamp = LocalDateTime.now().toString();

        // 记录用户消息
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("id", UUID.randomUUID().toString());
        userMsg.put("role", "user");
        userMsg.put("content", message);
        userMsg.put("timestamp", timestamp);
        sessionMessages.computeIfAbsent(sid, k -> new ArrayList<>()).add(userMsg);

        // 执行 AI 回复
        String reply = generateReply(message, characterId, sid);
        String replyTimestamp = LocalDateTime.now().toString();

        // 记录 AI 消息
        Map<String, Object> assistantMsg = new HashMap<>();
        assistantMsg.put("id", messageId);
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", reply);
        assistantMsg.put("timestamp", replyTimestamp);
        sessionMessages.get(sid).add(assistantMsg);

        log.info("[ChatApplication] sendMessage sessionId={} characterId={}", sid, characterId);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sid);
        result.put("messageId", messageId);
        result.put("role", "assistant");
        result.put("content", reply);
        result.put("timestamp", replyTimestamp);
        return result;
    }

    /**
     * 获取会话历史消息。
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    public List<Map<String, Object>> getSessionMessages(String sessionId) {
        return sessionMessages.getOrDefault(sessionId, List.of());
    }

    /**
     * 获取 AI 连接状态。
     *
     * @return 连接状态信息
     */
    public Map<String, Object> getConnectionStatus() {
        boolean hasActiveCharacter = !characterRegistry.listAll().isEmpty();

        Map<String, Object> status = new HashMap<>();
        status.put("connected", hasActiveCharacter);
        status.put("model", "default");
        status.put("status", hasActiveCharacter ? "ready" : "error");
        return status;
    }

    // ==================== 内部工具 ====================

    /**
     * 调用 Character 生成回复。
     */
    private String generateReply(String message, String characterId, String sessionId) {
        try {
            Character character;
            if (characterId != null && !characterId.isBlank()) {
                character = characterRegistry.get(characterId)
                        .orElse(null);
            } else {
                character = characterRegistry.getDefaultCharacter().orElse(null);
            }

            if (character == null) {
                return "No character available. Please deploy a character first.";
            }

            String reply = character.run(message);
            return reply != null ? reply : "No response generated.";
        } catch (Exception e) {
            log.error("[ChatApplication] Failed to generate reply: {}", e.getMessage());
            return "Sorry, an error occurred while processing your message: " + e.getMessage();
        }
    }
}