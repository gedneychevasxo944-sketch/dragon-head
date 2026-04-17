package org.dragon.memory;

import org.dragon.agent.llm.LLMRequest;
import org.dragon.agent.llm.LLMResponse;
import org.dragon.agent.llm.caller.LLMCaller;
import org.dragon.agent.llm.caller.LLMCallerSelector;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.memory.constants.MemoryScope;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.SessionSnapshot;
import org.dragon.memory.service.core.MemoryValidationPolicy;
import org.dragon.memory.service.core.impl.LlmMemoryExtractionServiceImpl;
import org.dragon.memory.storage.repo.CharacterMemoryRepository;
import org.dragon.memory.storage.repo.WorkspaceMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * LlmMemoryExtractionServiceImpl 单元测试
 *
 * @author binarytom
 * @version 1.0
 */
public class LlmMemoryExtractionServiceTest {

    private CharacterMemoryRepository characterMemoryRepository;
    private WorkspaceMemoryRepository workspaceMemoryRepository;
    private MemoryValidationPolicy memoryValidationPolicy;
    private LLMCallerSelector llmCallerSelector;
    private LLMCaller llmCaller;
    private ConfigApplication configApplication;
    private LlmMemoryExtractionServiceImpl service;

    @BeforeEach
    void setUp() {
        characterMemoryRepository = mock(CharacterMemoryRepository.class);
        workspaceMemoryRepository = mock(WorkspaceMemoryRepository.class);
        memoryValidationPolicy = mock(MemoryValidationPolicy.class);
        llmCallerSelector = mock(LLMCallerSelector.class);
        llmCaller = mock(LLMCaller.class);
        configApplication = mock(ConfigApplication.class);

        when(llmCallerSelector.getDefault()).thenReturn(llmCaller);
        when(configApplication.getGlobalPrompt(eq(PromptKeys.MEMORY_EXTRACT), any()))
                .thenReturn("extract-system-prompt");
        // 默认所有候选校验通过
        when(memoryValidationPolicy.validate(any()))
                .thenReturn(MemoryValidationPolicy.ValidationResult.valid());
        // 让 create 回传入参，方便断言
        when(characterMemoryRepository.create(any(), any()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(workspaceMemoryRepository.create(any(), any()))
                .thenAnswer(inv -> inv.getArgument(1));

        service = new LlmMemoryExtractionServiceImpl(
                characterMemoryRepository,
                workspaceMemoryRepository,
                memoryValidationPolicy,
                llmCallerSelector,
                configApplication);
    }

    // ---- routeScope (纯函数) ----

    @Test
    void testRouteScopeMapsTypes() {
        assertEquals(MemoryScope.CHARACTER,
                LlmMemoryExtractionServiceImpl.routeScope(MemoryType.CHARACTER_PROFILE));
        assertEquals(MemoryScope.CHARACTER,
                LlmMemoryExtractionServiceImpl.routeScope(MemoryType.FEEDBACK));
        assertEquals(MemoryScope.WORKSPACE,
                LlmMemoryExtractionServiceImpl.routeScope(MemoryType.PROJECT));
        assertEquals(MemoryScope.WORKSPACE,
                LlmMemoryExtractionServiceImpl.routeScope(MemoryType.REFERENCE));
        assertEquals(MemoryScope.WORKSPACE,
                LlmMemoryExtractionServiceImpl.routeScope(MemoryType.WORKSPACE_DECISION));
        assertEquals(MemoryScope.SESSION,
                LlmMemoryExtractionServiceImpl.routeScope(MemoryType.SESSION_SUMMARY));
        assertEquals(MemoryScope.SESSION,
                LlmMemoryExtractionServiceImpl.routeScope(null));
    }

    // ---- extract ----

    @Test
    void testExtractReturnsEmptyWhenSnapshotIsNull() {
        List<MemoryEntry> result = service.extract(null, List.of());

        assertTrue(result.isEmpty());
        verifyNoInteractions(llmCaller);
    }

    @Test
    void testExtractReturnsEmptyWhenPromptMissing() {
        when(configApplication.getGlobalPrompt(eq(PromptKeys.MEMORY_EXTRACT), any())).thenReturn(null);

        List<MemoryEntry> result = service.extract(snapshot(), List.of());

        assertTrue(result.isEmpty());
        verifyNoInteractions(llmCaller);
    }

    @Test
    void testExtractParsesJsonArray() {
        mockLlmResponse("""
                {"candidates":[
                  {"title":"用户偏好","description":"偏好简洁回复","content":"user prefers terse","type":"FEEDBACK"},
                  {"title":"项目决定","description":"立项","content":"launch plan","type":"PROJECT"}
                ]}
                """);

        List<MemoryEntry> result = service.extract(snapshot(), List.of("event-1"));

        assertEquals(2, result.size());
        MemoryEntry first = result.get(0);
        assertEquals("用户偏好", first.getTitle());
        assertEquals("偏好简洁回复", first.getDescription());
        assertEquals("user prefers terse", first.getContent());
        assertEquals(MemoryType.FEEDBACK, first.getType());
        assertNotNull(first.getId());
        assertNotNull(first.getFileName());
        assertEquals(MemoryType.PROJECT, result.get(1).getType());
    }

    @Test
    void testExtractToleratesTextBeforeAndAfterJson() {
        mockLlmResponse("这里是一些解释文字\n```json\n{\"candidates\":[{\"content\":\"foo\",\"type\":\"FEEDBACK\"}]}\n```\n后续废话");

        List<MemoryEntry> result = service.extract(snapshot(), List.of());

        assertEquals(1, result.size());
        assertEquals("foo", result.get(0).getContent());
    }

    @Test
    void testExtractSkipsCandidatesWithBlankContent() {
        mockLlmResponse("""
                {"candidates":[
                  {"title":"no-content","type":"FEEDBACK"},
                  {"content":"","type":"FEEDBACK"},
                  {"content":"real","type":"FEEDBACK"}
                ]}
                """);

        List<MemoryEntry> result = service.extract(snapshot(), List.of());

        assertEquals(1, result.size());
        assertEquals("real", result.get(0).getContent());
    }

    @Test
    void testExtractDefaultsUnknownTypeToSessionSummary() {
        mockLlmResponse("""
                {"candidates":[
                  {"content":"x","type":"NOT_A_REAL_TYPE"}
                ]}
                """);

        List<MemoryEntry> result = service.extract(snapshot(), List.of());

        assertEquals(1, result.size());
        assertEquals(MemoryType.SESSION_SUMMARY, result.get(0).getType());
    }

    @Test
    void testExtractReturnsEmptyWhenLlmReturnsBlank() {
        mockLlmResponse("");

        List<MemoryEntry> result = service.extract(snapshot(), List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractReturnsEmptyWhenLlmReturnsInvalidJson() {
        mockLlmResponse("这是纯文本，不是 JSON");

        List<MemoryEntry> result = service.extract(snapshot(), List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractSwallowsLlmException() {
        when(llmCaller.call(any())).thenThrow(new RuntimeException("boom"));

        List<MemoryEntry> result = service.extract(snapshot(), List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractForwardsSystemPromptAndUserMessage() {
        mockLlmResponse("{\"candidates\":[]}");

        service.extract(snapshot(), List.of("event-xyz"));

        ArgumentCaptor<LLMRequest> captor = ArgumentCaptor.forClass(LLMRequest.class);
        verify(llmCaller).call(captor.capture());
        LLMRequest req = captor.getValue();
        assertEquals("extract-system-prompt", req.getSystemPrompt());
        assertEquals(1, req.getMessages().size());
        String userContent = req.getMessages().get(0).getContent();
        assertTrue(userContent.contains("event-xyz"));
        assertTrue(userContent.contains("workspaceId: ws-1"));
        assertTrue(userContent.contains("characterId: char-1"));
    }

    // ---- promote ----

    @Test
    void testPromoteRoutesFeedbackToCharacterRepo() {
        mockLlmResponse("""
                {"candidates":[{"content":"reply-terse","type":"FEEDBACK"}]}
                """);

        List<MemoryEntry> promoted = service.promote("sess-1", snapshot(), List.of());

        assertEquals(1, promoted.size());
        assertEquals(MemoryScope.CHARACTER, promoted.get(0).getScope());
        assertEquals("char-1", promoted.get(0).getOwnerId());
        verify(characterMemoryRepository).create(eq("char-1"), any());
        verifyNoInteractions(workspaceMemoryRepository);
    }

    @Test
    void testPromoteRoutesProjectToWorkspaceRepo() {
        mockLlmResponse("""
                {"candidates":[{"content":"launch-plan","type":"PROJECT"}]}
                """);

        List<MemoryEntry> promoted = service.promote("sess-1", snapshot(), List.of());

        assertEquals(1, promoted.size());
        assertEquals(MemoryScope.WORKSPACE, promoted.get(0).getScope());
        assertEquals("ws-1", promoted.get(0).getOwnerId());
        verify(workspaceMemoryRepository).create(eq("ws-1"), any());
        verifyNoInteractions(characterMemoryRepository);
    }

    @Test
    void testPromoteSkipsSessionSummaryType() {
        mockLlmResponse("""
                {"candidates":[{"content":"recap","type":"SESSION_SUMMARY"}]}
                """);

        List<MemoryEntry> promoted = service.promote("sess-1", snapshot(), List.of());

        assertTrue(promoted.isEmpty());
        verifyNoInteractions(characterMemoryRepository);
        verifyNoInteractions(workspaceMemoryRepository);
    }

    @Test
    void testPromoteSkipsInvalidCandidates() {
        mockLlmResponse("""
                {"candidates":[{"content":"bad","type":"FEEDBACK"}]}
                """);
        when(memoryValidationPolicy.validate(any()))
                .thenReturn(MemoryValidationPolicy.ValidationResult.invalid("太短"));

        List<MemoryEntry> promoted = service.promote("sess-1", snapshot(), List.of());

        assertTrue(promoted.isEmpty());
        verifyNoInteractions(characterMemoryRepository);
        verifyNoInteractions(workspaceMemoryRepository);
    }

    @Test
    void testPromoteSkipsCharacterTypeWhenNoCharacterId() {
        mockLlmResponse("""
                {"candidates":[{"content":"c","type":"FEEDBACK"}]}
                """);
        SessionSnapshot snap = SessionSnapshot.builder()
                .sessionId("sess-1")
                .workspaceId("ws-1")
                .characterId(null)
                .summary("s")
                .updatedAt(Instant.now())
                .build();

        List<MemoryEntry> promoted = service.promote("sess-1", snap, List.of());

        assertTrue(promoted.isEmpty());
        verifyNoInteractions(characterMemoryRepository);
    }

    @Test
    void testPromoteMixedCandidatesRouteIndependently() {
        mockLlmResponse("""
                {"candidates":[
                  {"content":"fb","type":"FEEDBACK"},
                  {"content":"pj","type":"PROJECT"},
                  {"content":"ss","type":"SESSION_SUMMARY"}
                ]}
                """);

        List<MemoryEntry> promoted = service.promote("sess-1", snapshot(), List.of());

        assertEquals(2, promoted.size());
        verify(characterMemoryRepository).create(eq("char-1"), any());
        verify(workspaceMemoryRepository).create(eq("ws-1"), any());
    }

    // ---- helpers ----

    private SessionSnapshot snapshot() {
        return SessionSnapshot.builder()
                .sessionId("sess-1")
                .workspaceId("ws-1")
                .characterId("char-1")
                .summary("summary")
                .currentGoal("goal")
                .recentDecisions(List.of("decide-a"))
                .unresolvedQuestions(List.of("q?"))
                .updatedAt(Instant.now())
                .build();
    }

    private void mockLlmResponse(String content) {
        when(llmCaller.call(any()))
                .thenReturn(LLMResponse.builder().content(content).build());
    }
}
