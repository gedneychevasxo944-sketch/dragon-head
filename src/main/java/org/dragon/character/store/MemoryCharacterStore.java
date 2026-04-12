package org.dragon.character.store;

import org.dragon.character.Character;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryCharacterStore Character内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryCharacterStore implements CharacterStore {

    private final Map<String, Character> store = new ConcurrentHashMap<>();

    @Override
    public void save(Character character) {
        if (character == null || character.getId() == null) {
            throw new IllegalArgumentException("Character or Character id cannot be null");
        }
        store.put(character.getId(), character);
    }

    @Override
    public void update(Character character) {
        if (character == null || character.getId() == null) {
            throw new IllegalArgumentException("Character or Character id cannot be null");
        }
        if (!store.containsKey(character.getId())) {
            throw new IllegalArgumentException("Character not found: " + character.getId());
        }
        store.put(character.getId(), character);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public Optional<Character> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Character> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(store::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Character> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Character> findByStatus(CharacterProfile.Status status) {
        return store.values().stream()
                .filter(c -> c.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }

    @Override
    public int count() {
        return store.size();
    }
}