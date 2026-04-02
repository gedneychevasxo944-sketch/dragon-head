package org.dragon.memv2.web;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.SessionSnapshot;
import org.dragon.memv2.core.MemoryFacade;
import org.dragon.memv2.web.dto.MemoryConverter;
import org.dragon.memv2.web.dto.MemoryEntryDTO;
import org.dragon.memv2.web.dto.SessionSnapshotDTO;
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
public class SessionController {
    private final MemoryFacade memoryFacade;

    public SessionController(MemoryFacade memoryFacade) {
        this.memoryFacade = memoryFacade;
    }

    @PostMapping("/{sessionId}/update")
    public SessionSnapshotDTO updateSession(@PathVariable String sessionId,
                                            @RequestBody SessionSnapshotDTO snapshotDto) {
        SessionSnapshot snapshot = MemoryConverter.toEntity(snapshotDto);
        SessionSnapshot updatedSnapshot = memoryFacade.updateSession(sessionId, snapshot);
        return MemoryConverter.toDto(updatedSnapshot);
    }

    @PostMapping("/{sessionId}/flush")
    public List<MemoryEntryDTO> flushSessionToLongTerm(@PathVariable String sessionId) {
        List<MemoryEntry> entries = memoryFacade.flushSessionToLongTerm(sessionId);
        return entries.stream()
                .map(MemoryConverter::toDto)
                .collect(Collectors.toList());
    }
}
