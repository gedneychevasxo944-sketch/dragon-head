package org.dragon.memory;

import org.dragon.memory.service.core.impl.SessionMemoryServiceImpl;
import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.service.core.MemoryExtractionService;
import org.dragon.memory.constants.MemoryType;
import org.dragon.memory.entity.SessionSnapshot;
import org.dragon.memory.storage.repo.SessionMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DefaultSessionMemoryService 单元测试
 *
 * @author binarytom
 * @version 1.0
 */
public class SessionMemoryServiceTest {

    private SessionMemoryRepository sessionMemoryRepository;
    private MemoryExtractionService memoryExtractionService;
    private SessionMemoryServiceImpl sessionMemoryService;

    @BeforeEach
    void setUp() {
        sessionMemoryRepository = mock(SessionMemoryRepository.class);
        memoryExtractionService = mock(MemoryExtractionService.class);
        sessionMemoryService = new SessionMemoryServiceImpl(sessionMemoryRepository, memoryExtractionService);
    }

    // ---- start / get ----

    @Test
    void testStartDelegatesToRepository() {
        SessionSnapshot snapshot = snapshot("sess-1");
        when(sessionMemoryRepository.create("sess-1", "ws-1", "char-1")).thenReturn(snapshot);

        SessionSnapshot result = sessionMemoryService.start("sess-1", "ws-1", "char-1");

        assertSame(snapshot, result);
        verify(sessionMemoryRepository).create("sess-1", "ws-1", "char-1");
    }

    @Test
    void testGetReturnsSnapshotFromRepository() {
        SessionSnapshot snapshot = snapshot("sess-1");
        when(sessionMemoryRepository.get("sess-1")).thenReturn(Optional.of(snapshot));

        SessionSnapshot result = sessionMemoryService.get("sess-1");

        assertSame(snapshot, result);
    }

    @Test
    void testGetReturnsNullWhenNotFound() {
        when(sessionMemoryRepository.get("missing")).thenReturn(Optional.empty());

        assertNull(sessionMemoryService.get("missing"));
    }

    // ---- checkpoint ----

    @Test
    void testCheckpointCallsRepositoryWhenSnapshotExists() {
        SessionSnapshot snapshot = snapshot("sess-1");
        when(sessionMemoryRepository.get("sess-1")).thenReturn(Optional.of(snapshot));

        sessionMemoryService.checkpoint("sess-1");

        verify(sessionMemoryRepository).checkpoint("sess-1", snapshot);
    }

    @Test
    void testCheckpointSkipsWhenNoSnapshot() {
        when(sessionMemoryRepository.get("missing")).thenReturn(Optional.empty());

        sessionMemoryService.checkpoint("missing");

        verify(sessionMemoryRepository, never()).checkpoint(any(), any());
    }

    // ---- extractCandidates ----

    @Test
    void testExtractCandidatesReturnsEmptyWhenNoSnapshot() {
        when(sessionMemoryRepository.get("missing")).thenReturn(Optional.empty());

        List<MemoryEntry> result = sessionMemoryService.extractCandidates("missing");

        assertTrue(result.isEmpty());
        verifyNoInteractions(memoryExtractionService);
    }

    @Test
    void testExtractCandidatesDelegatesToExtractionService() {
        SessionSnapshot snapshot = snapshot("sess-1");
        List<String> events = List.of("event1", "event2");
        List<MemoryEntry> extracted = List.of(entry("candidate"));

        when(sessionMemoryRepository.get("sess-1")).thenReturn(Optional.of(snapshot));
        when(sessionMemoryRepository.listEvents("sess-1")).thenReturn(events);
        when(memoryExtractionService.extract(snapshot, events)).thenReturn(extracted);

        List<MemoryEntry> result = sessionMemoryService.extractCandidates("sess-1");

        assertSame(extracted, result);
        verify(memoryExtractionService).extract(snapshot, events);
    }

    // ---- promote ----

    @Test
    void testPromoteReturnsEmptyWhenNoSnapshot() {
        when(sessionMemoryRepository.get("missing")).thenReturn(Optional.empty());

        List<MemoryEntry> result = sessionMemoryService.promote("missing");

        assertTrue(result.isEmpty());
        verifyNoInteractions(memoryExtractionService);
    }

    @Test
    void testPromoteDelegatesToExtractionService() {
        SessionSnapshot snapshot = snapshot("sess-1");
        List<String> events = List.of("event1");
        List<MemoryEntry> promoted = List.of(entry("promoted"));

        when(sessionMemoryRepository.get("sess-1")).thenReturn(Optional.of(snapshot));
        when(sessionMemoryRepository.listEvents("sess-1")).thenReturn(events);
        when(memoryExtractionService.promote("sess-1", snapshot, events)).thenReturn(promoted);

        List<MemoryEntry> result = sessionMemoryService.promote("sess-1");

        assertSame(promoted, result);
        verify(memoryExtractionService).promote("sess-1", snapshot, events);
    }

    // ---- close ----

    @Test
    void testCloseExecutesCheckpointThenPromoteThenClear() {
        SessionSnapshot snapshot = snapshot("sess-1");
        List<String> events = List.of();

        when(sessionMemoryRepository.get("sess-1")).thenReturn(Optional.of(snapshot));
        when(sessionMemoryRepository.listEvents("sess-1")).thenReturn(events);
        when(memoryExtractionService.promote(any(), any(), any())).thenReturn(List.of());

        sessionMemoryService.close("sess-1");

        // checkpoint → promote → clear の順序を検証
        var inOrder = inOrder(sessionMemoryRepository, memoryExtractionService);
        inOrder.verify(sessionMemoryRepository).checkpoint("sess-1", snapshot);
        inOrder.verify(memoryExtractionService).promote(eq("sess-1"), eq(snapshot), any());
        inOrder.verify(sessionMemoryRepository).clear("sess-1");
    }

    @Test
    void testCloseStillClearsEvenIfNoSnapshot() {
        // snapshot が存在しない場合、checkpoint と promote はスキップされるが clear は呼ばれる
        when(sessionMemoryRepository.get("missing")).thenReturn(Optional.empty());

        sessionMemoryService.close("missing");

        verify(sessionMemoryRepository, never()).checkpoint(any(), any());
        verifyNoInteractions(memoryExtractionService);
        verify(sessionMemoryRepository).clear("missing");
    }

    // ---- appendEvent ----

    @Test
    void testAppendEventDelegatesToRepository() {
        sessionMemoryService.appendEvent("sess-1", "some event");

        verify(sessionMemoryRepository).appendEvent("sess-1", "some event");
    }

    // ---- appendEntry ----

    @Test
    void testAppendEntrySkipsWhenNoSnapshot() {
        when(sessionMemoryRepository.get("missing")).thenReturn(Optional.empty());

        sessionMemoryService.appendEntry("missing", entry("e1"));

        verify(sessionMemoryRepository, never()).appendEvent(any(), any());
    }

    @Test
    void testAppendEntrySerializesEntryAsEvent() {
        SessionSnapshot snapshot = snapshot("sess-1");
        when(sessionMemoryRepository.get("sess-1")).thenReturn(Optional.of(snapshot));

        MemoryEntry entry = MemoryEntry.builder()
                .title("My Title")
                .type(MemoryType.FEEDBACK)
                .content("My Content")
                .build();

        sessionMemoryService.appendEntry("sess-1", entry);

        verify(sessionMemoryRepository).appendEvent(eq("sess-1"), argThat(event ->
                event.contains("\"type\":\"FEEDBACK\"")
                        && event.contains("\"title\":\"My Title\"")
                        && event.contains("My Content")));
    }

    @Test
    void testAppendEntryHandlesNullTypeAndContent() {
        SessionSnapshot snapshot = snapshot("sess-1");
        when(sessionMemoryRepository.get("sess-1")).thenReturn(Optional.of(snapshot));

        MemoryEntry entry = MemoryEntry.builder()
                .title("No Type")
                .build();

        // null type と null content でも例外が発生しないこと
        assertDoesNotThrow(() -> sessionMemoryService.appendEntry("sess-1", entry));
        verify(sessionMemoryRepository).appendEvent(eq("sess-1"), argThat(event ->
                event.contains("\"type\":\"UNKNOWN\"") && event.contains("null")));
    }

    // ---- helpers ----

    private SessionSnapshot snapshot(String sessionId) {
        return SessionSnapshot.builder()
                .sessionId(sessionId)
                .summary("test summary")
                .recentDecisions(List.of())
                .unresolvedQuestions(List.of())
                .updatedAt(Instant.now())
                .build();
    }

    private MemoryEntry entry(String title) {
        return MemoryEntry.builder()
                .title(title)
                .type(MemoryType.FEEDBACK)
                .content(title + " content")
                .updatedAt(Instant.now())
                .build();
    }
}