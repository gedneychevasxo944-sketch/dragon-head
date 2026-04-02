package org.dragon.memv2.core;

import java.util.List;

/**
 * 代理记忆上下文
 * 负责管理代理的记忆上下文，包括会话记忆、角色记忆和工作空间记忆的访问
 *
 * @author binarytom
 * @version 1.0
 */
public class AgentMemoryContext {
    private final String agentId;
    private final String sessionId;
    private final String characterId;
    private final String workspaceId;
    private final SessionMemoryService sessionMemoryService;
    private final CharacterMemoryService characterMemoryService;
    private final WorkspaceMemoryService workspaceMemoryService;
    private final MemoryRecallService memoryRecallService;
    private final SessionToLongTermBridge sessionToLongTermBridge;

    private AgentMemoryContext(Builder builder) {
        this.agentId = builder.agentId;
        this.sessionId = builder.sessionId;
        this.characterId = builder.characterId;
        this.workspaceId = builder.workspaceId;
        this.sessionMemoryService = builder.sessionMemoryService;
        this.characterMemoryService = builder.characterMemoryService;
        this.workspaceMemoryService = builder.workspaceMemoryService;
        this.memoryRecallService = builder.memoryRecallService;
        this.sessionToLongTermBridge = builder.sessionToLongTermBridge;
    }

    /**
     * 获取代理ID
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * 获取会话ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 获取角色ID
     */
    public String getCharacterId() {
        return characterId;
    }

    /**
     * 获取工作空间ID
     */
    public String getWorkspaceId() {
        return workspaceId;
    }

    /**
     * 获取会话记忆服务
     */
    public SessionMemoryService getSessionMemoryService() {
        return sessionMemoryService;
    }

    /**
     * 获取角色记忆服务
     */
    public CharacterMemoryService getCharacterMemoryService() {
        return characterMemoryService;
    }

    /**
     * 获取工作空间记忆服务
     */
    public WorkspaceMemoryService getWorkspaceMemoryService() {
        return workspaceMemoryService;
    }

    /**
     * 获取记忆召回服务
     */
    public MemoryRecallService getMemoryRecallService() {
        return memoryRecallService;
    }

    /**
     * 获取会话到长期记忆的转换桥接
     */
    public SessionToLongTermBridge getSessionToLongTermBridge() {
        return sessionToLongTermBridge;
    }

    /**
     * 记忆召回方法
     */
    public List<MemorySearchResult> recall(MemoryQuery query) {
        return memoryRecallService.recallComposite(query);
    }

    /**
     * 保存记忆到会话
     */
    public void saveToSession(SessionSnapshot snapshot) {
        if (sessionId != null) {
            sessionMemoryService.update(sessionId, snapshot);
        }
    }

    /**
     * 将会话记忆转换为长期记忆
     */
    public List<MemoryEntry> convertSessionToLongTerm() {
        if (sessionId != null) {
            return sessionToLongTermBridge.convertSessionToLongTerm(sessionId);
        }
        return List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String agentId;
        private String sessionId;
        private String characterId;
        private String workspaceId;
        private SessionMemoryService sessionMemoryService;
        private CharacterMemoryService characterMemoryService;
        private WorkspaceMemoryService workspaceMemoryService;
        private MemoryRecallService memoryRecallService;
        private SessionToLongTermBridge sessionToLongTermBridge;

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder characterId(String characterId) {
            this.characterId = characterId;
            return this;
        }

        public Builder workspaceId(String workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public Builder sessionMemoryService(SessionMemoryService sessionMemoryService) {
            this.sessionMemoryService = sessionMemoryService;
            return this;
        }

        public Builder characterMemoryService(CharacterMemoryService characterMemoryService) {
            this.characterMemoryService = characterMemoryService;
            return this;
        }

        public Builder workspaceMemoryService(WorkspaceMemoryService workspaceMemoryService) {
            this.workspaceMemoryService = workspaceMemoryService;
            return this;
        }

        public Builder memoryRecallService(MemoryRecallService memoryRecallService) {
            this.memoryRecallService = memoryRecallService;
            return this;
        }

        public Builder sessionToLongTermBridge(SessionToLongTermBridge sessionToLongTermBridge) {
            this.sessionToLongTermBridge = sessionToLongTermBridge;
            return this;
        }

        public AgentMemoryContext build() {
            return new AgentMemoryContext(this);
        }
    }
}
