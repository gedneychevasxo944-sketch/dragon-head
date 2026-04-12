package org.dragon.character.store;

import org.dragon.character.Character;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * CharacterStore Character存储接口
 */
public interface CharacterStore extends Store {

    /**
     * 保存Character
     */
    void save(Character character);

    /**
     * 更新Character
     */
    void update(Character character);

    /**
     * 删除Character
     */
    void delete(String id);

    /**
     * 根据ID获取Character
     */
    Optional<Character> findById(String id);

    /**
     * 批量查找
     */
    List<Character> findByIds(List<String> ids);

    /**
     * 获取所有Character
     */
    List<Character> findAll();

    /**
     * 根据Workspace ID获取Character列表
     */
    List<Character> findByWorkspaceId(String workspaceId);

    /**
     * 根据状态获取Character列表
     */
    List<Character> findByStatus(CharacterProfile.Status status);

    /**
     * 检查Character是否存在
     */
    boolean exists(String id);

    /**
     * 获取Character数量
     */
    int count();
}