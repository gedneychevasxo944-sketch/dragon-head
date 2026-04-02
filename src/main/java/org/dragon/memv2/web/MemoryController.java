package org.dragon.memv2.web;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.MemoryQuery;
import org.dragon.memv2.core.MemorySearchResult;
import org.dragon.memv2.core.MemoryFacade;
import org.dragon.memv2.web.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public MemoryEntryDTO saveCharacterMemory(@PathVariable String characterId,
                                              @RequestBody MemoryEntryDTO entryDto) {
        MemoryEntry entry = MemoryConverter.toEntity(entryDto);
        MemoryEntry savedEntry = memoryFacade.saveCharacterMemory(characterId, entry);
        return MemoryConverter.toDto(savedEntry);
    }

    @PostMapping("/workspaces/{workspaceId}")
    public MemoryEntryDTO saveWorkspaceMemory(@PathVariable String workspaceId,
                                              @RequestBody MemoryEntryDTO entryDto) {
        MemoryEntry entry = MemoryConverter.toEntity(entryDto);
        MemoryEntry savedEntry = memoryFacade.saveWorkspaceMemory(workspaceId, entry);
        return MemoryConverter.toDto(savedEntry);
    }

    @PostMapping("/recall")
    public RecallResponse recall(@RequestBody RecallRequest request) {
        MemoryQuery query = MemoryConverter.toEntity(request.getQuery());
        List<MemorySearchResult> results = memoryFacade.recall(query);
        List<MemorySearchResultDTO> resultDtos = results.stream()
                .map(MemoryConverter::toDto)
                .collect(Collectors.toList());
        RecallResponse response = new RecallResponse();
        response.setResults(resultDtos);
        return response;
    }
}
