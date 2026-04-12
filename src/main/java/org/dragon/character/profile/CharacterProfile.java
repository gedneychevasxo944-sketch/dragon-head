package org.dragon.character.profile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dragon.character.mind.Mind;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Character 数据体
 * 包含 Character 的静态数据和心智配置
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterProfile {

    /**
     * Character 全局唯一标识
     */
    private String id;

    /**
     * Character 名称
     */
    private String name;

    /**
     * 版本号
     */
    @Builder.Default
    private Integer version = 0;

    /**
     * 描述
     */
    private String description;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 角色来源
     */
    private String source;

    /**
     * 心智模块配置
     */
    private MindConfig mindConfig;

    /**
     * Mind 实例
     */
    @JsonIgnore
    private Mind mind;

    /**
     * 扩展属性
     */
    @Builder.Default
    private Map<String, Object> extensions = Map.of();

    /**
     * 当前 Character 允许使用的工具名称集合
     */
    @Builder.Default
    private Set<String> allowedTools = new HashSet<>();

    /**
     * Trait IDs
     */
    private List<String> traits;

    /**
     * Trait 配置
     */
    private Map<String, Object> traitConfigs;

    /**
     * 技能引用
     */
    private List<String> skills;

    /**
     * Prompt 模板
     */
    private String promptTemplate;

    /**
     * 默认工具列表
     */
    private List<String> defaultTools;

    /**
     * 是否正在运行
     */
    private Boolean isRunning;

    /**
     * 派驻数量
     */
    private Integer deployedCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 状态
     */
    private Status status;

    /**
     * Character 状态枚举
     */
    public enum Status {
        UNLOADED,
        LOADED,
        RUNNING,
        PAUSED,
        DESTROYED
    }

    /**
     * Mind 配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MindConfig {
        /** 性格描述文件路径 */
        private String personalityDescriptorPath;
        /** 标签存储类型 */
        private String tagRepositoryType;
        /** 记忆存储类型 */
        private String memoryAccessType;
        /** 技能存储类型 */
        private String skillAccessType;
    }
}
