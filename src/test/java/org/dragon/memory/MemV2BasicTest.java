package org.dragon.memory;

import org.dragon.memory.config.MemoryProperties;
import org.dragon.memory.storage.MemoryIndexParser;
import org.dragon.memory.storage.MemoryMarkdownParser;
import org.dragon.memory.storage.MemoryPathResolver;
import org.dragon.memory.storage.fs.FileCharacterMemoryRepository;
import org.dragon.memory.storage.fs.FileSessionMemoryRepository;
import org.dragon.memory.storage.fs.FileWorkspaceMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemV2 模块基本功能测试
 *
 * @author binarytom
 * @version 1.0
 */
public class MemV2BasicTest {

    @TempDir
    Path tempDir;

    private MemoryPathResolver pathResolver;
    private MemoryMarkdownParser markdownParser;
    private MemoryIndexParser indexParser;

    @BeforeEach
    void setUp() {
        MemoryProperties props = new MemoryProperties();
        props.setRootDir(tempDir.toString());
        pathResolver = new MemoryPathResolver(props);
        markdownParser = new MemoryMarkdownParser();
        indexParser = new MemoryIndexParser();
    }

    // ---- Task 1.1 路径前缀验证 ----

    @Test
    public void testCharacterRootPathPrefix() {
        Path root = pathResolver.resolveCharacterRoot("test-char-001");
        assertTrue(root.getFileName().toString().startsWith("char-"),
                "Character root should start with 'char-', got: " + root.getFileName());
        assertTrue(root.toString().contains("char-test-char-001"),
                "Character root should contain 'char-test-char-001', got: " + root);
    }

    @Test
    public void testWorkspaceRootPathPrefix() {
        Path root = pathResolver.resolveWorkspaceRoot("test-ws-001");
        assertTrue(root.getFileName().toString().startsWith("ws-"),
                "Workspace root should start with 'ws-', got: " + root.getFileName());
        assertTrue(root.toString().contains("ws-test-ws-001"),
                "Workspace root should contain 'ws-test-ws-001', got: " + root);
    }

    @Test
    public void testSessionRootPathPrefix() {
        Path root = pathResolver.resolveSessionRoot("test-session-001");
        assertTrue(root.getFileName().toString().startsWith("session-"),
                "Session root should start with 'session-', got: " + root.getFileName());
        assertTrue(root.toString().contains("session-test-session-001"),
                "Session root should contain 'session-test-session-001', got: " + root);
    }

    // ---- Task 1.2 bindings.yml 路径验证 ----

    @Test
    public void testCharacterBindingsPath() {
        Path bindings = pathResolver.resolveCharacterBindings("char-001");
        assertEquals("bindings.yml", bindings.getFileName().toString(),
                "Character bindings file should be named 'bindings.yml'");
        assertTrue(bindings.getParent().getFileName().toString().startsWith("char-"),
                "Character bindings should be under a char- directory");
    }

    @Test
    public void testWorkspaceBindingsPath() {
        Path bindings = pathResolver.resolveWorkspaceBindings("ws-001");
        assertEquals("bindings.yml", bindings.getFileName().toString(),
                "Workspace bindings file should be named 'bindings.yml'");
        assertTrue(bindings.getParent().getFileName().toString().startsWith("ws-"),
                "Workspace bindings should be under a ws- directory");
    }

    // ---- Task 1.3 / 1.5 initSpace 目录结构验证 ----

    @Test
    public void testCharacterInitSpaceCreatesDirectoryStructure() {
        FileCharacterMemoryRepository repo =
                new FileCharacterMemoryRepository(pathResolver, markdownParser, indexParser);

        repo.initSpace("init-test-char");

        assertTrue(Files.isDirectory(pathResolver.resolveCharacterRoot("init-test-char")),
                "char root directory should exist");
        assertTrue(Files.isDirectory(pathResolver.resolveCharacterMemDir("init-test-char")),
                "mem/ directory should exist");
        assertTrue(Files.exists(pathResolver.resolveCharacterIndex("init-test-char")),
                "MEMORY.md should exist");
        assertTrue(Files.exists(pathResolver.resolveCharacterBindings("init-test-char")),
                "bindings.yml should exist");
    }

    @Test
    public void testCharacterInitSpaceMemoryMdContent() throws Exception {
        FileCharacterMemoryRepository repo =
                new FileCharacterMemoryRepository(pathResolver, markdownParser, indexParser);

        repo.initSpace("content-test-char");

        String content = Files.readString(pathResolver.resolveCharacterIndex("content-test-char"));
        assertTrue(content.contains("# MEMORY"), "MEMORY.md should contain '# MEMORY' header");
    }

    @Test
    public void testCharacterInitSpaceBindingsYmlContent() throws Exception {
        FileCharacterMemoryRepository repo =
                new FileCharacterMemoryRepository(pathResolver, markdownParser, indexParser);

        repo.initSpace("bindings-test-char");

        String content = Files.readString(pathResolver.resolveCharacterBindings("bindings-test-char"));
        assertTrue(content.contains("bindings:"), "bindings.yml should contain 'bindings:' key");
    }

    @Test
    public void testCharacterInitSpaceIdempotent() {
        FileCharacterMemoryRepository repo =
                new FileCharacterMemoryRepository(pathResolver, markdownParser, indexParser);

        // 调用两次，不应抛出异常，也不应覆盖已有文件
        repo.initSpace("idempotent-char");
        repo.initSpace("idempotent-char");

        assertTrue(Files.exists(pathResolver.resolveCharacterIndex("idempotent-char")),
                "MEMORY.md should still exist after second initSpace call");
    }

    @Test
    public void testWorkspaceInitSpaceCreatesDirectoryStructure() {
        FileWorkspaceMemoryRepository repo =
                new FileWorkspaceMemoryRepository(pathResolver, markdownParser, indexParser);

        repo.initSpace("init-test-ws");

        assertTrue(Files.isDirectory(pathResolver.resolveWorkspaceRoot("init-test-ws")),
                "ws root directory should exist");
        assertTrue(Files.isDirectory(pathResolver.resolveWorkspaceMemDir("init-test-ws")),
                "mem/ directory should exist");
        assertTrue(Files.exists(pathResolver.resolveWorkspaceIndex("init-test-ws")),
                "MEMORY.md should exist");
        assertTrue(Files.exists(pathResolver.resolveWorkspaceBindings("init-test-ws")),
                "bindings.yml should exist");
    }

    // ---- Task 1.4 session 空间初始化验证 ----

    @Test
    public void testSessionCreateInitializesDirectoryStructure() {
        FileSessionMemoryRepository repo =
                new FileSessionMemoryRepository(pathResolver, markdownParser);

        repo.create("init-test-session", "ws-001", "char-001");

        assertTrue(Files.isDirectory(pathResolver.resolveSessionRoot("init-test-session")),
                "session root directory should exist");
        assertTrue(Files.isDirectory(pathResolver.resolveSessionCheckpointsDir("init-test-session")),
                "checkpoints/ directory should exist");
        assertTrue(Files.exists(pathResolver.resolveSessionMemoryFile("init-test-session")),
                "session-memory.md should exist");
        assertTrue(Files.exists(pathResolver.resolveSessionEventsFile("init-test-session")),
                "events.jsonl should exist");
    }

    @Test
    public void testSessionCreateSetsCorrectIds() {
        FileSessionMemoryRepository repo =
                new FileSessionMemoryRepository(pathResolver, markdownParser);

        var snapshot = repo.create("session-ids-test", "ws-abc", "char-xyz");

        assertEquals("session-ids-test", snapshot.getSessionId());
        assertEquals("ws-abc", snapshot.getWorkspaceId());
        assertEquals("char-xyz", snapshot.getCharacterId());
    }
}