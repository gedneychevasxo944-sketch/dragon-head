package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dragon.character.Character;
import org.dragon.character.profile.CharacterProfile;

/**
 * CharacterEntity Character实体
 * 映射数据库 character 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "`character`")
public class CharacterEntity {

    @Id
    private String id;

    @Column(name = "workspace_ids")
    @DbJson
    private List<String> workspaceIds;

    private String name;

    private Integer version;

    private String description;

    private String avatar;

    private String source;

    @Column(name = "allowed_tools")
    @DbJson
    private Set<String> allowedTools;

    @DbJson
    private List<String> traits;

    @DbJson
    private Map<String, Object> traitConfigs;

    @DbJson
    private List<String> skills;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "default_tools")
    @DbJson
    private List<String> defaultTools;

    @Column(name = "is_running")
    private Boolean isRunning;

    @Column(name = "deployed_count")
    private Integer deployedCount;

    @Column(name = "mind_config")
    @DbJson
    private CharacterProfile.MindConfig mindConfig;

    @Column(columnDefinition = "TEXT")
    private String extensions;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 转换为Character
     */
    public Character toCharacter() {
        CharacterProfile profile = new CharacterProfile();
        profile.setId(this.id);
        profile.setWorkspaceIds(this.workspaceIds);
        profile.setName(this.name);
        profile.setVersion(this.version);
        profile.setDescription(this.description);
        profile.setAvatar(this.avatar);
        profile.setSource(this.source);
        profile.setAllowedTools(this.allowedTools);
        profile.setTraits(this.traits);
        profile.setTraitConfigs(this.traitConfigs);
        profile.setSkills(this.skills);
        profile.setPromptTemplate(this.promptTemplate);
        profile.setDefaultTools(this.defaultTools);
        profile.setIsRunning(this.isRunning);
        profile.setDeployedCount(this.deployedCount);
        profile.setMindConfig(this.mindConfig);
        profile.setStatus(this.status != null ? CharacterProfile.Status.valueOf(this.status) : null);
        profile.setCreatedAt(this.createdAt);
        profile.setUpdatedAt(this.updatedAt);

        // Parse extensions JSON string back to Map
        if (this.extensions != null && !this.extensions.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> extMap = mapper.readValue(this.extensions, mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                profile.setExtensions(extMap);
            } catch (Exception e) {
                profile.setExtensions(Map.of());
            }
        }

        Character character = new Character();
        character.setProfile(profile);
        return character;
    }

    /**
     * 从Character创建Entity
     */
    public static CharacterEntity fromCharacter(Character character) {
        CharacterProfile profile = character.getProfile();
        if (profile == null) {
            profile = new CharacterProfile();
        }

        String extensionsJson = null;
        if (profile.getExtensions() != null && !profile.getExtensions().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                extensionsJson = mapper.writeValueAsString(profile.getExtensions());
            } catch (Exception e) {
                extensionsJson = "{}";
            }
        }

        return CharacterEntity.builder()
                .id(profile.getId())
                .workspaceIds(profile.getWorkspaceIds())
                .name(profile.getName())
                .version(profile.getVersion())
                .description(profile.getDescription())
                .avatar(profile.getAvatar())
                .source(profile.getSource())
                .allowedTools(profile.getAllowedTools())
                .traits(profile.getTraits())
                .traitConfigs(profile.getTraitConfigs())
                .skills(profile.getSkills())
                .promptTemplate(profile.getPromptTemplate())
                .defaultTools(profile.getDefaultTools())
                .isRunning(profile.getIsRunning())
                .deployedCount(profile.getDeployedCount())
                .mindConfig(profile.getMindConfig())
                .extensions(extensionsJson)
                .status(profile.getStatus() != null ? profile.getStatus().name() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}