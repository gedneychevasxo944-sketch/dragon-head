package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.character.Character;
import org.dragon.datasource.entity.TraitEntity;

import java.util.List;

/**
 * CharacterDetailDTO - Character 详情返回对象
 * <p>包含 Character 基础信息以及解析后的 Skill 和 Trait 完整对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterDetailDTO {

    /**
     * Character 基础信息
     */
    private Character character;

    /**
     * 已绑定的 Skill 列表（包含完整信息）
     */
    private List<SkillInfo> skills;

    /**
     * 已关联的 Trait 列表（包含完整信息）
     */
    private List<TraitInfo> traits;

    /**
     * Skill 基础信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillInfo {
        private String skillId;
        private String name;
        private String displayName;
        private String description;
        private String category;
    }

    /**
     * Trait 基础信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraitInfo {
        private String id;
        private String name;
        private String category;
        private String description;
        private String content;
    }

    /**
     * 从 TraitEntity 转换为 TraitInfo
     */
    public static TraitInfo fromTraitEntity(TraitEntity entity) {
        return TraitInfo.builder()
                .id(entity.getId())
                .name(entity.getName())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .content(entity.getContent())
                .build();
    }
}
