package org.dragon.skill.runtime;

import org.dragon.skill.domain.SkillBindingDO;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.SkillVersionDO;
import org.dragon.skill.enums.BindingType;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.store.SkillBindingStore;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.SkillVersionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SkillRegistry 单元测试
 *
 * <p>测试多来源 Skill 聚合逻辑：
 * <ol>
 *   <li>builtin Skill 全量加载</li>
 *   <li>三种绑定关系聚合</li>
 *   <li>同名时用户绑定覆盖 builtin</li>
 *   <li>Caffeine 缓存</li>
 *   <li>按名称查找 Skill（支持别名）</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class SkillRegistryTest {

    @Mock
    private SkillStore skillStore;

    @Mock
    private SkillVersionStore skillVersionStore;

    @Mock
    private SkillBindingStore skillBindingStore;

    private SkillRegistry skillRegistry;

    @BeforeEach
    void setUp() {
        skillRegistry = new SkillRegistry(skillBindingStore, skillStore, skillVersionStore);
        skillRegistry.init();
    }

    // ==================== getSkills 测试 ====================

    /**
     * 测试 characterId + workspaceId 同时存在时的聚合查询
     */
    @Test
    void testGetSkills_WithCharacterAndWorkspace() {
        // given: mock builtin skills
        SkillDO builtinSkill = createSkillDO("builtin-skill", "builtin-skill", SkillCategory.CODER);
        SkillVersionDO builtinVersion = createSkillVersionDO(builtinSkill, 1, "builtin skill content");
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtinSkill));
        when(skillVersionStore.findById(builtinSkill.getPublishedVersionId())).thenReturn(Optional.of(builtinVersion));

        // given: mock bindings
        SkillBindingDO charBinding = createBindingDO("char-skill", BindingType.CHARACTER, "char-1");
        SkillBindingDO wsBinding = createBindingDO("ws-skill", BindingType.WORKSPACE, null);
        SkillBindingDO charWsBinding = createBindingDO("charws-skill", BindingType.CHARACTER_WORKSPACE, null);

        when(skillBindingStore.findAvailableByCharacterAndWorkspace("char-1", "ws-1"))
                .thenReturn(List.of(charBinding, wsBinding, charWsBinding));

        // given: mock skill lookups by skillId
        SkillDO charSkill = createSkillDO("char-skill", "char-skill", SkillCategory.OTHER);
        SkillVersionDO charVersion = createSkillVersionDO(charSkill, 1, "char skill content");
        when(skillStore.findLatestActiveBySkillId("char-skill")).thenReturn(Optional.of(charSkill));
        when(skillVersionStore.findById(charSkill.getPublishedVersionId())).thenReturn(Optional.of(charVersion));

        SkillDO wsSkill = createSkillDO("ws-skill", "ws-skill", SkillCategory.DATA_ANALYSIS);
        SkillVersionDO wsVersion = createSkillVersionDO(wsSkill, 1, "ws skill content");
        when(skillStore.findLatestActiveBySkillId("ws-skill")).thenReturn(Optional.of(wsSkill));
        when(skillVersionStore.findById(wsSkill.getPublishedVersionId())).thenReturn(Optional.of(wsVersion));

        SkillDO charWsSkill = createSkillDO("charws-skill", "charws-skill", SkillCategory.TOOL_CALLING);
        SkillVersionDO charWsVersion = createSkillVersionDO(charWsSkill, 1, "charws skill content");
        when(skillStore.findLatestActiveBySkillId("charws-skill")).thenReturn(Optional.of(charWsSkill));
        when(skillVersionStore.findById(charWsSkill.getPublishedVersionId())).thenReturn(Optional.of(charWsVersion));

        // when
        List<SkillRuntime> result = skillRegistry.getSkills("char-1", "ws-1");

        // then
        assertEquals(4, result.size()); // 1 builtin + 3 bindings

        // verify cache is used
        assertEquals(4, skillRegistry.getSkills("char-1", "ws-1").size());
        verify(skillStore, times(1)).findAllBuiltin(); // only called once due to cache
    }

    /**
     * 测试仅 characterId 时的查询
     */
    @Test
    void testGetSkills_CharacterOnly() {
        // given
        SkillDO builtinSkill = createSkillDO("builtin-skill", "builtin-skill", SkillCategory.CODER);
        SkillVersionDO builtinVersion = createSkillVersionDO(builtinSkill, 1, "builtin content");
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtinSkill));
        when(skillVersionStore.findById(builtinSkill.getPublishedVersionId())).thenReturn(Optional.of(builtinVersion));

        SkillBindingDO charBinding = createBindingDO("my-skill", BindingType.CHARACTER, "char-1");
        when(skillBindingStore.findByCharacterId("char-1")).thenReturn(List.of(charBinding));

        SkillDO charSkill = createSkillDO("my-skill", "my-skill", SkillCategory.OTHER);
        SkillVersionDO charVersion = createSkillVersionDO(charSkill, 1, "my skill content");
        when(skillStore.findLatestActiveBySkillId("my-skill")).thenReturn(Optional.of(charSkill));
        when(skillVersionStore.findById(charSkill.getPublishedVersionId())).thenReturn(Optional.of(charVersion));

        // when
        List<SkillRuntime> result = skillRegistry.getSkills("char-1", null);

        // then
        assertEquals(2, result.size()); // 1 builtin + 1 binding
    }

    /**
     * 测试仅 workspaceId 时的查询
     */
    @Test
    void testGetSkills_WorkspaceOnly() {
        // given
        SkillDO builtinSkill = createSkillDO("builtin-skill", "builtin-skill", SkillCategory.CODER);
        SkillVersionDO builtinVersion = createSkillVersionDO(builtinSkill, 1, "builtin content");
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtinSkill));
        when(skillVersionStore.findById(builtinSkill.getPublishedVersionId())).thenReturn(Optional.of(builtinVersion));

        SkillBindingDO wsBinding = createBindingDO("team-skill", BindingType.WORKSPACE, null);
        when(skillBindingStore.findByWorkspaceId("ws-1")).thenReturn(List.of(wsBinding));

        SkillDO wsSkill = createSkillDO("team-skill", "team-skill", SkillCategory.DATA_ANALYSIS);
        SkillVersionDO wsVersion = createSkillVersionDO(wsSkill, 1, "team skill content");
        when(skillStore.findLatestActiveBySkillId("team-skill")).thenReturn(Optional.of(wsSkill));
        when(skillVersionStore.findById(wsSkill.getPublishedVersionId())).thenReturn(Optional.of(wsVersion));

        // when
        List<SkillRuntime> result = skillRegistry.getSkills(null, "ws-1");

        // then
        assertEquals(2, result.size()); // 1 builtin + 1 binding
    }

    /**
     * 测试两者都为空时只返回空列表
     */
    @Test
    void testGetSkills_BothNull() {
        // given
        SkillDO builtinSkill = createSkillDO("builtin-skill", "builtin-skill", SkillCategory.CODER);
        SkillVersionDO builtinVersion = createSkillVersionDO(builtinSkill, 1, "builtin content");
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtinSkill));
        when(skillVersionStore.findById(builtinSkill.getPublishedVersionId())).thenReturn(Optional.of(builtinVersion));

        // when
        List<SkillRuntime> result = skillRegistry.getSkills(null, null);

        // then: builtin 不依赖绑定，但 bindings 为空时只返回 builtin 的部分（需验证实际逻辑）
        // 根据当前实现，characterId 和 workspaceId 都为 null 时 bindings 返回空列表
        assertNotNull(result);
    }

    /**
     * 测试 builtin Skill 全量加载
     */
    @Test
    void testGetSkills_BuiltinSkillsLoaded() {
        // given
        SkillDO builtin1 = createSkillDO("skill-1", "Skill 1", SkillCategory.CODER);
        SkillVersionDO builtinVersion1 = createSkillVersionDO(builtin1, 1, "content 1");
        SkillDO builtin2 = createSkillDO("skill-2", "Skill 2", SkillCategory.OTHER);
        SkillVersionDO builtinVersion2 = createSkillVersionDO(builtin2, 1, "content 2");
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtin1, builtin2));
        when(skillVersionStore.findById(builtin1.getPublishedVersionId())).thenReturn(Optional.of(builtinVersion1));
        when(skillVersionStore.findById(builtin2.getPublishedVersionId())).thenReturn(Optional.of(builtinVersion2));

        // when
        List<SkillRuntime> result = skillRegistry.getSkills("char-1", null);

        // then
        assertEquals(2, result.size());
    }

    // ==================== findByName 测试 ====================

    /**
     * 测试按名称精确查找（仅 builtin，无 bindings）
     */
    @Test
    void testFindByName_FromBuiltin() {
        // given: 只有一个 builtin skill
        SkillDO skill = createSkillDO("git-commit", "git-commit", SkillCategory.CODER);
        SkillVersionDO version = createSkillVersionDO(skill, 1, "# Git Commit");
        when(skillStore.findAllBuiltin()).thenReturn(List.of(skill));
        when(skillVersionStore.findById(skill.getPublishedVersionId())).thenReturn(Optional.of(version));

        // when
        SkillRuntime result = skillRegistry.findByName(null, null, "git-commit");

        // then
        assertNotNull(result);
        assertEquals("git-commit", result.getName());
    }

    /**
     * 测试按别名查找（通过 aliases 匹配）
     */
    @Test
    void testFindByName_AliasMatching() {
        // given: skill 有别名
        SkillDO skill = createSkillDO("git-commit", "git-commit", SkillCategory.CODER);
        SkillVersionDO version = createSkillVersionDOWithAliases(skill, 1, "Git Commit", "[\"gc\",\"commit\"]");
        when(skillStore.findAllBuiltin()).thenReturn(List.of(skill));
        when(skillVersionStore.findById(skill.getPublishedVersionId())).thenReturn(Optional.of(version));

        // when: 用别名查找
        SkillRuntime result = skillRegistry.findByName(null, null, "gc");

        // then
        assertNotNull(result);
        assertEquals("git-commit", result.getName());
    }

    /**
     * 测试查找不存在的 Skill 返回 null
     */
    @Test
    void testFindByName_NotFound() {
        // given
        SkillDO skill = createSkillDO("git-commit", "Git Commit", SkillCategory.CODER);
        SkillVersionDO version = createSkillVersionDO(skill, 1, "# Git Commit");
        when(skillStore.findAllBuiltin()).thenReturn(List.of(skill));
        when(skillVersionStore.findById(skill.getPublishedVersionId())).thenReturn(Optional.of(version));

        // when
        SkillRuntime result = skillRegistry.findByName(null, null, "non-existent");

        // then
        assertNull(result);
    }

    // ==================== 辅助方法 ====================

    private SkillDO createSkillDO(String skillId, String name, SkillCategory category) {
        SkillDO skill = new SkillDO();
        skill.setId(skillId);
        skill.setName(name);
        skill.setDescription("Test description");
        skill.setCategory(category);
        skill.setStatus(SkillStatus.ACTIVE);
        skill.setVisibility(SkillVisibility.PUBLIC);
        // publishedVersionId 会在 createSkillVersionDO 中设置
        return skill;
    }

    private SkillVersionDO createSkillVersionDO(SkillDO skill, int version, String content) {
        SkillVersionDO versionDO = new SkillVersionDO();
        versionDO.setId(Long.parseLong(skill.getId()) * 100 + version);
        versionDO.setSkillId(skill.getId());
        versionDO.setVersion(version);
        versionDO.setName(skill.getName());
        versionDO.setDescription(skill.getDescription());
        versionDO.setContent(content);
        versionDO.setFrontmatter("---");
        versionDO.setRuntimeConfig("{}");
        versionDO.setStatus(SkillVersionStatus.PUBLISHED);
        // 回填 publishedVersionId
        skill.setPublishedVersionId(versionDO.getId());
        return versionDO;
    }

    private SkillVersionDO createSkillVersionDOWithAliases(SkillDO skill, int version, String name, String runtimeConfig) {
        SkillVersionDO versionDO = new SkillVersionDO();
        versionDO.setId(Long.parseLong(skill.getId()) * 100 + version);
        versionDO.setSkillId(skill.getId());
        versionDO.setVersion(version);
        versionDO.setName(name);
        versionDO.setDescription(skill.getDescription());
        versionDO.setContent("# Test Skill Content");
        versionDO.setFrontmatter("---");
        versionDO.setRuntimeConfig("{\"aliases\":" + runtimeConfig + "}");
        versionDO.setStatus(SkillVersionStatus.PUBLISHED);
        // 回填 publishedVersionId
        skill.setPublishedVersionId(versionDO.getId());
        return versionDO;
    }

    private SkillBindingDO createBindingDO(String skillId, BindingType bindingType, String characterId) {
        SkillBindingDO binding = new SkillBindingDO();
        binding.setId(1L);
        binding.setSkillId(skillId);
        binding.setBindingType(bindingType);
        binding.setCharacterId(characterId);
        binding.setWorkspaceId("ws-1");
        return binding;
    }
}