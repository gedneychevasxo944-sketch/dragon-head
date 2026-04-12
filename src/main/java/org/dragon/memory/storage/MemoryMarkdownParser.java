package org.dragon.memory.storage;

import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.constants.MemoryScope;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.entity.SessionSnapshot;
import org.dragon.memory.entity.MemoryId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 解析器类
 * 负责 MemoryEntry 与 Markdown 文本之间的双向转换，以及 SessionSnapshot 的序列化
 *
 * @author binarytom
 * @version 1.0
 */
@Component
public class MemoryMarkdownParser {
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*([\\s\\S]*?)\\s*---", Pattern.MULTILINE);

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
        MemoryEntry entry = new MemoryEntry();

        // 解析 frontmatter
        Matcher frontmatterMatcher = FRONTMATTER_PATTERN.matcher(markdown);
        if (frontmatterMatcher.find()) {
            String frontmatter = frontmatterMatcher.group(1);
            parseFrontmatter(entry, frontmatter);
        }

        // 提取内容
        String content = frontmatterMatcher.replaceFirst("").trim();
        entry.setContent(content);

        return entry;
    }

    /**
     * 解析 frontmatter 内容
     */
    private void parseFrontmatter(MemoryEntry entry, String frontmatter) {
        for (String line : frontmatter.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int colonIndex = line.indexOf(":");
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();

                switch (key.toLowerCase()) {
                    case "id":
                        entry.setId(MemoryId.of(value));
                        break;
                    case "name":
                        entry.setTitle(value);
                        break;
                    case "summary":
                        entry.setDescription(value);
                        break;
                    case "scope":
                        try {
                            entry.setScope(MemoryScope.valueOf(value.toUpperCase()));
                        } catch (Exception e) {
                            entry.setScope(MemoryScope.CHARACTER);
                        }
                        break;
                    case "type":
                        try {
                            entry.setType(MemoryType.valueOf(value.toUpperCase()));
                        } catch (Exception e) {
                            entry.setType(MemoryType.REFERENCE);
                        }
                        break;
                    case "updated_at":
                        try {
                            entry.setUpdatedAt(Instant.parse(value));
                        } catch (Exception e) {
                            entry.setUpdatedAt(Instant.now());
                        }
                        break;
                }
            }
        }
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
        SessionSnapshot snapshot = new SessionSnapshot();
        snapshot.setContent(markdown);

        // 解析基本信息
        parseSessionBasicInfo(snapshot, markdown);
        parseSessionSummary(snapshot, markdown);
        parseSessionDecisions(snapshot, markdown);
        parseSessionQuestions(snapshot, markdown);

        return snapshot;
    }

    /**
     * 解析会话基本信息
     */
    private void parseSessionBasicInfo(SessionSnapshot snapshot, String markdown) {
        // 解析 Session ID
        Pattern pattern = Pattern.compile("\\*\\*Session ID\\*\\*: (\\S+)");
        Matcher matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            snapshot.setSessionId(matcher.group(1));
        }

        // 解析 Character ID
        pattern = Pattern.compile("\\*\\*Character ID\\*\\*: (\\S+)");
        matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            snapshot.setCharacterId(matcher.group(1));
        }

        // 解析 Workspace ID
        pattern = Pattern.compile("\\*\\*Workspace ID\\*\\*: (\\S+)");
        matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            snapshot.setWorkspaceId(matcher.group(1));
        }

        // 解析 Current Goal
        pattern = Pattern.compile("\\*\\*Current Goal\\*\\*: (.*)");
        matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            snapshot.setCurrentGoal(matcher.group(1).trim());
        }
    }

    /**
     * 解析会话摘要
     */
    private void parseSessionSummary(SessionSnapshot snapshot, String markdown) {
        Pattern pattern = Pattern.compile("## Summary\\s*(.*?)\\s*(?:## |$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            snapshot.setSummary(matcher.group(1).trim());
        }
    }

    /**
     * 解析会话决策
     */
    private void parseSessionDecisions(SessionSnapshot snapshot, String markdown) {
        Pattern pattern = Pattern.compile("## Recent Decisions\\s*(.*?)\\s*(?:## |$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            String decisionsText = matcher.group(1);
            Pattern decisionPattern = Pattern.compile("- (.*?)(?=\\n|$)");
            Matcher decisionMatcher = decisionPattern.matcher(decisionsText);
            while (decisionMatcher.find()) {
                snapshot.getRecentDecisions().add(decisionMatcher.group(1).trim());
            }
        }
    }

    /**
     * 解析会话未解决问题
     */
    private void parseSessionQuestions(SessionSnapshot snapshot, String markdown) {
        Pattern pattern = Pattern.compile("## Unresolved Questions\\s*(.*?)\\s*(?:## |$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            String questionsText = matcher.group(1);
            Pattern questionPattern = Pattern.compile("- (.*?)(?=\\n|$)");
            Matcher questionMatcher = questionPattern.matcher(questionsText);
            while (questionMatcher.find()) {
                snapshot.getUnresolvedQuestions().add(questionMatcher.group(1).trim());
            }
        }
    }
}
