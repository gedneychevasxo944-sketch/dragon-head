package org.dragon.memory.memv2.storage.fs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dragon.memory.memv2.core.SessionSnapshot;
import org.dragon.memory.memv2.storage.MemoryPathResolver;
import org.dragon.memory.memv2.storage.MemoryMarkdownParser;
import org.dragon.memory.memv2.storage.repo.SessionMemoryRepository;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 文件系统会话记忆仓库实现类
 * 使用本地文件系统存储会话记忆，管理 session-memory.md 和 events.jsonl 文件
 *
 * @author wyj
 * @version 1.0
 */
@Repository
public class FileSessionMemoryRepository implements SessionMemoryRepository {
    private final MemoryPathResolver pathResolver;
    private final MemoryMarkdownParser markdownParser;

    public FileSessionMemoryRepository(MemoryPathResolver pathResolver,
                                       MemoryMarkdownParser markdownParser) {
        this.pathResolver = pathResolver;
        this.markdownParser = markdownParser;
    }

    @Override
    public SessionSnapshot create(String sessionId, String workspaceId, String characterId) {
        Path sessionRoot = pathResolver.resolveSessionRoot(sessionId);
        try {
            if (!Files.exists(sessionRoot)) {
                Files.createDirectories(sessionRoot);
            }
            SessionSnapshot snapshot = new SessionSnapshot();
            snapshot.setSessionId(sessionId);
            snapshot.setWorkspaceId(workspaceId);
            snapshot.setCharacterId(characterId);
            snapshot.setSummary("Session initialized.");
            Files.writeString(pathResolver.resolveSessionMemoryFile(sessionId),
                    markdownParser.renderSession(snapshot));
            return snapshot;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create session memory: " + e.getMessage(), e);
        }
    }

    @Override
    public SessionSnapshot update(String sessionId, SessionSnapshot snapshot) {
        Path sessionFile = pathResolver.resolveSessionMemoryFile(sessionId);
        try {
            Files.writeString(sessionFile, markdownParser.renderSession(snapshot));
            return snapshot;
        } catch (IOException e) {
            throw new RuntimeException("Failed to update session memory: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<SessionSnapshot> get(String sessionId) {
        Path sessionFile = pathResolver.resolveSessionMemoryFile(sessionId);
        try {
            if (Files.exists(sessionFile)) {
                return Optional.of(markdownParser.parseSession(Files.readString(sessionFile)));
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get session memory: " + e.getMessage(), e);
        }
    }

    @Override
    public void appendEvent(String sessionId, String event) {
        Path eventsFile = pathResolver.resolveSessionEventsFile(sessionId);
        try {
            if (!Files.exists(eventsFile.getParent())) {
                Files.createDirectories(eventsFile.getParent());
            }
            if (!Files.exists(eventsFile)) {
                Files.createFile(eventsFile);
            }
            Files.writeString(eventsFile, event + "\n", StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append session event: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> listEvents(String sessionId) {
        Path eventsFile = pathResolver.resolveSessionEventsFile(sessionId);
        try {
            if (Files.exists(eventsFile)) {
                return Files.readAllLines(eventsFile);
            }
            return new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list session events: " + e.getMessage(), e);
        }
    }

    @Override
    public void checkpoint(String sessionId, SessionSnapshot snapshot) {
        Path checkpointsDir = pathResolver.resolveSessionCheckpointsDir(sessionId);
        try {
            if (!Files.exists(checkpointsDir)) {
                Files.createDirectories(checkpointsDir);
            }
            String checkpointFilename = "checkpoint_" + System.currentTimeMillis() + ".json";
            Files.writeString(checkpointsDir.resolve(checkpointFilename),
                    new ObjectMapper().writeValueAsString(snapshot));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create session checkpoint: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear(String sessionId) {
        Path sessionRoot = pathResolver.resolveSessionRoot(sessionId);
        try {
            if (Files.exists(sessionRoot)) {
                Files.walk(sessionRoot)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear session memory: " + e.getMessage(), e);
        }
    }
}
