package org.dragon.memory.core.command;

import org.dragon.memory.core.MemoryEntry;

/**
 * 保存角色记忆命令
 *
 * @author binarytom
 * @version 1.0
 */
public class SaveCharacterMemoryCommand {
    private final String characterId;
    private final MemoryEntry memoryEntry;

    private SaveCharacterMemoryCommand(Builder builder) {
        this.characterId = builder.characterId;
        this.memoryEntry = builder.memoryEntry;
    }

    public String getCharacterId() {
        return characterId;
    }

    public MemoryEntry getMemoryEntry() {
        return memoryEntry;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String characterId;
        private MemoryEntry memoryEntry;

        public Builder characterId(String characterId) {
            this.characterId = characterId;
            return this;
        }

        public Builder memoryEntry(MemoryEntry memoryEntry) {
            this.memoryEntry = memoryEntry;
            return this;
        }

        public SaveCharacterMemoryCommand build() {
            return new SaveCharacterMemoryCommand(this);
        }
    }
}
