package org.dragon.material;

import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * 物料解析内容内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryMaterialContentStore implements MaterialContentStore {

    private final Map<String, ParsedMaterialContent> store = new ConcurrentHashMap<>();

    @Override
    public void saveParsedContent(ParsedMaterialContent content) {
        store.put(content.getId(), content);
        log.debug("[MemoryMaterialContentStore] Saved parsed content: {}", content.getId());
    }

    @Override
    public Optional<ParsedMaterialContent> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<ParsedMaterialContent> findByMaterialId(String materialId) {
        return store.values().stream()
                .filter(c -> materialId.equals(c.getMaterialId()))
                .reduce((first, second) -> second);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
        log.debug("[MemoryMaterialContentStore] Deleted parsed content: {}", id);
    }

    @Override
    public void update(ParsedMaterialContent content) {
        store.put(content.getId(), content);
        log.debug("[MemoryMaterialContentStore] Updated parsed content: {}", content.getId());
    }
}
