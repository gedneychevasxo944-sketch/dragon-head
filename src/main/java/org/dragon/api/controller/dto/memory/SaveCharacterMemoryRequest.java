package org.dragon.api.controller.dto.memory;

/**
 * 保存角色记忆请求DTO
 *
 * @author binarytom
 * @version 1.0
 */
public class SaveCharacterMemoryRequest {
    private String characterId;
    private MemoryEntryDTO memoryEntry;

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
    }

    public MemoryEntryDTO getMemoryEntry() {
        return memoryEntry;
    }

    public void setMemoryEntry(MemoryEntryDTO memoryEntry) {
        this.memoryEntry = memoryEntry;
    }
}
