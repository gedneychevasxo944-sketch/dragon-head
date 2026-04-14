package org.dragon.skill.domain;

import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.enums.StorageType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SkillVersionDO — 技能版本领域对象。
 *
 * <p>每次更新技能时 INSERT 新记录，skillId 不变，version +1。
 *
 * <p>字段分类：
 * <ul>
 *   <li>标识：id, skillId, version</li>
 *   <li>版本内容：name, description, content, frontmatter, runtimeConfig</li>
 *   <li>编辑者：editorId, editorName</li>
 *   <li>版本状态：status</li>
 *   <li>存储信息：storageType, storageInfo</li>
 *   <li>时间戳：createdAt, publishedAt</li>
 * </ul>
 */
@Data
public class SkillVersionDO {

    // ── 标识 ─────────────────────────────────────────────────────

    /** 物理自增主键 */
    private Long id;

    /** 所属技能 ID */
    private String skillId;

    /** 版本号 */
    private Integer version;

    // ── 版本内容 ─────────────────────────────────────────────────

    /** 版本名称（解析自 SKILL.md） */
    private String name;

    /** 版本描述（解析自 SKILL.md） */
    private String description;

    /** SKILL.md 正文内容 */
    private String content;

    /** SKILL.md 原始 frontmatter YAML */
    private String frontmatter;

    /** 运行时配置 JSON */
    private String runtimeConfig;

    // ── 编辑者 ─────────────────────────────────────────────────

    /** 本次编辑者用户 ID */
    private Long editorId;

    /** 本次编辑者用户名 */
    private String editorName;

    // ── 版本状态 ─────────────────────────────────────────────────

    /** 版本状态 */
    private SkillVersionStatus status;

    /** 发版备注 */
    private String releaseNote;

    // ── 存储信息 ─────────────────────────────────────────────────

    /** 存储类型 */
    private StorageType storageType;

    /** 存储信息 JSON */
    private String storageInfo;

    // ── 时间戳 ─────────────────────────────────────────────────

    /** 版本创建时间 */
    private LocalDateTime createdAt;

    /** 发布时间 */
    private LocalDateTime publishedAt;
}
