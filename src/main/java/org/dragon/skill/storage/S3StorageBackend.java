package org.dragon.skill.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * S3 对象存储后端实现（预留骨架）。
 * 启用条件：配置 skill.storage.backend=s3
 *
 * S3 存储路径规范：
 *   Bucket: {skill.storage.s3.bucket}
 *   Key 前缀: skills/{workspaceId}/{skillName}/{version}/
 *   例：skills/0/weather-skill/3/SKILL.md
 *       skills/42/my-skill/1/scripts/run.sh
 *
 * 数据库中 storagePath 存储格式：
 *   s3://{bucket}/skills/{workspaceId}/{skillName}/{version}
 *
 * @since 1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "skill.storage.backend", havingValue = "s3")
public class S3StorageBackend implements SkillStorageBackend {

    // TODO: 注入 S3Client（software.amazon.awssdk:s3）
    // private final S3Client s3Client;
    // private final String bucket;

    @Override
    public String store(long workspaceId, String skillName, int version, InputStream zipStream) {
        // TODO: 解压 ZIP，逐文件上传到 S3
        // String keyPrefix = "skills/" + workspaceId + "/" + skillName + "/" + version;
        // 返回格式：s3://{bucket}/{keyPrefix}
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public void download(String storagePath, Path localTarget) {
        // TODO: 列举 S3 前缀下所有对象，批量下载到 localTarget
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public InputStream readFile(String storagePath, String relativePath) {
        // TODO: 解析 storagePath，拼接 key，调用 s3Client.getObject()
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public void delete(String storagePath) {
        // TODO: 列举并批量删除 S3 前缀下所有对象
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public boolean exists(String storagePath) {
        // TODO: 检查 S3 前缀是否存在对象
        throw new UnsupportedOperationException("S3 存储后端尚未实现");
    }

    @Override
    public String backendType() {
        return "S3";
    }
}