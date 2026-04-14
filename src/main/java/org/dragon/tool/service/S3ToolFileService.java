package org.dragon.tool.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.domain.ToolStorageInfoVO;
import org.dragon.tool.enums.ToolStorageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Amazon S3（或兼容 S3 协议的对象存储）的 Tool 文件存储实现。
 *
 * <p>激活条件：配置项 {@code tool.storage.type=s3}。
 *
 * <p><b>存储路径约定</b>：
 * <pre>
 * s3://{bucket}/tools/{toolId}/v{version}/{relativePath}
 * </pre>
 *
 * <p><b>接入方式</b>：在 pom.xml 中引入 AWS SDK，并向 Spring 容器注入
 * {@code S3Client} Bean，然后取消下方注释的注入代码即可：
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;software.amazon.awssdk&lt;/groupId&gt;
 *   &lt;artifactId&gt;s3&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 * </pre>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "tool.storage.type", havingValue = "s3")
public class S3ToolFileService implements ToolFileService {

    /** S3 bucket 名称 */
    @Value("${tool.storage.s3.bucket}")
    private String s3Bucket;

    // 注入 AWS SDK S3 客户端，例如：
    // @Autowired
    // private software.amazon.awssdk.services.s3.S3Client s3Client;
    //
    // 或 AWS SDK v1：
    // @Autowired
    // private com.amazonaws.services.s3.AmazonS3 s3Client;

    // ── 公共 API ─────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolStorageType getStorageType() {
        return ToolStorageType.S3;
    }

    /**
     * {@inheritDoc}
     *
     * <p>文件上传至 S3 路径 {@code {bucket}/tools/{toolId}/v{version}/}。
     */
    @Override
    public ToolStorageInfoVO upload(String toolId, int version, Map<String, byte[]> fileMap) {
        String basePath = buildBasePath(toolId, version);
        List<ToolStorageInfoVO.ToolFileItem> fileItems = new ArrayList<>();

        for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
            String relativePath = entry.getKey();
            byte[] content = entry.getValue();
            String s3Key = basePath + "/" + relativePath;

            putObject(s3Bucket, s3Key, content);
            log.debug("[S3ToolStorageService] 上传文件到 S3: s3://{}/{}", s3Bucket, s3Key);

            ToolStorageInfoVO.ToolFileItem item = new ToolStorageInfoVO.ToolFileItem();
            item.setPath(relativePath);
            item.setSize((long) content.length);
            item.setType(resolveFileType(relativePath));
            fileItems.add(item);
        }

        ToolStorageInfoVO vo = new ToolStorageInfoVO();
        vo.setBucket(s3Bucket);
        vo.setBasePath(basePath);
        vo.setFiles(fileItems);

        log.info("[S3ToolStorageService] 上传完成: toolId={}, version={}, files={}",
                toolId, version, fileItems.size());
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] download(ToolStorageInfoVO storageInfo, String relativePath) {
        String s3Key = storageInfo.getBasePath() + "/" + relativePath;
        return getObject(storageInfo.getBucket(), s3Key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>结果上传至 S3 路径 {@code s3://{bucket}/tool-results/{sessionId}/{toolUseId}.txt}。
     *
     * @return {@code s3://{bucket}/tool-results/{sessionId}/{toolUseId}.txt}
     */
    @Override
    public String storeResult(String sessionId, String toolUseId, String content) {
        String safeSession = sanitize(sessionId);
        String safeUseId   = sanitize(toolUseId);
        String s3Key = "tool-results/" + safeSession + "/" + safeUseId + ".txt";

        putObject(s3Bucket, s3Key, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        log.debug("[S3ToolFileService] 工具结果已落存: s3://{}/{}", s3Bucket, s3Key);
        return "s3://" + s3Bucket + "/" + s3Key;
    }

    // ── S3 操作（替换为 SDK 真实调用）────────────────────────────────

    /**
     * 上传文件到 S3。
     *
     * <p>替换此方法体为实际 SDK 调用，例如（AWS SDK v2）：
     * <pre>
     * PutObjectRequest req = PutObjectRequest.builder()
     *         .bucket(bucket).key(key).build();
     * s3Client.putObject(req, RequestBody.fromBytes(content));
     * </pre>
     *
     * @param bucket  S3 bucket 名称
     * @param key     S3 对象 key
     * @param content 文件字节内容
     */
    private void putObject(String bucket, String key, byte[] content) {
        // TODO: 注入 s3Client 并实现上传逻辑
        throw new UnsupportedOperationException(
                "S3 客户端未注入，请在 S3ToolStorageService 中注入 S3Client Bean");
    }

    /**
     * 从 S3 下载文件。
     *
     * <p>替换此方法体为实际 SDK 调用，例如（AWS SDK v2）：
     * <pre>
     * GetObjectRequest req = GetObjectRequest.builder()
     *         .bucket(bucket).key(key).build();
     * return s3Client.getObjectAsBytes(req).asByteArray();
     * </pre>
     *
     * @param bucket S3 bucket 名称
     * @param key    S3 对象 key
     * @return 文件字节内容
     */
    private byte[] getObject(String bucket, String key) {
        // TODO: 注入 s3Client 并实现下载逻辑
        throw new UnsupportedOperationException(
                "S3 客户端未注入，请在 S3ToolStorageService 中注入 S3Client Bean");
    }

    // ── 私有工具 ─────────────────────────────────────────────────────

    /** 构建 S3 对象路径前缀（不含 bucket） */
    private String buildBasePath(String toolId, int version) {
        return "tools/" + toolId + "/v" + version;
    }

    /** 清理路径中的危险字符，防止路径遍历 */
    private String sanitize(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[/\\\\.\\n\\r\\t]", "_");
    }

    /**
     * 根据扩展名推断文件类型标签。
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
}

