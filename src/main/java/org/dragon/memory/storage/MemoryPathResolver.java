package org.dragon.memory.storage;

import org.dragon.memory.config.MemoryProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 记忆路径解析器类
 * 负责统一管理记忆文件和目录的路径计算
 *
 * @author binarytom
 * @version 1.0
 */
@Component
public class MemoryPathResolver {
    private final MemoryProperties properties;

    public MemoryPathResolver(MemoryProperties properties) {
        this.properties = properties;
    }

    // Character Memory 路径解析

    /**
     * 解析角色记忆根目录
     *
     * @param characterId 角色ID
     * @return 角色记忆根目录路径
     */
    public Path resolveCharacterRoot(String characterId) {
        return Paths.get(properties.getRootDir(), "char-" + characterId);
    }

    /**
     * 解析角色记忆索引文件路径
     *
     * @param characterId 角色ID
     * @return 角色记忆索引文件路径
     */
    public Path resolveCharacterIndex(String characterId) {
        return resolveCharacterRoot(characterId).resolve("MEMORY.md");
    }

    /**
     * 解析角色记忆内容目录路径
     *
     * @param characterId 角色ID
     * @return 角色记忆内容目录路径
     */
    public Path resolveCharacterMemDir(String characterId) {
        return resolveCharacterRoot(characterId).resolve("mem");
    }

    /**
     * 解析角色记忆绑定关系文件路径
     *
     * @param characterId 角色ID
     * @return 角色记忆绑定关系文件路径
     */
    public Path resolveCharacterBindings(String characterId) {
        return resolveCharacterRoot(characterId).resolve("bindings.yml");
    }

    // Workspace Memory 路径解析

    /**
     * 解析工作空间记忆根目录
     *
     * @param workspaceId 工作空间ID
     * @return 工作空间记忆根目录路径
     */
    public Path resolveWorkspaceRoot(String workspaceId) {
        return Paths.get(properties.getRootDir(), "ws-" + workspaceId);
    }

    /**
     * 解析工作空间记忆索引文件路径
     *
     * @param workspaceId 工作空间ID
     * @return 工作空间记忆索引文件路径
     */
    public Path resolveWorkspaceIndex(String workspaceId) {
        return resolveWorkspaceRoot(workspaceId).resolve("MEMORY.md");
    }

    /**
     * 解析工作空间记忆内容目录路径
     *
     * @param workspaceId 工作空间ID
     * @return 工作空间记忆内容目录路径
     */
    public Path resolveWorkspaceMemDir(String workspaceId) {
        return resolveWorkspaceRoot(workspaceId).resolve("mem");
    }

    /**
     * 解析工作空间记忆绑定关系文件路径
     *
     * @param workspaceId 工作空间ID
     * @return 工作空间记忆绑定关系文件路径
     */
    public Path resolveWorkspaceBindings(String workspaceId) {
        return resolveWorkspaceRoot(workspaceId).resolve("bindings.yml");
    }

    // Session Memory 路径解析

    /**
     * 解析会话记忆根目录
     *
     * @param sessionId 会话ID
     * @return 会话记忆根目录路径
     */
    public Path resolveSessionRoot(String sessionId) {
        return Paths.get(properties.getRootDir(), "session-" + sessionId);
    }

    /**
     * 解析会话记忆文件路径
     *
     * @param sessionId 会话ID
     * @return 会话记忆文件路径
     */
    public Path resolveSessionMemoryFile(String sessionId) {
        return resolveSessionRoot(sessionId).resolve("session-memory.md");
    }

    /**
     * 解析会话事件文件路径
     *
     * @param sessionId 会话ID
     * @return 会话事件文件路径
     */
    public Path resolveSessionEventsFile(String sessionId) {
        return resolveSessionRoot(sessionId).resolve("events.jsonl");
    }

    /**
     * 解析会话检查点目录路径
     *
     * @param sessionId 会话ID
     * @return 会话检查点目录路径
     */
    public Path resolveSessionCheckpointsDir(String sessionId) {
        return resolveSessionRoot(sessionId).resolve("checkpoints");
    }
}