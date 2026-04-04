package org.dragon.api.controller;

import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.core.MemoryQuery;
import org.dragon.memory.core.MemorySearchResult;
import org.dragon.memory.core.MemoryFacade;
import org.dragon.memory.core.MemoryId;
import org.dragon.api.controller.dto.*;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("isAuthenticated()")
public class MemoryController {
    private final MemoryFacade memoryFacade;

    public MemoryController(MemoryFacade memoryFacade) {
        this.memoryFacade = memoryFacade;
    }

    @PostMapping("/characters/{characterId}")
    @PreAuthorize("canEdit(#characterId, 'CHARACTER')")
    public MemoryEntryDTO saveCharacterMemory(@PathVariable String characterId,
                                              @RequestBody MemoryEntryDTO entryDto) {
        MemoryEntry entry = MemoryConverter.toEntity(entryDto);
        MemoryEntry savedEntry = memoryFacade.saveCharacterMemory(characterId, entry);
        return MemoryConverter.toDto(savedEntry);
    }

    @PostMapping("/workspaces/{workspaceId}")
    @PreAuthorize("canEdit(#workspaceId, 'WORKSPACE')")
    public MemoryEntryDTO saveWorkspaceMemory(@PathVariable String workspaceId,
                                              @RequestBody MemoryEntryDTO entryDto) {
        MemoryEntry entry = MemoryConverter.toEntity(entryDto);
        MemoryEntry savedEntry = memoryFacade.saveWorkspaceMemory(workspaceId, entry);
        return MemoryConverter.toDto(savedEntry);
    }

    @GetMapping("/characters/{characterId}/{memoryId}")
    @PreAuthorize("canView(#characterId, 'CHARACTER')")
    public MemoryEntryDTO getCharacterMemory(@PathVariable String characterId,
                                             @PathVariable String memoryId) {
        return memoryFacade.getCharacterMemory(characterId, MemoryId.of(memoryId))
                .map(MemoryConverter::toDto)
                .orElse(null);
    }

    @GetMapping("/workspaces/{workspaceId}/{memoryId}")
    @PreAuthorize("canView(#workspaceId, 'WORKSPACE')")
    public MemoryEntryDTO getWorkspaceMemory(@PathVariable String workspaceId,
                                             @PathVariable String memoryId) {
        return memoryFacade.getWorkspaceMemory(workspaceId, MemoryId.of(memoryId))
                .map(MemoryConverter::toDto)
                .orElse(null);
    }

    @GetMapping("/characters/{characterId}")
    @PreAuthorize("canView(#characterId, 'CHARACTER')")
    public List<MemoryEntryDTO> listCharacterMemories(@PathVariable String characterId) {
        List<MemoryEntry> entries = memoryFacade.listCharacterMemories(characterId);
        return entries.stream()
                .map(MemoryConverter::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/workspaces/{workspaceId}")
    @PreAuthorize("canView(#workspaceId, 'WORKSPACE')")
    public List<MemoryEntryDTO> listWorkspaceMemories(@PathVariable String workspaceId) {
        List<MemoryEntry> entries = memoryFacade.listWorkspaceMemories(workspaceId);
        return entries.stream()
                .map(MemoryConverter::toDto)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/characters/{characterId}/{memoryId}")
    @PreAuthorize("canDelete(#characterId, 'CHARACTER')")
    public void deleteCharacterMemory(@PathVariable String characterId,
                                      @PathVariable String memoryId) {
        memoryFacade.deleteCharacterMemory(characterId, MemoryId.of(memoryId));
    }

    @DeleteMapping("/workspaces/{workspaceId}/{memoryId}")
    @PreAuthorize("canDelete(#workspaceId, 'WORKSPACE')")
    public void deleteWorkspaceMemory(@PathVariable String workspaceId,
                                      @PathVariable String memoryId) {
        memoryFacade.deleteWorkspaceMemory(workspaceId, MemoryId.of(memoryId));
    }

    @PostMapping("/recall")
    @PreAuthorize("isAuthenticated()")
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
