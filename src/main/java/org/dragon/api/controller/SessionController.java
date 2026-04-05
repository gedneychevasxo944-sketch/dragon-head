package org.dragon.api.controller;

import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.core.SessionSnapshot;
import org.dragon.memory.core.MemoryFacade;
import org.dragon.api.controller.dto.memory.MemoryConverter;
import org.dragon.api.controller.dto.memory.MemoryEntryDTO;
import org.dragon.api.controller.dto.memory.SessionSnapshotDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话记忆管理控制器
 * 提供HTTP接口，用于管理会话记忆
 *
 * @author binarytom
 * @version 1.0
 */
@RestController
@RequestMapping("/api/sessions")
@PreAuthorize("isAuthenticated()")
public class SessionController {
    private final MemoryFacade memoryFacade;

    public SessionController(MemoryFacade memoryFacade) {
        this.memoryFacade = memoryFacade;
    }

    @PostMapping("/{sessionId}/start")
    public SessionSnapshotDTO startSession(@PathVariable String sessionId,
                                           @RequestParam String workspaceId,
                                           @RequestParam String characterId) {
        SessionSnapshot snapshot = memoryFacade.startSession(sessionId, workspaceId, characterId);
        return MemoryConverter.toDto(snapshot);
    }

    @GetMapping("/{sessionId}")
    public SessionSnapshotDTO getSession(@PathVariable String sessionId) {
        SessionSnapshot snapshot = memoryFacade.getSession(sessionId);
        return MemoryConverter.toDto(snapshot);
    }

    @PostMapping("/{sessionId}/update")
    public SessionSnapshotDTO updateSession(@PathVariable String sessionId,
                                            @RequestBody SessionSnapshotDTO snapshotDto) {
        SessionSnapshot snapshot = MemoryConverter.toEntity(snapshotDto);
        SessionSnapshot updatedSnapshot = memoryFacade.updateSession(sessionId, snapshot);
        return MemoryConverter.toDto(updatedSnapshot);
    }

    @PostMapping("/{sessionId}/checkpoint")
    public void checkpointSession(@PathVariable String sessionId) {
        memoryFacade.checkpointSession(sessionId);
    }

    @PostMapping("/{sessionId}/flush")
    public List<MemoryEntryDTO> flushSessionToLongTerm(@PathVariable String sessionId) {
        List<MemoryEntry> entries = memoryFacade.flushSessionToLongTerm(sessionId);
        return entries.stream()
                .map(MemoryConverter::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/{sessionId}/close")
    public void closeSession(@PathVariable String sessionId) {
        memoryFacade.closeSession(sessionId);
    }
}
