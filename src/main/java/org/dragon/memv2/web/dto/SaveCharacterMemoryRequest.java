package org.dragon.memv2.web.dto;

import org.dragon.memv2.core.MemoryEntry;

/**
 * 保存角色记忆请求DTO
 *
 * @author binarytom
 * @version 1.0
 */
public class SaveCharacterMemoryRequest {
    private String characterId;
    private MemoryEntry memoryEntry;

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public MemoryEntry getMemoryEntry() {
        return memoryEntry;
    }

    public void setMemoryEntry(MemoryEntry memoryEntry) {
        this.memoryEntry = memoryEntry;
    }
}
