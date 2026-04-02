package org.dragon.memv2.web;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.MemoryQuery;
import org.dragon.memv2.core.MemorySearchResult;
import org.dragon.memv2.core.MemoryFacade;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 记忆管理控制器
 * 提供HTTP接口，用于管理角色和工作空间记忆
 *
 * @author binarytom
 * @version 1.0
 */
@RestController
@RequestMapping("/api/memories")
public class MemoryController {
    private final MemoryFacade memoryFacade;

    public MemoryController(MemoryFacade memoryFacade) {
        this.memoryFacade = memoryFacade;
    }

    @PostMapping("/characters/{characterId}")
    public MemoryEntry saveCharacterMemory(@PathVariable String characterId,
                                           @RequestBody MemoryEntry entry) {
        return memoryFacade.saveCharacterMemory(characterId, entry);
    }

    @PostMapping("/workspaces/{workspaceId}")
    public MemoryEntry saveWorkspaceMemory(@PathVariable String workspaceId,
                                           @RequestBody MemoryEntry entry) {
        return memoryFacade.saveWorkspaceMemory(workspaceId, entry);
    }

    @PostMapping("/recall")
    public List<MemorySearchResult> recall(@RequestBody MemoryQuery query) {
        return memoryFacade.recall(query);
    }
}
