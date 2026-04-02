package org.dragon.memv2.web;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.SessionSnapshot;
import org.dragon.memv2.core.MemoryFacade;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话记忆管理控制器
 * 提供HTTP接口，用于管理会话记忆
 *
 * @author wyj
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
    public SessionSnapshot updateSession(@PathVariable String sessionId,
                                         @RequestBody SessionSnapshot snapshot) {
        return memoryFacade.updateSession(sessionId, snapshot);
    }

    @PostMapping("/{sessionId}/flush")
    public List<MemoryEntry> flushSessionToLongTerm(@PathVariable String sessionId) {
        return memoryFacade.flushSessionToLongTerm(sessionId);
    }
}
