package org.dragon.api.controller;

import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.core.MemoryQuery;
import org.dragon.memory.core.MemorySearchResult;
import org.dragon.memory.core.MemoryFacade;
import org.dragon.memory.core.MemoryId;
import org.dragon.api.controller.dto.*;
import org.dragon.permission.checker.PermissionChecker;
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
    private final PermissionChecker permissionChecker;

    public MemoryController(MemoryFacade memoryFacade, PermissionChecker permissionChecker) {
        this.memoryFacade = memoryFacade;
        this.permissionChecker = permissionChecker;
    }

    @PostMapping("/characters/{characterId}")
    public MemoryEntryDTO saveCharacterMemory(@PathVariable String characterId,
                                              @RequestBody MemoryEntryDTO entryDto) {
        permissionChecker.checkEdit("CHARACTER", characterId);
        MemoryEntry entry = MemoryConverter.toEntity(entryDto);
        MemoryEntry savedEntry = memoryFacade.saveCharacterMemory(characterId, entry);
        return MemoryConverter.toDto(savedEntry);
    }

    @PostMapping("/workspaces/{workspaceId}")
    public MemoryEntryDTO saveWorkspaceMemory(@PathVariable String workspaceId,
                                              @RequestBody MemoryEntryDTO entryDto) {
        permissionChecker.checkEdit("WORKSPACE", workspaceId);
        MemoryEntry entry = MemoryConverter.toEntity(entryDto);
        MemoryEntry savedEntry = memoryFacade.saveWorkspaceMemory(workspaceId, entry);
        return MemoryConverter.toDto(savedEntry);
    }

    @GetMapping("/characters/{characterId}/{memoryId}")
    public MemoryEntryDTO getCharacterMemory(@PathVariable String characterId,
                                             @PathVariable String memoryId) {
        permissionChecker.checkView("CHARACTER", characterId);
        return memoryFacade.getCharacterMemory(characterId, MemoryId.of(memoryId))
                .map(MemoryConverter::toDto)
                .orElse(null);
    }

    @GetMapping("/workspaces/{workspaceId}/{memoryId}")
    public MemoryEntryDTO getWorkspaceMemory(@PathVariable String workspaceId,
                                             @PathVariable String memoryId) {
        permissionChecker.checkView("WORKSPACE", workspaceId);
        return memoryFacade.getWorkspaceMemory(workspaceId, MemoryId.of(memoryId))
                .map(MemoryConverter::toDto)
                .orElse(null);
    }

    @GetMapping("/characters/{characterId}")
    public List<MemoryEntryDTO> listCharacterMemories(@PathVariable String characterId) {
        permissionChecker.checkView("CHARACTER", characterId);
        List<MemoryEntry> entries = memoryFacade.listCharacterMemories(characterId);
        return entries.stream()
                .map(MemoryConverter::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/workspaces/{workspaceId}")
    public List<MemoryEntryDTO> listWorkspaceMemories(@PathVariable String workspaceId) {
        permissionChecker.checkView("WORKSPACE", workspaceId);
        List<MemoryEntry> entries = memoryFacade.listWorkspaceMemories(workspaceId);
        return entries.stream()
                .map(MemoryConverter::toDto)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/characters/{characterId}/{memoryId}")
    public void deleteCharacterMemory(@PathVariable String characterId,
                                      @PathVariable String memoryId) {
        permissionChecker.checkDelete("CHARACTER", characterId);
        memoryFacade.deleteCharacterMemory(characterId, MemoryId.of(memoryId));
    }

    @DeleteMapping("/workspaces/{workspaceId}/{memoryId}")
    public void deleteWorkspaceMemory(@PathVariable String workspaceId,
                                      @PathVariable String memoryId) {
        permissionChecker.checkDelete("WORKSPACE", workspaceId);
        memoryFacade.deleteWorkspaceMemory(workspaceId, MemoryId.of(memoryId));
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
