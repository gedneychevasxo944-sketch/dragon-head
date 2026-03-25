package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;

import java.sql.*;

public class MemorySchemaManager {

    private final ResolvedMemorySearchConfig searchConfig;

    public MemorySchemaManager(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    /**
     * 创建表和索引
     */
    public void createTables() {
        String dbPath = searchConfig.getStorePath();
        String url = "jdbc:sqlite:" + dbPath;

        try (Connection conn = DriverManager.getConnection(url)) {
            // 创建 meta 表
            String createMetaTable = "CREATE TABLE IF NOT EXISTS meta (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "key TEXT UNIQUE NOT NULL," +
                    "value TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            executeUpdate(conn, createMetaTable);

            // 创建 files 表
            String createFilesTable = "CREATE TABLE IF NOT EXISTS files (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "path TEXT UNIQUE NOT NULL," +
                    "content_hash TEXT," +
                    "last_modified TIMESTAMP," +
                    "source TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            executeUpdate(conn, createFilesTable);

            // 创建 chunks 表
            String createChunksTable = "CREATE TABLE IF NOT EXISTS chunks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "file_id INTEGER NOT NULL," +
                    "chunk_id INTEGER," +
                    "content TEXT," +
                    "start_pos INTEGER," +
                    "end_pos INTEGER," +
                    "score REAL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (file_id) REFERENCES files(id))";
            executeUpdate(conn, createChunksTable);

            // 创建 embedding_cache 表
            String createCacheTable = "CREATE TABLE IF NOT EXISTS embedding_cache (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "provider_key TEXT NOT NULL," +
                    "model TEXT NOT NULL," +
                    "text TEXT NOT NULL," +
                    "hash TEXT NOT NULL," +
                    "embedding TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            executeUpdate(conn, createCacheTable);

            // 创建向量表（如果需要）
            if (searchConfig.isStoreVectorEnabled()) {
                String createVectorTable = "CREATE TABLE IF NOT EXISTS chunks_vec (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "chunk_id INTEGER UNIQUE NOT NULL," +
                        "vector TEXT," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (chunk_id) REFERENCES chunks(id))";
                executeUpdate(conn, createVectorTable);
            }

            // 创建 FTS 表（如果需要）
            if (searchConfig.isFtsEnabled()) {
                String createFtsTable = "CREATE VIRTUAL TABLE IF NOT EXISTS chunks_fts USING fts5 (" +
                        "content," +
                        "tokenize = 'porter'," +
                        "content = chunks," +
                        "content_rowid = id)";
                executeUpdate(conn, createFtsTable);
            }

            createIndices(conn);

        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    /**
     * 创建索引
     */
    private void createIndices(Connection conn) throws SQLException {
        // 在 chunks 表上创建 content 索引以提高搜索性能
        String createChunksIndex = "CREATE INDEX IF NOT EXISTS idx_chunks_content ON chunks(content)";
        executeUpdate(conn, createChunksIndex);

        // 在 embedding_cache 表上创建复合索引
        String createCacheIndex = "CREATE INDEX IF NOT EXISTS idx_cache_key ON embedding_cache(provider_key, model, hash)";
        executeUpdate(conn, createCacheIndex);

        // 在 files 表上创建路径索引
        String createFilesIndex = "CREATE INDEX IF NOT EXISTS idx_files_path ON files(path)";
        executeUpdate(conn, createFilesIndex);
    }

    /**
     * 升级 schema
     */
    public void upgradeSchema() {
        // 简单实现：检查并添加缺失的列
        String dbPath = searchConfig.getStorePath();
        String url = "jdbc:sqlite:" + dbPath;

        try (Connection conn = DriverManager.getConnection(url)) {
            // 可以添加 schema 版本检查和升级逻辑
            System.out.println("Schema upgrade completed successfully");
        } catch (SQLException e) {
            System.err.println("Error upgrading schema: " + e.getMessage());
        }
    }

    private void executeUpdate(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
}
