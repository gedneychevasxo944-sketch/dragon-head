package org.dragon.tool.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.domain.ToolStorageInfoVO;
import org.dragon.tool.enums.ToolStorageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于本地文件系统的 Tool 文件存储实现（开发 / 测试环境）。
 *
 * <p>激活条件：配置项 {@code tool.storage.type=local}（默认值）。
 *
 * <p><b>存储路径约定</b>：
 * <pre>
 * {tool.storage.local.base-path}/tools/{toolId}/v{version}/{relativePath}
 * </pre>
 *
 * <p><b>安全保护</b>：对每个上传文件做路径遍历防护，拒绝含 {@code ..} 或绝对路径的文件名。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "tool.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalToolFileService implements ToolFileService {

    /** 本地存储根目录，默认 /data/tools */
    @Value("${tool.storage.local.base-path:/data/tools}")
    private String localBasePath;

    // ── 公共 API ─────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolStorageType getStorageType() {
        return ToolStorageType.LOCAL;
    }

    /**
     * {@inheritDoc}
     *
     * <p>文件写入本地目录 {@code {localBasePath}/tools/{toolId}/v{version}/}。
     * 若文件已存在则覆盖（支持草稿反复保存场景）。
     */
    @Override
    public ToolStorageInfoVO upload(String toolId, int version, Map<String, byte[]> fileMap) {
        String basePath = resolveBasePath(toolId, version);
        Path baseDir = Paths.get(basePath);
        List<ToolStorageInfoVO.ToolFileItem> fileItems = new ArrayList<>();

        for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
            String relativePath = entry.getKey();
            byte[] content = entry.getValue();

            validateRelativePath(relativePath, baseDir);

            Path targetPath = baseDir.resolve(relativePath).normalize();
            try {
                Files.createDirectories(targetPath.getParent());
                // TRUNCATE_EXISTING：覆盖写，支持草稿版本多次上传
                Files.write(targetPath, content,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.debug("[LocalToolStorageService] 写入本地文件: {}", targetPath);
            } catch (IOException e) {
                throw new ToolStorageException(
                        "文件写入失败: " + relativePath + "，原因: " + e.getMessage(), e);
            }

            ToolStorageInfoVO.ToolFileItem item = new ToolStorageInfoVO.ToolFileItem();
            item.setPath(relativePath);
            item.setSize((long) content.length);
            item.setType(resolveFileType(relativePath));
            fileItems.add(item);
        }

        ToolStorageInfoVO vo = new ToolStorageInfoVO();
        vo.setBucket(null);   // 本地模式无 bucket
        vo.setBasePath(basePath);
        vo.setFiles(fileItems);

        log.info("[LocalToolStorageService] 上传完成: toolId={}, version={}, files={}",
                toolId, version, fileItems.size());
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] download(ToolStorageInfoVO storageInfo, String relativePath) {
        Path targetPath = Paths.get(storageInfo.getBasePath(), relativePath);
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new ToolStorageException(
                    "文件读取失败: " + relativePath + "，原因: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>结果文件写入本地目录 {@code {localBasePath}/tool-results/{sessionId}/{toolUseId}.txt}。
     */
    @Override
    public String storeResult(String sessionId, String toolUseId, String content) {
        String safeSessId = sanitize(sessionId);
        String safeUseId  = sanitize(toolUseId);
        Path targetPath = Paths.get(localBasePath, "tool-results", safeSessId, safeUseId + ".txt");
        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, content,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("[LocalToolFileService] 工具结果已落存: {}", targetPath);
            return targetPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new ToolStorageException(
                    "工具结果写入失败: sessionId=" + sessionId + ", toolUseId=" + toolUseId
                            + "，原因: " + e.getMessage(), e);
        }
    }

    // ── 私有工具 ─────────────────────────────────────────────────────

    /**
     * 路径遍历防护：校验 relativePath 规范化后仍在 baseDir 内部。
     *
     * <p>对抗两类攻击：
     * <ol>
     *   <li>绝对路径：{@code /etc/passwd}</li>
     *   <li>目录逃逸：{@code ../../secret.sh}</li>
     * </ol>
     */
    private void validateRelativePath(String relativePath, Path baseDir) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ToolStorageException("文件路径不能为空");
        }
        Path normalized = baseDir.resolve(relativePath).normalize();
        if (!normalized.startsWith(baseDir.normalize())) {
            throw new ToolStorageException("非法文件路径（路径遍历）: " + relativePath);
        }
    }

    /** 构建本地完整目录路径 */
    private String resolveBasePath(String toolId, int version) {
        return localBasePath + "/tools/" + toolId + "/v" + version;
    }

    /** 清理路径中的危险字符，防止路径遍历 */
    private String sanitize(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[/\\\\.\\n\\r\\t]", "_");
    }

    /**
     * 根据扩展名推断文件类型标签。
     *
     * <p>返回值用于 UI 语法高亮提示，不影响执行逻辑。
     */
    private String resolveFileType(String relativePath) {
        String lower = relativePath.toLowerCase();
        if (lower.endsWith(".py"))                              return "python";
        if (lower.endsWith(".js"))                              return "javascript";
        if (lower.endsWith(".ts"))                              return "typescript";
        if (lower.endsWith(".sh") || lower.endsWith(".bash"))  return "shell";
        if (lower.endsWith(".json"))                            return "json";
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "yaml";
        if (lower.endsWith(".md"))                             return "markdown";
        return "text";
    }

    // ── 异常类 ───────────────────────────────────────────────────────

    /**
     * Tool 文件存储异常。
     */
    public static class ToolStorageException extends RuntimeException {

        /**
         * @param message 错误描述
         */
        public ToolStorageException(String message) {
            super(message);
        }

        /**
         * @param message 错误描述
         * @param cause   原始异常
         */
        public ToolStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

