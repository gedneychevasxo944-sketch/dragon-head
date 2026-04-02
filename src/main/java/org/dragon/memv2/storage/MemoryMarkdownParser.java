package org.dragon.memv2.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.SessionSnapshot;
import org.springframework.stereotype.Component;

/**
 * Markdown 解析器类
 * 负责 MemoryEntry 与 Markdown 文本之间的双向转换，以及 SessionSnapshot 的序列化
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryMarkdownParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将 MemoryEntry 渲染为 Markdown 字符串
     *
     * @param entry 记忆条目
     * @return Markdown 格式字符串
     */
    public String render(MemoryEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("id: ").append(entry.getId()).append("\n");
        sb.append("name: ").append(entry.getTitle()).append("\n");
        sb.append("summary: ").append(entry.getDescription()).append("\n");
        sb.append("scope: ").append(entry.getScope()).append("\n");
        sb.append("type: ").append(entry.getType()).append("\n");
        sb.append("updated_at: ").append(entry.getUpdatedAt()).append("\n");
        sb.append("---\n\n");
        sb.append(entry.getContent());
        return sb.toString();
    }

    /**
     * 将 Markdown 字符串解析为 MemoryEntry
     *
     * @param markdown Markdown 格式字符串
     * @return MemoryEntry 对象
     */
    public MemoryEntry parse(String markdown) {
        // 简化实现：实际应用中需要解析 frontmatter 和内容
        MemoryEntry entry = new MemoryEntry();
        entry.setContent(markdown);
        return entry;
    }

    /**
     * 将 SessionSnapshot 渲染为 Markdown 字符串
     *
     * @param snapshot 会话快照
     * @return Markdown 格式字符串
     */
    public String renderSession(SessionSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Session Memory\n\n");
        sb.append("- **Session ID**: ").append(snapshot.getSessionId()).append("\n");
        sb.append("- **Character ID**: ").append(snapshot.getCharacterId()).append("\n");
        sb.append("- **Workspace ID**: ").append(snapshot.getWorkspaceId()).append("\n");
        sb.append("- **Current Goal**: ").append(snapshot.getCurrentGoal()).append("\n");
        sb.append("\n## Summary\n\n").append(snapshot.getSummary()).append("\n");
        if (!snapshot.getRecentDecisions().isEmpty()) {
            sb.append("\n## Recent Decisions\n\n");
            for (String decision : snapshot.getRecentDecisions()) {
                sb.append("- ").append(decision).append("\n");
            }
        }
        if (!snapshot.getUnresolvedQuestions().isEmpty()) {
            sb.append("\n## Unresolved Questions\n\n");
            for (String question : snapshot.getUnresolvedQuestions()) {
                sb.append("- ").append(question).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 将 Markdown 字符串解析为 SessionSnapshot
     *
     * @param markdown Markdown 格式字符串
     * @return SessionSnapshot 对象
     */
    public SessionSnapshot parseSession(String markdown) {
        // 简化实现：实际应用中需要解析 Markdown 内容
        SessionSnapshot snapshot = new SessionSnapshot();
        snapshot.setContent(markdown);
        return snapshot;
    }
}
