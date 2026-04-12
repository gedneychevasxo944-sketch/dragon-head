package org.dragon.memory.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * bindings.yml 读写工具类
 * 管理记忆目录下的绑定关系文件，格式为：
 * <pre>
 * bindings:
 *   sourceId-memoryId: mem/file_name.md
 *   sourceId-memoryId: mem/other_file.md
 * </pre>
 *
 * @author binarytom
 * @version 1.0
 */
@Slf4j
@Component
public class BindingsYmlManager {

    /**
     * 读取 bindings.yml，返回所有绑定记录
     *
     * @param bindingsFile bindings.yml 文件路径
     * @return key 为 sourceId-memoryId，value 为 mem/file_name.md
     */
    public Map<String, String> readBindings(Path bindingsFile) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!Files.exists(bindingsFile)) {
            return result;
        }
        try {
            List<String> lines = Files.readAllLines(bindingsFile);
            boolean inBindingsBlock = false;
            for (String line : lines) {
                if (line.trim().equals("bindings:") || line.trim().equals("bindings: {}")) {
                    inBindingsBlock = true;
                    continue;
                }
                if (inBindingsBlock && line.startsWith("  ")) {
                    // 解析缩进的 key: value 行
                    String trimmed = line.trim();
                    int colonIdx = trimmed.indexOf(": ");
                    if (colonIdx > 0) {
                        String key = trimmed.substring(0, colonIdx).trim();
                        String value = trimmed.substring(colonIdx + 2).trim();
                        result.put(key, value);
                    }
                } else if (inBindingsBlock && !line.isBlank()) {
                    // 遇到非缩进的非空行，退出 bindings 块
                    inBindingsBlock = false;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bindings.yml: " + bindingsFile, e);
        }
        return result;
    }

    /**
     * 追加或更新一条绑定记录
     *
     * @param bindingsFile bindings.yml 文件路径
     * @param key          绑定 key（sourceId-memoryId）
     * @param filePath     快照文件路径（mem/file_name.md）
     */
    public void writeBinding(Path bindingsFile, String key, String filePath) {
        log.info("[BindingsYmlManager] Writing binding: {} -> {}", key, filePath);
        Map<String, String> bindings = readBindings(bindingsFile);
        bindings.put(key, filePath);
        writeAll(bindingsFile, bindings);
    }

    /**
     * 删除一条绑定记录
     *
     * @param bindingsFile bindings.yml 文件路径
     * @param key          绑定 key（sourceId-memoryId）
     */
    public void deleteBinding(Path bindingsFile, String key) {
        log.info("[BindingsYmlManager] Deleting binding: {}", key);
        Map<String, String> bindings = readBindings(bindingsFile);
        if (bindings.remove(key) != null) {
            writeAll(bindingsFile, bindings);
        }
    }

    /**
     * 将所有绑定记录写回文件
     */
    private void writeAll(Path bindingsFile, Map<String, String> bindings) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("bindings:\n");
            if (bindings.isEmpty()) {
                // 保持 bindings: {} 格式表示空
                sb = new StringBuilder("bindings: {}\n");
            } else {
                for (Map.Entry<String, String> entry : bindings.entrySet()) {
                    sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            if (!Files.exists(bindingsFile.getParent())) {
                Files.createDirectories(bindingsFile.getParent());
            }
            Files.writeString(bindingsFile, sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write bindings.yml: " + bindingsFile, e);
        }
    }
}