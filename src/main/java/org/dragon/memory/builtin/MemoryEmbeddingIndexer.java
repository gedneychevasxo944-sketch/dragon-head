package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.MemorySearchResult;
import org.dragon.memory.provider.EmbeddingProvider;
import org.dragon.memory.provider.EmbeddingProviderFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MemoryEmbeddingIndexer {

    private final ResolvedMemorySearchConfig searchConfig;
    private final EmbeddingProvider embeddingProvider;
    private final MemoryIndexRepository indexRepository;

    public MemoryEmbeddingIndexer(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        this.embeddingProvider = EmbeddingProviderFactory.create(searchConfig.getProvider(), searchConfig.getModel());
        this.indexRepository = new MemoryIndexRepository(searchConfig);
    }

    // Markdown 分块功能
    public List<Chunk> chunkMarkdown(String content) {
        List<Chunk> chunks = new ArrayList<>();
        int chunkSize = searchConfig.getChunkingTokens();
        int overlap = searchConfig.getChunkingOverlap();

        if (content.length() <= chunkSize) {
            chunks.add(new Chunk(content, 0, content.length(), 0));
            return chunks;
        }

        int start = 0;
        int chunkId = 0;
        while (start < content.length()) {
            int end = start + chunkSize;
            if (end > content.length()) {
                end = content.length();
            }

            String chunkText = content.substring(start, end);
            chunks.add(new Chunk(chunkText, start, end, chunkId));

            start += chunkSize - overlap;
            chunkId++;
        }

        return chunks;
    }

    // 文件索引逻辑
    public void indexFile(String path) {
        try {
            // 读取文件内容
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            // 分块
            List<Chunk> chunks = chunkMarkdown(content.toString());

            // 向量化
            List<List<Double>> embeddings = embedChunks(chunks);

            // 写入索引
            for (int i = 0; i < chunks.size(); i++) {
                Chunk chunk = chunks.get(i);
                List<Double> embedding = embeddings.get(i);
                indexRepository.upsertChunk(chunk, embedding);
            }

            System.out.println("Successfully indexed file: " + path);

        } catch (IOException e) {
            System.err.println("Error indexing file " + path + ": " + e.getMessage());
        }
    }

    // 向量化功能
    private List<List<Double>> embedChunks(List<Chunk> chunks) {
        List<List<Double>> embeddings = new ArrayList<>();

        if (embeddingProvider == null) {
            System.err.println("Embedding provider not configured");
            return embeddings;
        }

        try {
            for (Chunk chunk : chunks) {
                List<Double> embedding = embeddingProvider.getEmbedding(chunk.getText());
                embeddings.add(embedding);
            }
        } catch (Exception e) {
            System.err.println("Error embedding chunks: " + e.getMessage());
        }

        return embeddings;
    }

    // 查询向量化功能
    public List<Double> embedQuery(String text) {
        if (embeddingProvider == null) {
            System.err.println("Embedding provider not configured");
            return null;
        }

        try {
            return embeddingProvider.getEmbedding(text);
        } catch (Exception e) {
            System.err.println("Error embedding query: " + e.getMessage());
            return null;
        }
    }

    // 内部分块类
    public static class Chunk {
        private final String text;
        private final int startPos;
        private final int endPos;
        private final int chunkId;

        public Chunk(String text, int startPos, int endPos, int chunkId) {
            this.text = text;
            this.startPos = startPos;
            this.endPos = endPos;
            this.chunkId = chunkId;
        }

        public String getText() {
            return text;
        }

        public int getStartPos() {
            return startPos;
        }

        public int getEndPos() {
            return endPos;
        }

        public int getChunkId() {
            return chunkId;
        }

        public String getContent() {
            return text;
        }
    }
}
