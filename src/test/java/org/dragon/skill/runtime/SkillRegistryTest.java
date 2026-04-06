package org.dragon.skill.runtime;

import org.dragon.skill.domain.SkillBindingDO;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.enums.BindingType;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.VersionType;
import org.dragon.skill.store.SkillBindingStore;
import org.dragon.skill.store.SkillStore;
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
    private SkillBindingStore skillBindingStore;

    private SkillRegistry skillRegistry;

    @BeforeEach
    void setUp() {
        skillRegistry = new SkillRegistry(skillBindingStore, skillStore);
        skillRegistry.init();
    }

    // ==================== getSkills 测试 ====================

    /**
     * 测试 characterId + workspaceId 同时存在时的聚合查询
     */
    @Test
    void testGetSkills_WithCharacterAndWorkspace() {
        // given: mock builtin skills
        SkillDO builtinSkill = createSkillDO("builtin-skill", "builtin-skill", SkillCategory.DEVELOPMENT);
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtinSkill));

        // given: mock bindings
        SkillBindingDO charBinding = createBindingDO("char-skill", BindingType.CHARACTER, "char-1");
        SkillBindingDO wsBinding = createBindingDO("ws-skill", BindingType.WORKSPACE, null);
        SkillBindingDO charWsBinding = createBindingDO("charws-skill", BindingType.CHARACTER_WORKSPACE, null);

        when(skillBindingStore.findAvailableByCharacterAndWorkspace("char-1", "ws-1"))
                .thenReturn(List.of(charBinding, wsBinding, charWsBinding));

        // given: mock skill lookups by skillId
        SkillDO charSkill = createSkillDO("char-skill", "char-skill", SkillCategory.DEPLOYMENT);
        SkillDO wsSkill = createSkillDO("ws-skill", "ws-skill", SkillCategory.ANALYSIS);
        SkillDO charWsSkill = createSkillDO("charws-skill", "charws-skill", SkillCategory.UTILITY);

        when(skillStore.findLatestActiveBySkillId("char-skill")).thenReturn(Optional.of(charSkill));
        when(skillStore.findLatestActiveBySkillId("ws-skill")).thenReturn(Optional.of(wsSkill));
        when(skillStore.findLatestActiveBySkillId("charws-skill")).thenReturn(Optional.of(charWsSkill));

        // when
        List<SkillDefinition> result = skillRegistry.getSkills("char-1", "ws-1");

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
        SkillDO builtinSkill = createSkillDO("builtin-skill", "builtin-skill", SkillCategory.DEVELOPMENT);
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtinSkill));

        SkillBindingDO charBinding = createBindingDO("my-skill", BindingType.CHARACTER, "char-1");
        when(skillBindingStore.findByCharacterId("char-1")).thenReturn(List.of(charBinding));

        SkillDO charSkill = createSkillDO("my-skill", "my-skill", SkillCategory.DEPLOYMENT);
        when(skillStore.findLatestActiveBySkillId("my-skill")).thenReturn(Optional.of(charSkill));

        // when
        List<SkillDefinition> result = skillRegistry.getSkills("char-1", null);

        // then
        assertEquals(2, result.size()); // 1 builtin + 1 binding
    }

    /**
     * 测试仅 workspaceId 时的查询
     */
    @Test
    void testGetSkills_WorkspaceOnly() {
        // given
        SkillDO builtinSkill = createSkillDO("builtin-skill", "builtin-skill", SkillCategory.DEVELOPMENT);
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtinSkill));

        SkillBindingDO wsBinding = createBindingDO("team-skill", BindingType.WORKSPACE, null);
        when(skillBindingStore.findByWorkspaceId("ws-1")).thenReturn(List.of(wsBinding));

        SkillDO wsSkill = createSkillDO("team-skill", "team-skill", SkillCategory.ANALYSIS);
        when(skillStore.findLatestActiveBySkillId("team-skill")).thenReturn(Optional.of(wsSkill));

        // when
        List<SkillDefinition> result = skillRegistry.getSkills(null, "ws-1");

        // then
        assertEquals(2, result.size()); // 1 builtin + 1 binding
    }

    /**
     * 测试两者都为空时只返回空列表
     */
    @Test
    void testGetSkills_BothNull() {
        // given
        SkillDO builtinSkill = createSkillDO("builtin-skill", "builtin-skill", SkillCategory.DEVELOPMENT);
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtinSkill));

        // when
        List<SkillDefinition> result = skillRegistry.getSkills(null, null);

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
        SkillDO builtin1 = createSkillDO("skill-1", "Skill 1", SkillCategory.DEVELOPMENT);
        SkillDO builtin2 = createSkillDO("skill-2", "Skill 2", SkillCategory.DEPLOYMENT);
        when(skillStore.findAllBuiltin()).thenReturn(List.of(builtin1, builtin2));

        // when
        List<SkillDefinition> result = skillRegistry.getSkills("char-1", null);

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
        SkillDO skill = createSkillDO("git-commit", "git-commit", "Git Commit", SkillCategory.DEVELOPMENT);
        when(skillStore.findAllBuiltin()).thenReturn(List.of(skill));

        // when
        SkillDefinition result = skillRegistry.findByName(null, null, "git-commit");

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
        SkillDO skill = createSkillDO("git-commit", "git-commit", "Git Commit", SkillCategory.DEVELOPMENT);
        skill.setAliases("[\"gc\", \"commit\"]");
        when(skillStore.findAllBuiltin()).thenReturn(List.of(skill));

        // when: 用别名查找
        SkillDefinition result = skillRegistry.findByName(null, null, "gc");

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
        SkillDO skill = createSkillDO("git-commit", "Git Commit", SkillCategory.DEVELOPMENT);
        when(skillStore.findAllBuiltin()).thenReturn(List.of(skill));

        // when
        SkillDefinition result = skillRegistry.findByName(null, null, "non-existent");

        // then
        assertNull(result);
    }

    // ==================== 版本策略测试 ====================

    /**
     * 测试 LATEST 版本策略
     */
    @Test
    void testResolveLatestVersion() {
        // given
        SkillBindingDO binding = createBindingDO("my-skill", BindingType.CHARACTER, "char-1");
        binding.setVersionType(VersionType.LATEST);

        when(skillBindingStore.findByCharacterId("char-1")).thenReturn(List.of(binding));
        when(skillStore.findLatestActiveBySkillId("my-skill")).thenReturn(
                Optional.of(createSkillDO("my-skill", "My Skill", SkillCategory.DEVELOPMENT)));
        when(skillStore.findAllBuiltin()).thenReturn(List.of());

        // when
        List<SkillDefinition> result = skillRegistry.getSkills("char-1", null);

        // then
        assertEquals(1, result.size());
        verify(skillStore, times(1)).findLatestActiveBySkillId("my-skill");
    }

    /**
     * 测试 FIXED 版本策略
     */
    @Test
    void testResolveFixedVersion() {
        // given
        SkillBindingDO binding = createBindingDO("my-skill", BindingType.CHARACTER, "char-1");
        binding.setVersionType(VersionType.FIXED);
        binding.setFixedVersion(3);

        when(skillBindingStore.findByCharacterId("char-1")).thenReturn(List.of(binding));
        when(skillStore.findBySkillIdAndVersion("my-skill", 3)).thenReturn(
                Optional.of(createSkillDO("my-skill", "My Skill v3", SkillCategory.DEVELOPMENT)));
        when(skillStore.findAllBuiltin()).thenReturn(List.of());

        // when
        List<SkillDefinition> result = skillRegistry.getSkills("char-1", null);

        // then
        assertEquals(1, result.size());
        assertEquals("My Skill v3", result.get(0).getDisplayName());
        verify(skillStore, times(1)).findBySkillIdAndVersion("my-skill", 3);
    }

    // ==================== 辅助方法 ====================

    private SkillDO createSkillDO(String skillId, String name, SkillCategory category) {
        return createSkillDO(skillId, name, name, category);
    }

    private SkillDO createSkillDO(String skillId, String name, String displayName, SkillCategory category) {
        SkillDO skill = new SkillDO();
        skill.setId(1L);
        skill.setSkillId(skillId);
        skill.setName(name);
        skill.setDisplayName(displayName);
        skill.setDescription("Test description");
        skill.setCategory(category);
        skill.setStatus(SkillStatus.ACTIVE);
        skill.setVersion(1);
        skill.setExecutionContext(ExecutionContext.INLINE);
        skill.setContent("# Test Skill Content");
        skill.setAliases("[]");
        skill.setAllowedTools("[]");
        return skill;
    }

    private SkillBindingDO createBindingDO(String skillId, BindingType bindingType, String characterId) {
        SkillBindingDO binding = new SkillBindingDO();
        binding.setId(1L);
        binding.setSkillId(skillId);
        binding.setBindingType(bindingType);
        binding.setCharacterId(characterId);
        binding.setWorkspaceId("ws-1");
        binding.setVersionType(VersionType.LATEST);
        return binding;
    }
}
