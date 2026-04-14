package org.dragon.memory;

import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.agent.llm.caller.LLMCallerSelector;
import org.dragon.agent.model.ModelInstance;
import org.dragon.memory.service.core.impl.MemoryRecallServiceImpl;
import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.MemoryQuery;
import org.dragon.memory.constants.MemoryScope;
import org.dragon.memory.entity.MemorySearchResult;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.entity.SessionSnapshot;
import org.dragon.memory.storage.repo.CharacterMemoryRepository;
import org.dragon.memory.storage.repo.SessionMemoryRepository;
import org.dragon.memory.storage.repo.WorkspaceMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DefaultMemoryRecallService 单元测试
 *
 * @author binarytom
 * @version 1.0
 */
public class MemoryRecallServiceTest {

    private CharacterMemoryRepository characterMemoryRepository;
    private WorkspaceMemoryRepository workspaceMemoryRepository;
    private SessionMemoryRepository sessionMemoryRepository;
    private LLMCallerSelector llmCallerSelector;
    private MemoryRecallServiceImpl recallService;

    @BeforeEach
    void setUp() {
        characterMemoryRepository = mock(CharacterMemoryRepository.class);
        workspaceMemoryRepository = mock(WorkspaceMemoryRepository.class);
        sessionMemoryRepository = mock(SessionMemoryRepository.class);

        // 默认 LLM caller 返回空响应，searchEntries 降级为全量分会
        LLMCaller defaultCaller = mock(LLMCaller.class);
        when(defaultCaller.call(any())).thenReturn(LLMResponse.builder().content("").build());
        llmCallerSelector = mock(LLMCallerSelector.class);
        when(llmCallerSelector.getDefault()).thenReturn(defaultCaller);
        when(llmCallerSelector.selectByProvider(any(ModelInstance.ModelProvider.class))).thenReturn(defaultCaller);

        recallService = new MemoryRecallServiceImpl(
                characterMemoryRepository,
                workspaceMemoryRepository,
                sessionMemoryRepository,
                llmCallerSelector);
    }

    // ---- recallCharacter ----

    @Test
    void testRecallCharacterReturnsRankedResults() {
        MemoryEntry entry = entry("char-entry", MemoryType.CHARACTER_PROFILE, MemoryScope.CHARACTER);
        when(characterMemoryRepository.list("char-1")).thenReturn(List.of(entry));

        List<MemorySearchResult> results = recallService.recallCharacter("char-1", null, 10);

        assertEquals(1, results.size());
        assertEquals("char-entry", results.get(0).getMemory().getTitle());
    }

    @Test
    void testRecallCharacterRespectsLimit() {
        List<MemoryEntry> entries = List.of(
                entry("e1", MemoryType.CHARACTER_PROFILE, MemoryScope.CHARACTER),
                entry("e2", MemoryType.CHARACTER_PROFILE, MemoryScope.CHARACTER),
                entry("e3", MemoryType.CHARACTER_PROFILE, MemoryScope.CHARACTER));
        when(characterMemoryRepository.list("char-1")).thenReturn(entries);

        List<MemorySearchResult> results = recallService.recallCharacter("char-1", null, 2);

        assertEquals(2, results.size());
    }

    // ---- recallWorkspace ----

    @Test
    void testRecallWorkspaceReturnsRankedResults() {
        MemoryEntry entry = entry("ws-entry", MemoryType.WORKSPACE_DECISION, MemoryScope.WORKSPACE);
        when(workspaceMemoryRepository.list("ws-1")).thenReturn(List.of(entry));

        List<MemorySearchResult> results = recallService.recallWorkspace("ws-1", null, 10);

        assertEquals(1, results.size());
        assertEquals("ws-entry", results.get(0).getMemory().getTitle());
    }

    // ---- recallSession ----

    @Test
    void testRecallSessionReturnsEmptyWhenNoSnapshot() {
        when(sessionMemoryRepository.get("sess-x")).thenReturn(Optional.empty());

        List<MemorySearchResult> results = recallService.recallSession("sess-x", null, 10);

        assertTrue(results.isEmpty());
    }

    @Test
    void testRecallSessionBuildsMultipleEntries() {
        SessionSnapshot snapshot = SessionSnapshot.builder()
                .sessionId("sess-1")
                .summary("会话摘要内容")
                .recentDecisions(List.of("决策A", "决策B"))
                .unresolvedQuestions(List.of("问题X"))
                .updatedAt(Instant.now())
                .build();
        when(sessionMemoryRepository.get("sess-1")).thenReturn(Optional.of(snapshot));

        List<MemorySearchResult> results = recallService.recallSession("sess-1", null, 10);

        // summary + 2 decisions + 1 question = 4 entries
        assertEquals(4, results.size());
    }

    @Test
    void testRecallSessionSummaryEntryHasCorrectType() {
        SessionSnapshot snapshot = SessionSnapshot.builder()
                .sessionId("sess-2")
                .summary("摘要")
                .recentDecisions(List.of())
                .unresolvedQuestions(List.of())
                .updatedAt(Instant.now())
                .build();
        when(sessionMemoryRepository.get("sess-2")).thenReturn(Optional.of(snapshot));

        List<MemorySearchResult> results = recallService.recallSession("sess-2", null, 10);

        assertEquals(1, results.size());
        assertEquals(MemoryType.SESSION_SUMMARY, results.get(0).getMemory().getType());
        assertEquals(MemoryScope.SESSION, results.get(0).getMemory().getScope());
    }

    @Test
    void testRecallSessionSkipsSummaryWhenBlank() {
        SessionSnapshot snapshot = SessionSnapshot.builder()
                .sessionId("sess-3")
                .summary("  ")
                .recentDecisions(List.of("决策A"))
                .unresolvedQuestions(List.of())
                .updatedAt(Instant.now())
                .build();
        when(sessionMemoryRepository.get("sess-3")).thenReturn(Optional.of(snapshot));

        List<MemorySearchResult> results = recallService.recallSession("sess-3", null, 10);

        // blank summary skipped, only 1 decision
        assertEquals(1, results.size());
        assertEquals(MemoryType.WORKSPACE_DECISION, results.get(0).getMemory().getType());
    }

    // ---- recallComposite ----

    @Test
    void testRecallCompositeOrderSessionFirst() {
        // 用带顺序记录的 ranker 验证 session 条目先传入
        SessionSnapshot snapshot = SessionSnapshot.builder()
                .sessionId("sess-c")
                .summary("session summary")
                .recentDecisions(List.of())
                .unresolvedQuestions(List.of())
                .updatedAt(Instant.now())
                .build();
        when(sessionMemoryRepository.get("sess-c")).thenReturn(Optional.of(snapshot));
        when(characterMemoryRepository.list("char-c")).thenReturn(
                List.of(entry("char-entry", MemoryType.CHARACTER_PROFILE, MemoryScope.CHARACTER)));
        when(workspaceMemoryRepository.list("ws-c")).thenReturn(
                List.of(entry("ws-entry", MemoryType.WORKSPACE_DECISION, MemoryScope.WORKSPACE)));

        MemoryRecallServiceImpl svc = new MemoryRecallServiceImpl(
                characterMemoryRepository, workspaceMemoryRepository,
                sessionMemoryRepository, llmCallerSelector);

        // 空 query 走全量召回，确保三条来源的条目都被返回，验证插入顺序
        MemoryQuery query = MemoryQuery.builder()
                .sessionId("sess-c")
                .characterId("char-c")
                .workspaceId("ws-c")
                .limit(10)
                .build();

        List<MemorySearchResult> results = svc.recallComposite(query);

        assertEquals(3, results.size());
        // 全量召回时分数相同，顺序由插入顺序决定：session > character > workspace
        assertEquals(MemoryType.SESSION_SUMMARY, results.get(0).getMemory().getType());
        assertEquals(MemoryType.CHARACTER_PROFILE, results.get(1).getMemory().getType());
        assertEquals(MemoryType.WORKSPACE_DECISION, results.get(2).getMemory().getType());
    }

    @Test
    void testRecallCompositeRespectsLimit() {
        when(sessionMemoryRepository.get(any())).thenReturn(Optional.empty());
        when(characterMemoryRepository.list("char-c")).thenReturn(List.of(
                entry("e1", MemoryType.CHARACTER_PROFILE, MemoryScope.CHARACTER),
                entry("e2", MemoryType.CHARACTER_PROFILE, MemoryScope.CHARACTER),
                entry("e3", MemoryType.CHARACTER_PROFILE, MemoryScope.CHARACTER)));
        when(workspaceMemoryRepository.list(any())).thenReturn(List.of());

        MemoryQuery query = MemoryQuery.builder()
                .characterId("char-c")
                .limit(2)
                .build();

        List<MemorySearchResult> results = recallService.recallComposite(query);

        assertEquals(2, results.size());
    }

    @Test
    void testRecallCompositeFiltersByType() {
        when(sessionMemoryRepository.get(any())).thenReturn(Optional.empty());
        when(characterMemoryRepository.list("char-f")).thenReturn(List.of(
                entry("profile", MemoryType.CHARACTER_PROFILE, MemoryScope.CHARACTER),
                entry("feedback", MemoryType.FEEDBACK, MemoryScope.CHARACTER)));
        when(workspaceMemoryRepository.list(any())).thenReturn(List.of());

        MemoryQuery query = MemoryQuery.builder()
                .characterId("char-f")
                .types(Set.of(MemoryType.FEEDBACK))
                .limit(10)
                .build();

        List<MemorySearchResult> results = recallService.recallComposite(query);

        assertEquals(1, results.size());
        assertEquals(MemoryType.FEEDBACK, results.get(0).getMemory().getType());
    }

    @Test
    void testRecallCompositeFiltersByScope() {
        when(sessionMemoryRepository.get(any())).thenReturn(Optional.empty());
        when(characterMemoryRepository.list(any())).thenReturn(List.of());
        when(workspaceMemoryRepository.list("ws-f")).thenReturn(List.of(
                entry("ws-entry", MemoryType.WORKSPACE_DECISION, MemoryScope.WORKSPACE)));

        MemoryQuery query = MemoryQuery.builder()
                .workspaceId("ws-f")
                .scopes(Set.of(MemoryScope.CHARACTER))  // 只要 CHARACTER scope，ws 结果被过滤掉
                .limit(10)
                .build();

        List<MemorySearchResult> results = recallService.recallComposite(query);

        assertTrue(results.isEmpty());
    }

    @Test
    void testRecallCompositeSkipsNullIds() {
        MemoryQuery query = MemoryQuery.builder()
                .limit(10)
                .build();

        List<MemorySearchResult> results = recallService.recallComposite(query);

        assertTrue(results.isEmpty());
        verifyNoInteractions(characterMemoryRepository, workspaceMemoryRepository, sessionMemoryRepository);
    }

    // ---- helpers ----

    private MemoryEntry entry(String title, MemoryType type, MemoryScope scope) {
        return MemoryEntry.builder()
                .title(title)
                .type(type)
                .scope(scope)
                .content(title + " content")
                .updatedAt(Instant.now())
                .build();
    }
}