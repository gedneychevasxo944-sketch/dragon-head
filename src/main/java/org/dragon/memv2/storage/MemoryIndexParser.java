package org.dragon.memv2.storage;
import org.dragon.memv2.core.MemoryId;


import org.dragon.memv2.core.MemoryIndexItem;
import org.dragon.memv2.core.MemoryType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 记忆索引解析器类
 * 负责解析 MEMORY.md 索引文件，将其转换为 MemoryIndexItem 对象列表
 *
 * @author binarytom
 * @version 1.0
 */
@Component
public class MemoryIndexParser {
    private static final Pattern INDEX_ITEM_PATTERN = Pattern.compile("\\[([^\\]]*)\\]\\((mem\\/[^)]*)\\) - (.*)");

    /**
     * 解析 MEMORY.md 内容为 MemoryIndexItem 列表
     *
     * @param content 索引文件内容
     * @return 记忆索引条目列表
     */
    public List<MemoryIndexItem> parse(String content) {
        List<MemoryIndexItem> items = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- ")) {
                line = line.substring(2).trim();
                Matcher matcher = INDEX_ITEM_PATTERN.matcher(line);
                if (matcher.find()) {
                    MemoryIndexItem item = new MemoryIndexItem();
                    item.setMemoryId(generateMemoryId(matcher.group(1)));
                    item.setTitle(matcher.group(1));
                    item.setRelativePath(matcher.group(2));
                    item.setSummaryLine(matcher.group(3));
                    item.setType(determineMemoryType(matcher.group(1), matcher.group(3)));
                    item.setUpdatedAt(Instant.now());
                    items.add(item);
                }
            }
        }

        return items;
    }

    /**
     * 生成记忆ID
     */
    private MemoryId generateMemoryId(String title) {
        String idString = title.toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_").trim();
        return MemoryId.of(idString);
    }

    /**
     * 根据标题和摘要确定记忆类型
     */
    private MemoryType determineMemoryType(String title, String summary) {
        String lowerTitle = title.toLowerCase();
        String lowerSummary = summary.toLowerCase();

        if (lowerTitle.contains("反馈") || lowerSummary.contains("反馈") || lowerTitle.contains("feedback")) {
            return MemoryType.FEEDBACK;
        } else if (lowerTitle.contains("项目") || lowerSummary.contains("项目") || lowerTitle.contains("project")) {
            return MemoryType.PROJECT;
        } else if (lowerTitle.contains("决策") || lowerSummary.contains("决策") || lowerTitle.contains("decision")) {
            return MemoryType.WORKSPACE_DECISION;
        } else if (lowerTitle.contains("配置") || lowerSummary.contains("配置") || lowerTitle.contains("profile")) {
            return MemoryType.CHARACTER_PROFILE;
        } else if (lowerTitle.contains("会话") || lowerSummary.contains("会话") || lowerTitle.contains("session")) {
            return MemoryType.SESSION_SUMMARY;
        } else {
            return MemoryType.REFERENCE;
        }
    }

    /**
     * 将 MemoryIndexItem 列表渲染为 MEMORY.md 内容
     *
     * @param items 索引条目列表
     * @return Markdown 格式的索引内容
     */
    public String render(List<MemoryIndexItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("# MEMORY\n\n");
        for (MemoryIndexItem item : items) {
            sb.append("- [").append(item.getTitle()).append("](mem/")
                    .append(item.getRelativePath()).append(") - ").append(item.getSummaryLine()).append("\n");
        }
        return sb.toString();
    }
}
