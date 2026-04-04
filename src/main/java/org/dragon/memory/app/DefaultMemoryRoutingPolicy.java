package org.dragon.memory.app;

import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.core.MemoryRoutingPolicy;
import org.dragon.memory.core.MemoryScope;
import org.dragon.memory.core.MemoryType;
import org.dragon.memory.core.SessionSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 记忆路由策略默认实现
 * 负责判断候选记忆应该存储到哪个作用域（CHARACTER / WORKSPACE / SESSION）
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultMemoryRoutingPolicy implements MemoryRoutingPolicy {
    // 角色记忆类型集合
    private static final Set<MemoryType> CHARACTER_MEMORY_TYPES = Set.of(
            MemoryType.CHARACTER_PROFILE,
            MemoryType.FEEDBACK
    );

    // 工作空间记忆类型集合
    private static final Set<MemoryType> WORKSPACE_MEMORY_TYPES = Set.of(
            MemoryType.PROJECT,
            MemoryType.REFERENCE,
            MemoryType.WORKSPACE_DECISION
    );

    // 临时记忆类型集合（仅保留在SESSION中）
    private static final Set<MemoryType> TEMPORARY_MEMORY_TYPES = Set.of(
            MemoryType.SESSION_SUMMARY
    );

    @Override
    public MemoryScope route(MemoryEntry candidate, SessionSnapshot snapshot) {
        // 首先根据类型判断
        MemoryType type = candidate.getType();
        if (TEMPORARY_MEMORY_TYPES.contains(type)) {
            return MemoryScope.SESSION;
        }
        if (CHARACTER_MEMORY_TYPES.contains(type)) {
            return MemoryScope.CHARACTER;
        }
        if (WORKSPACE_MEMORY_TYPES.contains(type)) {
            return MemoryScope.WORKSPACE;
        }

        // 根据内容判断
        String content = candidate.getContent().toLowerCase();
        String title = candidate.getTitle().toLowerCase();

        // 角色相关内容
        if (isCharacterRelated(content, title)) {
            return MemoryScope.CHARACTER;
        }

        // 工作空间相关内容
        if (isWorkspaceRelated(content, title)) {
            return MemoryScope.WORKSPACE;
        }

        // 临时内容
        if (isTemporary(content, title)) {
            return MemoryScope.SESSION;
        }

        // 默认路由：根据是否有工作空间ID来判断
        return snapshot.getWorkspaceId() != null ? MemoryScope.WORKSPACE : MemoryScope.CHARACTER;
    }

    /**
     * 判断内容是否与角色相关
     */
    private boolean isCharacterRelated(String content, String title) {
        List<String> characterKeywords = List.of(
                "用户偏好", "角色", "行为风格", "个体规则", "回答方式", "习惯"
        );
        return containsAny(content, title, characterKeywords);
    }

    /**
     * 判断内容是否与工作空间相关
     */
    private boolean isWorkspaceRelated(String content, String title) {
        List<String> workspaceKeywords = List.of(
                "项目事实", "团队决策", "工作空间共识", "架构设计", "开发规范", "项目约束"
        );
        return containsAny(content, title, workspaceKeywords);
    }

    /**
     * 判断内容是否为临时内容
     */
    private boolean isTemporary(String content, String title) {
        List<String> temporaryKeywords = List.of(
                "临时", "暂时", "待确认", "当前会话", "本次讨论"
        );
        return containsAny(content, title, temporaryKeywords);
    }

    /**
     * 检查内容是否包含任何关键词
     */
    private boolean containsAny(String content, String title, List<String> keywords) {
        return keywords.stream().anyMatch(keyword ->
                content.contains(keyword) || title.contains(keyword)
        );
    }
}
