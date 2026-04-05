package org.dragon.skill.service;

import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillStatusException;
import org.dragon.skill.store.SkillStore;
import org.dragon.util.bean.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SkillLifecycleService 单元测试
 *
 * <p>测试状态流转逻辑：
 * <ol>
 *   <li>publish: draft → active</li>
 *   <li>disable: active → disabled</li>
 *   <li>republish: disabled → active</li>
 *   <li>delete: 任意 → deleted（所有版本）</li>
 *   <li>状态前置条件校验</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class SkillLifecycleServiceTest {

    @Mock
    private SkillStore skillStore;

    private SkillLifecycleService lifecycleService;
    private UserInfo testUser;

    @BeforeEach
    void setUp() {
        lifecycleService = new SkillLifecycleService();
        testUser = new UserInfo("1", "testUser", null, true);
        try {
            var field = SkillLifecycleService.class.getDeclaredField("skillStore");
            field.setAccessible(true);
            field.set(lifecycleService, skillStore);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== publish 测试 ====================

    /**
     * 测试 draft → active 发布成功
     */
    @Test
    void testPublish_DraftToActive() {
        // given
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, 1, SkillStatus.DRAFT);
        when(skillStore.findLatestBySkillId(skillId)).thenReturn(Optional.of(skill));

        // when
        lifecycleService.publish(skillId, testUser);

        // then
        assertEquals(SkillStatus.ACTIVE, skill.getStatus());
        assertNotNull(skill.getPublishedAt());
        verify(skillStore, times(1)).update(skill);
    }

    /**
     * 测试发布时 skill 不存在抛出异常
     */
    @Test
    void testPublish_SkillNotFound() {
        when(skillStore.findLatestBySkillId("non-existent")).thenReturn(Optional.empty());

        assertThrows(SkillNotFoundException.class,
                () -> lifecycleService.publish("non-existent", testUser));
    }

    /**
     * 测试错误状态转换时抛出异常
     */
    @Test
    void testPublish_InvalidStatus() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, 1, SkillStatus.ACTIVE);
        when(skillStore.findLatestBySkillId(skillId)).thenReturn(Optional.of(skill));

        assertThrows(SkillStatusException.class,
                () -> lifecycleService.publish(skillId, testUser));
    }

    // ==================== disable 测试 ====================

    /**
     * 测试 active → disabled 下架成功
     */
    @Test
    void testDisable_ActiveToDisabled() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, 1, SkillStatus.ACTIVE);
        when(skillStore.findLatestBySkillId(skillId)).thenReturn(Optional.of(skill));

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
        SkillDO skill = createSkillDO(skillId, 1, SkillStatus.DRAFT);
        when(skillStore.findLatestBySkillId(skillId)).thenReturn(Optional.of(skill));

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
        SkillDO skill = createSkillDO(skillId, 1, SkillStatus.DISABLED);
        when(skillStore.findLatestBySkillId(skillId)).thenReturn(Optional.of(skill));

        lifecycleService.republish(skillId, testUser);

        assertEquals(SkillStatus.ACTIVE, skill.getStatus());
        assertNotNull(skill.getPublishedAt());
        verify(skillStore, times(1)).update(skill);
    }

    /**
     * 测试重新发布时状态不正确抛出异常
     */
    @Test
    void testRepublish_InvalidStatus() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, 1, SkillStatus.DRAFT);
        when(skillStore.findLatestBySkillId(skillId)).thenReturn(Optional.of(skill));

        assertThrows(SkillStatusException.class,
                () -> lifecycleService.republish(skillId, testUser));
    }

    // ==================== delete 测试 ====================

    /**
     * 测试删除技能将所有版本标记为 deleted
     */
    @Test
    void testDelete_AllVersionsMarked() {
        String skillId = "skill-123";
        SkillDO v1 = createSkillDO(skillId, 1, SkillStatus.ACTIVE);
        SkillDO v2 = createSkillDO(skillId, 2, SkillStatus.ACTIVE);
        when(skillStore.findLatestBySkillId(skillId)).thenReturn(Optional.of(v2));
        when(skillStore.findAllVersionsBySkillId(skillId)).thenReturn(List.of(v1, v2));

        lifecycleService.delete(skillId, testUser);

        assertEquals(SkillStatus.DELETED, v1.getStatus());
        assertEquals(SkillStatus.DELETED, v2.getStatus());
        verify(skillStore, times(2)).update(any());
    }

    /**
     * 测试删除已 deleted 的技能是幂等操作
     */
    @Test
    void testDelete_Idempotent() {
        String skillId = "skill-123";
        SkillDO skill = createSkillDO(skillId, 1, SkillStatus.DELETED);
        when(skillStore.findLatestBySkillId(skillId)).thenReturn(Optional.of(skill));

        // should not throw
        lifecycleService.delete(skillId, testUser);

        // update should not be called since status is already DELETED
        verify(skillStore, never()).update(any());
    }

    /**
     * 测试删除不存在的 skill 抛出异常
     */
    @Test
    void testDelete_NotFound() {
        when(skillStore.findLatestBySkillId("non-existent")).thenReturn(Optional.empty());

        assertThrows(SkillNotFoundException.class,
                () -> lifecycleService.delete("non-existent", testUser));
    }

    // ==================== 辅助方法 ====================

    private SkillDO createSkillDO(String skillId, int version, SkillStatus status) {
        SkillDO skill = new SkillDO();
        skill.setId(1L);
        skill.setSkillId(skillId);
        skill.setName("test-skill");
        skill.setDisplayName("Test Skill");
        skill.setCategory(SkillCategory.DEVELOPMENT);
        skill.setStatus(status);
        skill.setVersion(version);
        skill.setExecutionContext(ExecutionContext.INLINE);
        return skill;
    }
}
