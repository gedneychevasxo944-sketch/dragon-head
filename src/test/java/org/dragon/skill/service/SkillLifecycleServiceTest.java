package org.dragon.skill.service;

import org.dragon.skill.actionlog.SkillActionLogService;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.SkillVersionDO;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillStatusException;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.SkillVersionStore;
import org.dragon.util.bean.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SkillLifecycleService 单元测试
 *
 * <p>测试状态流转逻辑：
 * <ol>
 *   <li>publish: draft version → published (old published → deprecated)</li>
 *   <li>disable: active → disabled</li>
 *   <li>republish: disabled → active</li>
 *   <li>delete: 任意 → soft deleted (deletedAt)</li>
 *   <li>状态前置条件校验</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class SkillLifecycleServiceTest {

    @Mock
    private SkillStore skillStore;

    @Mock
    private SkillVersionStore skillVersionStore;

    @Mock
    private SkillActionLogService actionLogService;

    private SkillLifecycleService lifecycleService;
    private UserInfo testUser;

    @BeforeEach
    void setUp() {
        lifecycleService = new SkillLifecycleService();
        testUser = new UserInfo("1", "testUser", null, true);

        // 使用反射注入依赖
        try {
            var skillStoreField = SkillLifecycleService.class.getDeclaredField("skillStore");
            skillStoreField.setAccessible(true);
            skillStoreField.set(lifecycleService, skillStore);

            var versionStoreField = SkillLifecycleService.class.getDeclaredField("skillVersionStore");
            versionStoreField.setAccessible(true);
            versionStoreField.set(lifecycleService, skillVersionStore);

            var actionLogField = SkillLifecycleService.class.getDeclaredField("actionLogService");
            actionLogField.setAccessible(true);
            actionLogField.set(lifecycleService, actionLogService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== publish 测试 ====================

    /**
     * 测试发布 draft 版本成功
     */
    @Test
    void testPublish_DraftVersionSuccess() {
        // given
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.DRAFT);
        SkillVersionDO draftVersion = createSkillVersionDO(skillId, 1, SkillVersionStatus.DRAFT);

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));
        when(skillVersionStore.findBySkillIdAndVersion(skillId, 1)).thenReturn(Optional.of(draftVersion));
        when(skillVersionStore.findById(any())).thenReturn(Optional.empty());

        // when
        lifecycleService.publish(skillId, 1, null, testUser);

        // then
        assertEquals(SkillVersionStatus.PUBLISHED, draftVersion.getStatus());
        assertNotNull(draftVersion.getPublishedAt());
        assertEquals(skill.getPublishedVersionId(), draftVersion.getId());
        assertEquals(SkillStatus.ACTIVE, skill.getStatus());
        verify(skillVersionStore, times(1)).update(draftVersion);
        verify(skillStore, times(1)).update(skill);
    }

    /**
     * 测试发布时 skill 不存在抛出异常
     */
    @Test
    void testPublish_SkillNotFound() {
        when(skillStore.findBySkillId("non-existent")).thenReturn(Optional.empty());

        assertThrows(SkillNotFoundException.class,
                () -> lifecycleService.publish("non-existent", 1, null, testUser));
    }

    /**
     * 测试发布时版本不存在抛出异常
     */
    @Test
    void testPublish_VersionNotFound() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.DRAFT);

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));
        when(skillVersionStore.findBySkillIdAndVersion(skillId, 99)).thenReturn(Optional.empty());

        assertThrows(SkillNotFoundException.class,
                () -> lifecycleService.publish(skillId, 99, null, testUser));
    }

    /**
     * 测试发布非 draft 版本时抛出异常
     */
    @Test
    void testPublish_InvalidVersionStatus() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.DRAFT);
        SkillVersionDO publishedVersion = createSkillVersionDO(skillId, 1, SkillVersionStatus.PUBLISHED);

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));
        when(skillVersionStore.findBySkillIdAndVersion(skillId, 1)).thenReturn(Optional.of(publishedVersion));

        assertThrows(SkillStatusException.class,
                () -> lifecycleService.publish(skillId, 1, null, testUser));
    }

    /**
     * 测试发布时旧已发布版本标记为 DEPRECATED
     */
    @Test
    void testPublish_OldVersionDeprecated() {
        // given
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.ACTIVE);
        skill.setPublishedVersionId(100L);

        SkillVersionDO oldPublished = createSkillVersionDO(skillId, 1, SkillVersionStatus.PUBLISHED);
        oldPublished.setId(100L);

        SkillVersionDO newDraft = createSkillVersionDO(skillId, 2, SkillVersionStatus.DRAFT);

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));
        when(skillVersionStore.findBySkillIdAndVersion(skillId, 2)).thenReturn(Optional.of(newDraft));
        when(skillVersionStore.findById(100L)).thenReturn(Optional.of(oldPublished));

        // when
        lifecycleService.publish(skillId, 2, null, testUser);

        // then
        assertEquals(SkillVersionStatus.DEPRECATED, oldPublished.getStatus());
        verify(skillVersionStore).update(oldPublished);
    }

    // ==================== disable 测试 ====================

    /**
     * 测试 active → disabled 下架成功
     */
    @Test
    void testDisable_ActiveToDisabled() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.ACTIVE);

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));

        lifecycleService.disable(skillId, testUser);

        assertEquals(SkillStatus.DISABLED, skill.getStatus());
        verify(skillStore, times(1)).update(skill);
    }

    /**
     * 测试下架时状态不正确抛出异常
     */
    @Test
    void testDisable_InvalidStatus() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.DRAFT);

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));

        assertThrows(SkillStatusException.class,
                () -> lifecycleService.disable(skillId, testUser));
    }

    // ==================== republish 测试 ====================

    /**
     * 测试 disabled → active 重新发布成功
     */
    @Test
    void testRepublish_DisabledToActive() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.DISABLED);
        skill.setPublishedVersionId(100L);

        SkillVersionDO publishedVersion = createSkillVersionDO(skillId, 1, SkillVersionStatus.PUBLISHED);
        publishedVersion.setId(100L);

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));
        when(skillVersionStore.findById(100L)).thenReturn(Optional.of(publishedVersion));

        lifecycleService.republish(skillId, testUser);

        assertEquals(SkillStatus.ACTIVE, skill.getStatus());
        verify(skillStore, times(1)).update(skill);
    }

    /**
     * 测试重新发布时状态不正确抛出异常
     */
    @Test
    void testRepublish_InvalidStatus() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.DRAFT);

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));

        assertThrows(SkillStatusException.class,
                () -> lifecycleService.republish(skillId, testUser));
    }

    // ==================== delete 测试 ====================

    /**
     * 测试删除技能标记 deletedAt
     */
    @Test
    void testDelete_SetsDeletedAt() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.ACTIVE);

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));

        lifecycleService.delete(skillId, testUser);

        assertNotNull(skill.getDeletedAt());
        verify(skillStore, times(1)).update(skill);
    }

    /**
     * 测试删除已删除的技能是幂等操作
     */
    @Test
    void testDelete_Idempotent() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, SkillStatus.ACTIVE);
        skill.setDeletedAt(java.time.LocalDateTime.now().minusDays(1));

        when(skillStore.findBySkillId(skillId)).thenReturn(Optional.of(skill));

        // should not throw
        lifecycleService.delete(skillId, testUser);

        // update should not be called since already deleted
        verify(skillStore, never()).update(any());
    }

    /**
     * 测试删除不存在的 skill 抛出异常
     */
    @Test
    void testDelete_NotFound() {
        when(skillStore.findBySkillId("non-existent")).thenReturn(Optional.empty());

        assertThrows(SkillNotFoundException.class,
                () -> lifecycleService.delete("non-existent", testUser));
    }

    // ==================== 辅助方法 ====================

    private SkillDO createSkillDO(String skillId, SkillStatus status) {
        SkillDO skill = new SkillDO();
        skill.setId(skillId);
        skill.setName("test-skill");
        skill.setCategory(SkillCategory.CODER);
        skill.setVisibility(SkillVisibility.PUBLIC);
        skill.setStatus(status);
        return skill;
    }

    private SkillVersionDO createSkillVersionDO(String skillId, int version, SkillVersionStatus status) {
        SkillVersionDO versionDO = new SkillVersionDO();
        versionDO.setId((long) (Math.random() * 10000));
        versionDO.setSkillId(skillId);
        versionDO.setVersion(version);
        versionDO.setName("test-skill");
        versionDO.setDescription("Test description");
        versionDO.setContent("# Test Content");
        versionDO.setStatus(status);
        return versionDO;
    }
}