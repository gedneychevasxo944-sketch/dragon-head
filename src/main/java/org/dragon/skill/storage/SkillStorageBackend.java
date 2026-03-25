package org.dragon.skill.storage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Skill 文件存储后端接口。
 * 屏蔽底层存储实现（本地文件系统 / S3 / 其他对象存储）的差异。
 *
 * 存储路径规范（逻辑路径，与具体存储无关）：
 *   {workspaceId}/{skillName}/{version}/
 *   例：0/weather-skill/3/SKILL.md
 *       42/my-custom-skill/1/scripts/run.sh
 *
 * @since 1.0
 */
public interface SkillStorageBackend {

    /**
     * 上传 Skill ZIP 包并解压存储。
     *
     * @param workspaceId 工作空间 ID（内置为 0）
     * @param skillName   Skill 名称
     * @param version     版本号
     * @param zipStream   ZIP 文件输入流
     * @return 存储后的根路径标识（本地为绝对路径，S3 为 bucket/prefix）
     */
    String store(long workspaceId, String skillName, int version, InputStream zipStream);

    /**
     * 将指定版本的 Skill 文件下载到本地目标目录。
     * 用于 Agent 执行前的文件准备阶段。
     *
     * @param storagePath 存储路径标识（来自数据库 storage_path 字段）
     * @param localTarget 本地目标目录（sandbox 或执行环境路径）
     */
    void download(String storagePath, Path localTarget);

    /**
     * 读取指定文件内容（单文件，不下载整个目录）。
     * 主要用于上传时解析 SKILL.md。
     *
     * @param storagePath 存储根路径
     * @param relativePath 相对路径，如 "SKILL.md" 或 "scripts/run.sh"
     * @return 文件内容输入流
     */
    InputStream readFile(String storagePath, String relativePath);

    /**
     * 删除指定版本的所有 Skill 文件。
     *
     * @param storagePath 存储路径标识
     */
    void delete(String storagePath);

    /**
     * 检查指定路径是否存在。
     */
    boolean exists(String storagePath);

    /**
     * 获取当前存储后端类型描述（用于日志和监控）。
     */
    String backendType();
}