package org.dragon.skill.service;

import org.dragon.skill.domain.StorageInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Amazon S3（或兼容 S3 协议的对象存储）的 Skill 文件存储实现。
 *
 * <p>激活条件：配置项 {@code skill.storage.type=s3}。
 *
 * <p>存储路径约定：
 * <pre>
 * s3://{bucket}/skills/{skillId}/v{version}/SKILL.md
 * </pre>
 *
 * <p><b>接入方式</b>：在 pom.xml 中引入 AWS SDK，并向 Spring 容器注入
 * {@code AmazonS3} Bean，然后取消下方注释的注入代码即可。
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;software.amazon.awssdk&lt;/groupId&gt;
 *   &lt;artifactId&gt;s3&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 * </pre>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "skill.storage.type", havingValue = "s3")
public class S3SkillStorageService implements SkillStorageService {

    /** S3 bucket 名称 */
    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("skill.storage.s3.bucket")
    @Value("${skill.storage.s3.bucket}")
    private String s3Bucket;

    // 注入 AWS SDK S3 客户端，例如：
    // @Autowired
    // private software.amazon.awssdk.services.s3.S3Client s3Client;
    //
    // 或 AWS SDK v1：
    // @Autowired
    // private com.amazonaws.services.s3.AmazonS3 s3Client;

    // ── 公共 API ─────────────────────────────────────────────────────

    @Override
    public StorageInfoVO upload(String skillId, int version, Map<String, byte[]> fileMap) {
        String basePath = buildBasePath(skillId, version);
        List<StorageInfoVO.SkillFileItem> fileItems = new ArrayList<>();

        for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
            String relativePath = entry.getKey();
            byte[] content      = entry.getValue();
            String s3Key        = basePath + "/" + relativePath;

            putObject(s3Bucket, s3Key, content);
            log.debug("上传文件到 S3: s3://{}/{}", s3Bucket, s3Key);

            StorageInfoVO.SkillFileItem item = new StorageInfoVO.SkillFileItem();
            item.setPath(relativePath);
            item.setSize((long) content.length);
            item.setType(resolveFileType(relativePath));
            fileItems.add(item);
        }

        StorageInfoVO vo = new StorageInfoVO();
        vo.setBucket(s3Bucket);
        vo.setBasePath(basePath);
        vo.setRootFilePath(basePath + "/SKILL.md");
        vo.setFiles(fileItems);
        return vo;
    }

    @Override
    public byte[] download(StorageInfoVO storageInfo, String relativePath) {
        String s3Key = storageInfo.getBasePath() + "/" + relativePath;
        return getObject(storageInfo.getBucket(), s3Key);
    }

    // ── S3 操作（替换为 SDK 真实调用）────────────────────────────────

    /**
     * 上传文件到 S3。
     * <p>替换此方法体为实际 SDK 调用，例如：
     * <pre>
     * PutObjectRequest req = PutObjectRequest.builder()
     *         .bucket(bucket).key(key).build();
     * s3Client.putObject(req, RequestBody.fromBytes(content));
     * </pre>
     */
    private void putObject(String bucket, String key, byte[] content) {
        // TODO: 注入 s3Client 并实现上传逻辑
        throw new UnsupportedOperationException(
                "S3 客户端未注入，请在 S3SkillStorageService 中注入 AmazonS3 / S3Client Bean");
    }

    /**
     * 从 S3 下载文件。
     * <p>替换此方法体为实际 SDK 调用，例如：
     * <pre>
     * GetObjectRequest req = GetObjectRequest.builder()
     *         .bucket(bucket).key(key).build();
     * return s3Client.getObjectAsBytes(req).asByteArray();
     * </pre>
     */
    private byte[] getObject(String bucket, String key) {
        // TODO: 注入 s3Client 并实现下载逻辑
        throw new UnsupportedOperationException(
                "S3 客户端未注入，请在 S3SkillStorageService 中注入 AmazonS3 / S3Client Bean");
    }

    // ── 私有工具 ─────────────────────────────────────────────────────

    /** 构建 S3 对象路径前缀（不含 bucket） */
    private String buildBasePath(String skillId, int version) {
        return "skills/" + skillId + "/v" + version;
    }

    /** 根据扩展名推断文件类型标签 */
    private String resolveFileType(String relativePath) {
        String lower = relativePath.toLowerCase();
        if (lower.endsWith(".md"))                              return "markdown";
        if (lower.endsWith(".json"))                            return "json";
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "yaml";
        return "text";
    }
}

