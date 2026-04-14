package org.dragon.tool.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * API 格式的工具结果块参数。
 *
 * <p>对应 TypeScript 版本的 {@code ToolResultBlockParam} 类型，
 * 来自 {@code @anthropic-ai/sdk/resources/index.mjs}。
 *
 * <p>这是最终发送给 Claude API 的消息块格式：
 * <pre>
 * {
 *   "type": "tool_result",
 *   "tool_use_id": "toolu_xxx",
 *   "content": "..." | [{ "type": "text", "text": "..." }, ...],
 *   "is_error": false
 * }
 * </pre>
 *
 * <p>内容可以是：
 * <ul>
 *   <li>字符串：纯文本内容</li>
 *   <li>内容块数组：文本、图片等的混合内容</li>
 * </ul>
 */
@Data
@Builder
public class ToolResultBlockParam {

    /**
     * 固定为 "tool_result"。
     */
    @Builder.Default
    private final String type = "tool_result";

    /**
     * 对应的 tool_use 调用 ID。
     *
     * <p>模型发起的每个 tool_use 都有一个唯一的 ID，
     * tool_result 必须通过此 ID 与之配对。
     */
    private final String toolUseId;

    /**
     * 结果内容。
     *
     * <p>可以是字符串或内容块列表：
     * <ul>
     *   <li>字符串：直接作为文本内容</li>
     *   <li>List&lt;ContentBlock&gt;：混合内容（文本 + 图片等）</li>
     * </ul>
     */
    private final Object content;

    /**
     * 是否为错误结果。
     *
     * <p>当工具执行失败时设置为 true。
     */
    private final Boolean isError;

    /**
     * 缓存控制（用于提示缓存）。
     */
    private final CacheControl cacheControl;

    // ── 静态工厂方法 ─────────────────────────────────────────────────────

    /**
     * 创建文本结果。
     */
    public static ToolResultBlockParam ofText(String toolUseId, String content) {
        return ToolResultBlockParam.builder()
                .toolUseId(toolUseId)
                .content(content)
                .build();
    }

    /**
     * 创建文本结果（带错误标记）。
     */
    public static ToolResultBlockParam ofText(String toolUseId, String content, boolean isError) {
        return ToolResultBlockParam.builder()
                .toolUseId(toolUseId)
                .content(content)
                .isError(isError)
                .build();
    }

    /**
     * 创建内容块列表结果。
     */
    public static ToolResultBlockParam ofBlocks(String toolUseId, List<ContentBlock> blocks) {
        return ToolResultBlockParam.builder()
                .toolUseId(toolUseId)
                .content(blocks)
                .build();
    }

    /**
     * 创建错误结果。
     */
    public static ToolResultBlockParam ofError(String toolUseId, String errorMessage) {
        return ToolResultBlockParam.builder()
                .toolUseId(toolUseId)
                .content("<tool_use_error>" + errorMessage + "</tool_use_error>")
                .isError(true)
                .build();
    }

    // ── 内部类型 ─────────────────────────────────────────────────────────

    /**
     * 内容块基类。
     */
    public interface ContentBlock {
        String getType();
    }

    /**
     * 文本内容块。
     */
    @Data
    @Builder
    public static class TextBlock implements ContentBlock {
        @Builder.Default
        private final String type = "text";
        private final String text;
    }

    /**
     * 图片内容块。
     */
    @Data
    @Builder
    public static class ImageBlock implements ContentBlock {
        @Builder.Default
        private final String type = "image";
        private final ImageSource source;
    }

    /**
     * 图片来源。
     */
    @Data
    @Builder
    public static class ImageSource {
        private final String type;      // "base64" | "url"
        private final String mediaType; // "image/jpeg", "image/png", etc.
        private final String data;      // base64 数据或 URL
    }

    /**
     * 缓存控制。
     */
    @Data
    @Builder
    public static class CacheControl {
        private final String type;  // "ephemeral"
    }

    /**
     * 判断内容是否为空。
     */
    public boolean isContentEmpty() {
        if (content == null) return true;
        if (content instanceof String) {
            return ((String) content).trim().isEmpty();
        }
        if (content instanceof List) {
            return ((List<?>) content).isEmpty();
        }
        return false;
    }

    /**
     * 获取内容作为字符串（如果是文本内容）。
     */
    public String getContentAsString() {
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object block : (List<?>) content) {
                if (block instanceof TextBlock) {
                    sb.append(((TextBlock) block).getText());
                }
            }
            return sb.toString();
        }
        return content != null ? content.toString() : "";
    }
}
