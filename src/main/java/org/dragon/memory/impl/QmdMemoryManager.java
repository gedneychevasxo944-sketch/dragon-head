package org.dragon.memory.impl;

import org.dragon.memory.MemorySearchManager;
import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.*;
import org.dragon.memory.qmd.*;

import java.util.List;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class QmdMemoryManager implements MemorySearchManager {

    private final ResolvedMemorySearchConfig searchConfig;
    private final QmdCliClient qmdCliClient;
    private final QmdMcporterClient qmdMcporterClient;
    private final QmdCollectionRegistry qmdCollectionRegistry;
    private final QmdSessionExporter qmdSessionExporter;
    private final QmdDocPathResolver qmdDocPathResolver;
    private final QmdUpdateCoordinator qmdUpdateCoordinator;
    private final QmdRepairService qmdRepairService;

    public QmdMemoryManager(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        this.qmdCliClient = new QmdCliClient();
        this.qmdMcporterClient = new QmdMcporterClient();
        this.qmdCollectionRegistry = new QmdCollectionRegistry(searchConfig);
        this.qmdSessionExporter = new QmdSessionExporter();
        this.qmdDocPathResolver = new QmdDocPathResolver();
        this.qmdUpdateCoordinator = new QmdUpdateCoordinator(searchConfig);
        this.qmdRepairService = new QmdRepairService();
    }

    @Override
    public List<MemorySearchResult> search(String query, SearchOptions opts) {
        String[] searchArgs = {"search", query};
        String searchOutput = qmdCliClient.executeCommand("qmd", searchArgs);
        return parseSearchResult(searchOutput);
    }

    private List<MemorySearchResult> parseSearchResult(String output) {
        List<MemorySearchResult> results = new java.util.ArrayList<>();
        // 简单实现：解析 qmd 搜索输出并转换为 MemorySearchResult
        // 实际应用中需要根据 qmd 的输出格式进行更详细的解析
        if (!output.isEmpty()) {
            String[] lines = output.split("\n");
            if (lines.length > 0) {
                MemorySearchResult result = new MemorySearchResult();
                result.setSnippet(lines[0]);
                result.setPath("dummy/path.md");
                result.setScore(0.9);
                results.add(result);
            }
        }
        return results;
    }

    @Override
    public ReadFileResult readFile(ReadFileRequest request) {
        String path = qmdDocPathResolver.resolveDocIdToPath(request.getPath());
        StringBuilder content = new StringBuilder();
        int totalLines = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                if (request.getStartLine() > 0 && totalLines < request.getStartLine()) {
                    continue;
                }
                if (request.getEndLine() > 0 && totalLines > request.getEndLine()) {
                    break;
                }
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return new ReadFileResult(path, content.toString(), totalLines);
    }

    @Override
    public MemoryProviderStatus status() {
        MemoryProviderStatus status = new MemoryProviderStatus();
        status.setBackend("qmd");
        status.setProvider(searchConfig.getProvider());
        status.setModel(searchConfig.getModel());
        status.setRequestedProvider(searchConfig.getFallback());
        // TODO: 补充其他状态信息
        return status;
    }

    @Override
    public MemorySyncProgressUpdate sync(SyncRequest request) {
        return qmdUpdateCoordinator.sync(request);
    }

    @Override
    public MemoryEmbeddingProbeResult probeEmbeddingAvailability() {
        try {
            // 简单实现：检查 qmd 命令是否可用
            String[] embedArgs = {"embed"};
            String embedOutput = qmdCliClient.executeCommand("qmd", embedArgs);
            boolean available = !embedOutput.contains("Error");
            return new MemoryEmbeddingProbeResult(available, available ? null : embedOutput);
        } catch (Exception e) {
            return new MemoryEmbeddingProbeResult(false, e.getMessage());
        }
    }

    @Override
    public boolean probeVectorAvailability() {
        return searchConfig.isStoreVectorEnabled();
    }

    @Override
    public void warmSession(String sessionKey) {
        // 简单实现：异步触发会话同步
        new Thread(() -> {
            qmdUpdateCoordinator.sync(SyncRequest.targetedSessionSync(sessionKey));
        }).start();
    }

    @Override
    public void close() {
        // TODO: 关闭资源
    }
}