package org.dragon.skill.service;

import org.dragon.skill.domain.StorageInfoVO;

import java.util.Map;

/**
 * Skill 文件存储抽象接口。
 *
 * <p>屏蔽底层存储差异，调用方（SkillRegisterService）面向此接口编程，
 * 通过配置项 {@code skill.storage.type} 自动选择实现：
 * <ul>
 *   <li>{@code local} → {@link LocalSkillStorageService}</li>
 *   <li>{@code s3}    → {@link S3SkillStorageService}</li>
 * </ul>
 *
 * <p>路径约定（两种实现共用）：
 * <pre>
 * skills/{skillId}/v{version}/SKILL.md
 * skills/{skillId}/v{version}/schema/input.json
 * </pre>
 */
public interface SkillStorageService {

    /**
     * 将 fileMap 中所有文件上传到存储后端，返回 StorageInfoVO。
     *
     * @param skillId 技能业务 UUID
     * @param version 本次版本号
     * @param fileMap 相对路径 → 文件字节内容
     * @return 填充好的 StorageInfoVO，供持久化到 storage_info 字段
     */
    StorageInfoVO upload(String skillId, int version, Map<String, byte[]> fileMap);

    /**
     * 下载单个文件的字节内容（供预览/内容加载使用）。
     *
     * @param storageInfo 存储元信息（由 storage_info 字段反序列化而来）
     * @param relativePath 相对路径，如 "SKILL.md"
     * @return 文件字节内容
     */
    byte[] download(StorageInfoVO storageInfo, String relativePath);
}

