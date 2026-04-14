package org.dragon.tool.domain;

import lombok.Data;
import org.dragon.tool.enums.ToolStorageType;

import java.util.List;

/**
 * Tool 版本文件存储元信息（{@code storage_info} JSON 字段的 Java 映射）。
 *
 * <p>存储类型与位置信息一体化，序列化为 JSON 后存储在 {@link ToolVersionDO#getStorageInfo()} 中：
 * <ul>
 *   <li>S3 模式：{@code type=S3, bucket + basePath + files}</li>
 *   <li>LOCAL 模式：{@code type=LOCAL, basePath + files}（bucket 为 null）</li>
 * </ul>
 *
 * <p><b>路径约定</b>：
 * <pre>
 * tools/{toolId}/v{version}/main.py
 * tools/{toolId}/v{version}/requirements.txt
 * tools/{toolId}/v{version}/handler.sh
 * </pre>
 */
@Data
public class ToolStorageInfoVO {

    /**
     * 存储类型（LOCAL / S3）。
     */
    private ToolStorageType type;

    /**
     * S3 bucket 名称；LOCAL 模式为 null。
     */
    private String bucket;

    /**
     * 版本目录路径（不含文件名）。
     *
     * <p>LOCAL 模式为绝对路径，如 {@code /data/tools/tool-abc/v3}；
     * S3 模式为对象路径前缀，如 {@code tools/tool-abc/v3}。
     */
    private String basePath;

    /**
     * 该版本下所有文件列表。
     */
    private List<ToolFileItem> files;

    /**
     * Tool 版本的单个文件元信息。
     */
    @Data
    public static class ToolFileItem {

        /**
         * 相对于 {@link ToolStorageInfoVO#basePath} 的路径。
         *
         * <p>示例：{@code "main.py"}、{@code "lib/utils.py"}、{@code "run.sh"}
         */
        private String path;

        /**
         * 文件字节大小。
         */
        private Long size;

        /**
         * 文件类型标签，用于 UI 展示和语法高亮提示。
         *
         * <p>取值示例：{@code python}、{@code shell}、{@code javascript}、
         * {@code json}、{@code yaml}、{@code text}
         */
        private String type;
    }
}

