package org.dragon.character.mind.memory;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * MemoryAccess 默认实现（存根）
 * 实际生产环境需要接入向量数据库
 *
 * @author wyj
 * @version 1.0
 */
@Component
@Primary
public class DefaultMemoryAccess implements MemoryAccess {

    private String characterId;

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    @Override
    public String getCharacterId() {
        return characterId;
    }

    @Override
    public List<Memory> semanticSearch(String query, int topK) {
        // TODO: 实现基于向量数据库的语义检索
        // 当前返回空结果
        return Collections.emptyList();
    }

    @Override
    public List<Memory> getByType(Memory.MemoryType type, LocalDateTime from, LocalDateTime to) {
        return Collections.emptyList();
    }

    @Override
    public void store(Memory memory) {
        // TODO: 实现存储逻辑
    }

    @Override
    public void storeBatch(List<Memory> memories) {
        // TODO: 实现批量存储
    }

    @Override
    public Memory get(String memoryId) {
        return null;
    }

    @Override
    public void delete(String memoryId) {
        // TODO: 实现删除逻辑
    }

    @Override
    public void clear() {
        // TODO: 实现清空逻辑
    }

    @Override
    public int count() {
        return 0;
    }
}
