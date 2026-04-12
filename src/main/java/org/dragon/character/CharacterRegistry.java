package org.dragon.character;

import org.dragon.character.profile.CharacterProfile;
import org.dragon.character.store.CharacterStore;
import org.dragon.store.StoreFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Character 注册中心
 * 负责管理所有 Character 的生命周期
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class CharacterRegistry {

    private final CharacterStore characterStore;

    /**
     * 默认 Character ID
     */
    private volatile String defaultCharacterId;

    public CharacterRegistry(StoreFactory storeFactory) {
        this.characterStore = storeFactory.get(CharacterStore.class);
    }

    /**
     * 注册 Character
     *
     * @param character Character 实例
     */
    public void register(Character character) {
        if (character == null || character.getId() == null) {
            throw new IllegalArgumentException("Character or Character id cannot be null");
        }

        // 设置创建/更新时间
        if (character.getCreatedAt() == null) {
            character.setCreatedAt(LocalDateTime.now());
        }
        character.setUpdatedAt(LocalDateTime.now());

        // 如果是第一个 Character，设为默认
        if (characterStore.count() == 0) {
            defaultCharacterId = character.getId();
        }

        characterStore.save(character);
        log.info("[CharacterRegistry] Registered character: {}, version: {}",
                character.getId(), character.getVersion());
    }

    /**
     * 注销 Character
     *
     * @param characterId Character ID
     */
    public void unregister(String characterId) {
        if (!characterStore.exists(characterId)) {
            return;
        }

        characterStore.delete(characterId);
        log.info("[CharacterRegistry] Unregistered character: {}", characterId);

        // 如果删除的是默认 Character，选择下一个
        if (defaultCharacterId != null && defaultCharacterId.equals(characterId)) {
            List<Character> all = characterStore.findAll();
            defaultCharacterId = all.isEmpty() ? null : all.get(0).getId();
        }
    }

    /**
     * 获取 Character
     *
     * @param characterId Character ID
     * @return Optional Character
     */
    public Optional<Character> get(String characterId) {
        return characterStore.findById(characterId);
    }

    /**
     * 批量获取 Character
     *
     * @param characterIds Character ID 列表
     * @return Character 列表
     */
    public List<Character> findByIds(List<String> characterIds) {
        return characterStore.findByIds(characterIds);
    }

    /**
     * 获取默认 Character
     *
     * @return Optional Character
     */
    public Optional<Character> getDefaultCharacter() {
        if (defaultCharacterId == null) {
            return Optional.empty();
        }
        return get(defaultCharacterId);
    }

    /**
     * 获取所有 Character
     *
     * @return Character 列表
     */
    public List<Character> listAll() {
        return characterStore.findAll();
    }

    /**
     * 更新 Character
     *
     * @param character Character 实例
     */
    public void update(Character character) {
        if (character == null || character.getId() == null) {
            throw new IllegalArgumentException("Character or Character id cannot be null");
        }

        if (!characterStore.exists(character.getId())) {
            throw new IllegalArgumentException("Character not found: " + character.getId());
        }

        character.setUpdatedAt(LocalDateTime.now());
        characterStore.update(character);
        log.info("[CharacterRegistry] Updated character: {}", character.getId());
    }

    /**
     * 设置默认 Character
     *
     * @param characterId Character ID
     */
    public void setDefaultCharacter(String characterId) {
        if (!characterStore.exists(characterId)) {
            throw new IllegalArgumentException("Character not found: " + characterId);
        }
        defaultCharacterId = characterId;
        log.info("[CharacterRegistry] Set default character: {}", characterId);
    }

    /**
     * 加载 Character（状态变更）
     *
     * @param characterId Character ID
     */
    public void load(String characterId) {
        get(characterId).ifPresent(character -> {
            character.setStatus(CharacterProfile.Status.LOADED);
            character.setUpdatedAt(LocalDateTime.now());
            characterStore.update(character);
            log.info("[CharacterRegistry] Loaded character: {}", characterId);
        });
    }

    /**
     * 启动 Character（状态变更）
     *
     * @param characterId Character ID
     */
    public void start(String characterId) {
        get(characterId).ifPresent(character -> {
            character.setStatus(CharacterProfile.Status.RUNNING);
            character.setUpdatedAt(LocalDateTime.now());
            characterStore.update(character);
            log.info("[CharacterRegistry] Started character: {}", characterId);
        });
    }

    /**
     * 暂停 Character（状态变更）
     *
     * @param characterId Character ID
     */
    public void pause(String characterId) {
        get(characterId).ifPresent(character -> {
            character.setStatus(CharacterProfile.Status.PAUSED);
            character.setUpdatedAt(LocalDateTime.now());
            characterStore.update(character);
            log.info("[CharacterRegistry] Paused character: {}", characterId);
        });
    }

    /**
     * 销毁 Character（状态变更）
     *
     * @param characterId Character ID
     */
    public void destroy(String characterId) {
        get(characterId).ifPresent(character -> {
            character.setStatus(CharacterProfile.Status.DESTROYED);
            character.setUpdatedAt(LocalDateTime.now());
            characterStore.update(character);
            log.info("[CharacterRegistry] Destroyed character: {}", characterId);
        });
    }

    /**
     * 获取注册表大小
     *
     * @return 注册的 Character 数量
     */
    public int size() {
        return characterStore.count();
    }

    /**
     * 检查 Character 是否存在
     *
     * @param characterId Character ID
     * @return 是否存在
     */
    public boolean exists(String characterId) {
        return characterStore.exists(characterId);
    }
}