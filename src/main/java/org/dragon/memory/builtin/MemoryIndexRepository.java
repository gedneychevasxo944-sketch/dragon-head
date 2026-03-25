package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.MemorySearchResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemoryIndexRepository {

    private final ResolvedMemorySearchConfig searchConfig;

    public MemoryIndexRepository(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    // 获取数据库连接
    private Connection getConnection() throws SQLException {
        String dbPath = searchConfig.getStorePath();
        String url = "jdbc:sqlite:" + dbPath;
        return DriverManager.getConnection(url);
    }

    // 关键词搜索
    public List<MemorySearchResult> searchKeyword(String query, int limit) {
        List<MemorySearchResult> results = new ArrayList<>();
        String sql = "SELECT c.*, f.path FROM chunks c " +
                "JOIN files f ON c.file_id = f.id " +
                "WHERE c.content LIKE ? " +
                "ORDER BY c.score DESC " +
                "LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + query + "%");
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MemorySearchResult result = new MemorySearchResult();
                    result.setPath(rs.getString("path"));
                    result.setScore(rs.getDouble("score"));
                    result.setSnippet(truncate(rs.getString("content"), 200));
                    results.add(result);
                }
            }
        } catch (SQLException e) {
            System.err.println("Keyword search failed: " + e.getMessage());
        }

        return results;
    }

    // 向量搜索（内存 fallback）
    public List<MemorySearchResult> searchVector(List<Double> queryVector, int limit) {
        List<MemorySearchResult> results = new ArrayList<>();
        String sql = "SELECT c.*, f.path, v.vector FROM chunks c " +
                "JOIN files f ON c.file_id = f.id " +
                "JOIN chunks_vec v ON c.id = v.chunk_id " +
                "ORDER BY (c.score + computeSimilarity(v.vector, ?)) DESC " +
                "LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 注意：这里使用了模拟的相似度计算，实际需要实现向量相似度计算
            pstmt.setString(1, vectorToString(queryVector));
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MemorySearchResult result = new MemorySearchResult();
                    result.setPath(rs.getString("path"));
                    result.setScore(rs.getDouble("score"));
                    result.setSnippet(truncate(rs.getString("content"), 200));
                    results.add(result);
                }
            }
        } catch (SQLException e) {
            System.err.println("Vector search failed: " + e.getMessage());
        }

        return results;
    }

    // 查询文件记录
    public int findFileIdByPath(String path) {
        String sql = "SELECT id FROM files WHERE path = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, path);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Find file failed: " + e.getMessage());
        }
        return -1;
    }

    // 插入或更新文件记录
    public int upsertFile(String path) {
        int fileId = findFileIdByPath(path);
        if (fileId > 0) {
            // 更新现有记录
            String sql = "UPDATE files SET last_modified = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE path = ?";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, path);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Update file failed: " + e.getMessage());
            }
        } else {
            // 插入新记录
            String sql = "INSERT INTO files (path, last_modified, source) VALUES (?, CURRENT_TIMESTAMP, 'memory')";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setString(1, path);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        fileId = rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                System.err.println("Insert file failed: " + e.getMessage());
            }
        }
        return fileId;
    }

    // 插入或更新 chunk 记录
    public void upsertChunk(MemoryEmbeddingIndexer.Chunk chunk, List<Double> embedding) {
        String path = "dummy/path.md"; // TODO: 实际文件路径需要通过参数传入
        int fileId = upsertFile(path);

        if (fileId > 0) {
            String sql = "INSERT OR REPLACE INTO chunks (file_id, chunk_id, content, start_pos, end_pos, score) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setInt(1, fileId);
                pstmt.setInt(2, chunk.getChunkId());
                pstmt.setString(3, chunk.getContent());
                pstmt.setInt(4, chunk.getStartPos());
                pstmt.setInt(5, chunk.getEndPos());
                pstmt.setDouble(6, 0.0); // 默认分数
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int chunkId = rs.getInt(1);
                        if (searchConfig.isStoreVectorEnabled() && embedding != null) {
                            upsertVector(chunkId, embedding);
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Upsert chunk failed: " + e.getMessage());
            }
        }
    }

    // 插入或更新向量记录
    private void upsertVector(int chunkId, List<Double> vector) {
        String sql = "INSERT OR REPLACE INTO chunks_vec (chunk_id, vector) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, chunkId);
            pstmt.setString(2, vectorToString(vector));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Upsert vector failed: " + e.getMessage());
        }
    }

    // 删除所有 chunk 和文件记录（用于重建索引）
    public void clearAllChunks() {
        String deleteChunksSql = "DELETE FROM chunks";
        String deleteFilesSql = "DELETE FROM files";
        String deleteVectorsSql = "DELETE FROM chunks_vec";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(deleteChunksSql);
            stmt.executeUpdate(deleteVectorsSql);
            stmt.executeUpdate(deleteFilesSql);
        } catch (SQLException e) {
            System.err.println("Clear all chunks failed: " + e.getMessage());
        }
    }

    // 将 List<Double> 转换为字符串表示形式
    private String vectorToString(List<Double> vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector.get(i));
        }
        return sb.toString();
    }

    // 截断文本
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
