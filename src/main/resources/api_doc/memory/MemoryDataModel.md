# Memory 模块数据模型文档

## 1. 概述

Memory 模块采用层次化数据模型，将记忆内容组织为数据源、文件和片段三个核心层次，并通过绑定关系与 Character 和 Workspace 关联。

## 2. 核心数据实体

### 2.1 SourceDocument（数据源）

**用途**: 代表外部数据源的抽象，是记忆内容的来源。

**字段说明**:

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 数据源唯一标识符 |
| title | string | 是 | 数据源标题 |
| sourcePath | string | 是 | 数据源路径（文件系统路径或 URL） |
| sourceType | string | 是 | 数据源类型：file/url/api/chat/fused/character_memory/workspace_memory |
| backend | string | 否 | 后端存储位置 |
| provider | string | 否 | 数据源提供者 |
| enabled | boolean | 否 | 是否启用（默认 true） |
| status | string | 否 | 同步状态：active/error/disabled/syncing |
| lastIndexedAt | string | 否 | 最后索引时间 |
| itemCount | number | 否 | 项目数量 |
| fileCount | number | 否 | 文件数量 |
| errorMessage | string | 否 | 错误信息 |
| isFusedSource | boolean | 否 | 是否为融合数据源 |
| isBuiltIn | boolean | 否 | 是否为内置数据源 |
| createdAt | string | 是 | 创建时间 |
| updatedAt | string | 是 | 更新时间 |

### 2.2 MemoryFile（记忆文件）

**用途**: 代表从数据源中解析出的文件，是记忆片段的容器。

**字段说明**:

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 文件唯一标识符 |
| sourceId | string | 是 | 所属数据源 ID |
| title | string | 是 | 文件标题 |
| description | string | 否 | 文件描述 |
| filePath | string | 是 | 文件路径 |
| fileType | string | 是 | 文件类型：markdown/json/text/other |
| chunkCount | number | 否 | 包含的片段数量 |
| totalSize | number | 否 | 文件大小（字节） |
| syncStatus | string | 否 | 同步状态：synced/syncing/pending/failed/disabled |
| healthStatus | string | 否 | 健康状态：healthy/warning/error/unknown |
| bindings | Binding[] | 否 | 与 Character/Workspace 的绑定关系 |
| lastSyncAt | string | 否 | 最后同步时间 |
| createdAt | string | 是 | 创建时间 |
| updatedAt | string | 是 | 更新时间 |

### 2.3 MemoryChunk（记忆片段）

**用途**: 代表记忆文件中的最小可检索单元，是内容的基本片段。

**字段说明**:

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 片段唯一标识符 |
| fileId | string | 是 | 所属文件 ID |
| title | string | 是 | 片段标题 |
| content | string | 是 | 片段内容 |
| summary | string | 否 | 内容摘要 |
| tags | string[] | 否 | 标签列表 |
| indexedStatus | string | 否 | 索引状态：indexed/pending/failed |
| relations | string[] | 否 | 关联片段 ID 列表 |
| sourceLocation | object | 否 | 源文件位置信息 |
| fusedFrom | FusedFrom[] | 否 | 融合来源信息（仅融合片段有） |
| createdAt | string | 是 | 创建时间 |
| updatedAt | string | 是 | 更新时间 |

### 2.4 Binding（绑定关系）

**用途**: 表示 MemoryFile 与 Character 或 Workspace 之间的关联关系。

**字段说明**:

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 绑定唯一标识符 |
| fileId | string | 是 | 关联的文件 ID |
| targetType | string | 是 | 目标类型：character/workspace |
| targetId | string | 是 | 目标 ID |
| targetName | string | 是 | 目标名称 |
| mountType | string | 是 | 挂载类型：full/selective/rule |
| selectedChunkIds | string[] | 否 | 选择性挂载的片段 ID 列表 |
| mountRules | MountRule[] | 否 | 规则挂载的规则列表 |
| mountedAt | string | 是 | 挂载时间 |
| mountedBy | string | 是 | 挂载人 |

### 2.5 MountRule（挂载规则）

**用途**: 定义规则挂载时的内容选择条件。

**字段说明**:

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 规则唯一标识符 |
| type | string | 是 | 规则类型：tag/content/time |
| operator | string | 是 | 操作符：include/exclude |
| value | string | 是 | 规则值（标签名、关键词或时间表达式） |

### 2.6 FusedFrom（融合来源）

**用途**: 表示融合记忆片段的原始来源信息。

**字段说明**:

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| chunkId | string | 是 | 原始片段 ID |
| chunkTitle | string | 是 | 原始片段标题 |
| sourceName | string | 是 | 来源名称 |
| fusedAt | string | 是 | 融合时间 |

---

## 3. 遗留数据模型（保持兼容性）

### 3.1 CharacterMemory（角色记忆）

**用途**: 代表角色的记忆，包含角色的所有记忆内容。

**字段说明**:

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 角色记忆唯一标识符 |
| characterId | string | 是 | 角色 ID |
| characterName | string | 是 | 角色名称 |
| characterAvatar | string | 否 | 角色头像 |
| status | string | 否 | 同步状态：synced/syncing/pending/failed/disabled |
| healthStatus | string | 否 | 健康状态：healthy/warning/error/unknown |
| knowledgeItemsCount | number | 否 | 知识项数量 |
| sessionRecordsCount | number | 否 | 会话记录数量 |
| lastSyncAt | string | 否 | 最后同步时间 |
| hitCount | number | 否 | 命中次数 |
| totalChunks | number | 否 | 总片段数 |
| errorMessage | string | 否 | 错误信息 |
| mountedFiles | MemoryFile[] | 否 | 挂载的记忆文件列表 |

### 3.2 WorkspaceMemory（工作空间记忆）

**用途**: 代表工作空间的记忆，包含团队的共享记忆内容。

**字段说明**:

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 工作空间记忆唯一标识符 |
| workspaceId | string | 是 | 工作空间 ID |
| workspaceName | string | 是 | 工作空间名称 |
| status | string | 否 | 同步状态：synced/syncing/pending/failed/disabled |
| healthStatus | string | 否 | 健康状态：healthy/warning/error/unknown |
| sharedItemsCount | number | 否 | 共享项数量 |
| sourceDocumentsCount | number | 否 | 数据源数量 |
| lastSyncAt | string | 否 | 最后同步时间 |
| errorCount | number | 否 | 错误数 |
| totalChunks | number | 否 | 总片段数 |
| enabledSources | string[] | 否 | 启用的数据源 ID 列表 |
| hitEffect | number | 否 | 命中效果 |
| errorMessage | string | 否 | 错误信息 |
| mountedFiles | MemoryFile[] | 否 | 挂载的记忆文件列表 |

### 3.3 KnowledgeItem（知识项）

**用途**: 代表记忆中的知识项，是内容的逻辑单元。

**字段说明**:

| 字段名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | string | 是 | 知识项唯一标识符 |
| fileId | string | 否 | 所属文件 ID |
| title | string | 是 | 知识项标题 |
| scope | string | 是 | 范围：character/workspace |
| ownerId | string | 是 | 所有者 ID |
| ownerName | string | 是 | 所有者名称 |
| sourceType | string | 是 | 来源类型：document/chat/manual/imported/generated |
| tags | string[] | 否 | 标签列表 |
| summary | string | 否 | 内容摘要 |
| content | string | 是 | 知识内容 |
| indexedStatus | string | 否 | 索引状态：indexed/pending/failed |
| lastUpdateAt | string | 否 | 最后更新时间 |
| hitCount | number | 否 | 命中次数 |
| relations | string[] | 否 | 关联知识项 ID 列表 |

---

## 4. 数据关联关系

### 4.1 主要实体关系图

```
SourceDocument (1) → (N) MemoryFile (1) → (N) MemoryChunk
    ↑                                      ↓
    └──────────────────────────────────────┘
```

**说明**:
1. 一个 SourceDocument 包含多个 MemoryFile
2. 一个 MemoryFile 包含多个 MemoryChunk
3. SourceDocument 和 MemoryChunk 之间可以有直接关联（用于快速访问）

### 4.2 绑定关系图

```
Character (1) ← (N) Binding (N) → (1) MemoryFile (1) → (N) MemoryChunk
Workspace (1) ← (N) Binding (N) → (1) MemoryFile (1) → (N) MemoryChunk
```

**说明**:
1. 一个 Character/Workspace 可以绑定多个 MemoryFile
2. 一个 MemoryFile 可以被多个 Character/Workspace 绑定
3. 绑定关系通过 Binding 实体实现

### 4.3 数据流动示意图

```
外部数据源 → 解析 → SourceDocument → 解析 → MemoryFile → 解析 → MemoryChunk
    ↓                                     ↓
    └─────────────────────────────────────┘
    ↑                                     ↑
    同步 ← 索引 ← MemoryChunk ← 绑定 ← MemoryFile ← 绑定 ← Character/Workspace
```

**说明**:
1. 外部数据源通过同步过程解析为 SourceDocument
2. SourceDocument 解析为 MemoryFile
3. MemoryFile 解析为 MemoryChunk
4. MemoryChunk 建立索引，支持检索
5. Character/Workspace 可以绑定 MemoryFile，获取其内容

---

## 5. 存储设计

### 5.1 数据库表结构

#### 5.1.1 source_documents 表

```sql
CREATE TABLE source_documents (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    source_path TEXT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    backend VARCHAR(255),
    provider VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    status VARCHAR(32) DEFAULT 'active',
    last_indexed_at TIMESTAMP,
    item_count INT DEFAULT 0,
    file_count INT DEFAULT 0,
    error_message TEXT,
    is_fused_source BOOLEAN DEFAULT FALSE,
    is_built_in BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_source_documents_title ON source_documents(title);
CREATE INDEX idx_source_documents_status ON source_documents(status);
CREATE INDEX idx_source_documents_source_type ON source_documents(source_type);
```

#### 5.1.2 memory_files 表

```sql
CREATE TABLE memory_files (
    id VARCHAR(64) PRIMARY KEY,
    source_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    file_path TEXT NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    chunk_count INT DEFAULT 0,
    total_size BIGINT DEFAULT 0,
    sync_status VARCHAR(32) DEFAULT 'pending',
    health_status VARCHAR(32) DEFAULT 'unknown',
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (source_id) REFERENCES source_documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_memory_files_source_id ON memory_files(source_id);
CREATE INDEX idx_memory_files_title ON memory_files(title);
CREATE INDEX idx_memory_files_sync_status ON memory_files(sync_status);
```

#### 5.1.3 memory_chunks 表

```sql
CREATE TABLE memory_chunks (
    id VARCHAR(64) PRIMARY KEY,
    file_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    summary TEXT,
    tags JSON,
    indexed_status VARCHAR(32) DEFAULT 'pending',
    relations JSON,
    source_location JSON,
    fused_from JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES memory_files(id) ON DELETE CASCADE
);

CREATE INDEX idx_memory_chunks_file_id ON memory_chunks(file_id);
CREATE INDEX idx_memory_chunks_indexed_status ON memory_chunks(indexed_status);
```

#### 5.1.4 bindings 表

```sql
CREATE TABLE bindings (
    id VARCHAR(64) PRIMARY KEY,
    file_id VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    target_name VARCHAR(255) NOT NULL,
    mount_type VARCHAR(32) NOT NULL,
    selected_chunk_ids JSON,
    mount_rules JSON,
    mounted_at TIMESTAMP,
    mounted_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES memory_files(id) ON DELETE CASCADE
);

CREATE INDEX idx_bindings_file_id ON bindings(file_id);
CREATE INDEX idx_bindings_target ON bindings(target_type, target_id);
CREATE INDEX idx_bindings_mount_type ON bindings(mount_type);
```

---

## 6. 索引设计

### 6.1 全文索引

- **MemoryChunk.content**: 使用倒排索引，支持全文搜索
- **MemoryChunk.title**: 使用倒排索引，支持标题搜索
- **MemoryChunk.tags**: 使用倒排索引，支持标签搜索

### 6.2 向量索引

- **MemoryChunk.embedding**: 使用向量数据库（如 Pinecone 或 Milvus），支持语义搜索
- **相似度计算**: 使用余弦相似度，支持检索与查询语义相似的内容

### 6.3 元数据索引

- **SourceDocument.status**: 用于状态过滤
- **SourceDocument.source_type**: 用于类型过滤
- **MemoryFile.sync_status**: 用于同步状态过滤
- **MemoryChunk.indexed_status**: 用于索引状态过滤
- **Binding.target_type/target_id**: 用于绑定关系查询

---

## 7. 数据约束和验证

### 7.1 唯一性约束

- SourceDocument.id: 数据源唯一标识符
- MemoryFile.id: 文件唯一标识符
- MemoryChunk.id: 片段唯一标识符
- Binding.id: 绑定关系唯一标识符

### 7.2 引用完整性

- MemoryFile.source_id → SourceDocument.id
- MemoryChunk.file_id → MemoryFile.id
- Binding.file_id → MemoryFile.id

### 7.3 数据格式验证

```typescript
// SourceDocument 验证
export const SourceDocumentSchema = z.object({
  title: z.string().min(1).max(255),
  sourcePath: z.string().min(1),
  sourceType: z.enum(['file', 'url', 'api', 'chat', 'fused', 'character_memory', 'workspace_memory']),
  enabled: z.boolean().optional(),
  status: z.enum(['active', 'error', 'disabled', 'syncing']).optional(),
  isFusedSource: z.boolean().optional(),
  isBuiltIn: z.boolean().optional()
});

// MemoryFile 验证
export const MemoryFileSchema = z.object({
  title: z.string().min(1).max(255),
  sourceId: z.string().min(1),
  description: z.string().max(1000).optional(),
  filePath: z.string().min(1),
  fileType: z.enum(['markdown', 'json', 'text', 'other']),
  syncStatus: z.enum(['synced', 'syncing', 'pending', 'failed', 'disabled']).optional(),
  healthStatus: z.enum(['healthy', 'warning', 'error', 'unknown']).optional()
});

// MemoryChunk 验证
export const MemoryChunkSchema = z.object({
  title: z.string().min(1).max(255),
  fileId: z.string().min(1),
  content: z.string().min(1),
  summary: z.string().max(1000).optional(),
  tags: z.array(z.string()).optional(),
  indexedStatus: z.enum(['indexed', 'pending', 'failed']).optional(),
  relations: z.array(z.string()).optional(),
  sourceLocation: z.object({
    startLine: z.number().min(0),
    endLine: z.number().min(0)
  }).optional(),
  fusedFrom: z.array(z.object({
    chunkId: z.string(),
    chunkTitle: z.string(),
    sourceName: z.string(),
    fusedAt: z.string()
  })).optional()
});

// Binding 验证
export const BindingSchema = z.object({
  fileId: z.string().min(1),
  targetType: z.enum(['character', 'workspace']),
  targetId: z.string().min(1),
  targetName: z.string().min(1),
  mountType: z.enum(['full', 'selective', 'rule']),
  selectedChunkIds: z.array(z.string()).optional(),
  mountRules: z.array(z.object({
    type: z.enum(['tag', 'content', 'time']),
    operator: z.enum(['include', 'exclude']),
    value: z.string().min(1)
  })).optional()
});
```

---

## 8. 数据同步机制

### 8.1 同步状态流转

```
pending → syncing → synced
        ↓
      failed
      ↓
    disabled
```

**说明**:
1. `pending`: 待同步状态
2. `syncing`: 同步中状态
3. `synced`: 同步成功状态
4. `failed`: 同步失败状态
5. `disabled`: 禁用状态

### 8.2 同步策略

#### 8.2.1 完全同步

```typescript
export interface FullSyncStrategy {
  type: 'full';
  options?: {
    force: boolean; // 是否强制同步，忽略本地修改
  };
}
```

#### 8.2.2 增量同步

```typescript
export interface IncrementalSyncStrategy {
  type: 'incremental';
  options?: {
    since: string; // 上次同步时间
    checksum: string; // 文件校验和
  };
}
```

#### 8.2.3 定时同步

```typescript
export interface ScheduledSyncStrategy {
  type: 'scheduled';
  options: {
    cron: string; // 定时表达式
  };
}
```

---

## 9. 数据安全考虑

### 9.1 数据加密

- **传输加密**: 使用 HTTPS 加密传输数据
- **存储加密**: 敏感数据（如密码、API 密钥）加密存储
- **静态加密**: 数据库文件加密，防止物理泄漏

### 9.2 访问控制

- **基于角色的访问控制（RBAC）**: 根据用户角色授予不同权限
- **资源级权限**: 对 MemoryFile 和 MemoryChunk 实现精细的权限控制
- **操作审计**: 记录所有数据操作，支持审计和回溯

### 9.3 数据备份和恢复

- **定期备份**: 数据库定期备份，支持自动恢复
- **增量备份**: 仅备份变更数据，减少备份时间
- **灾难恢复**: 支持跨区域备份，提高可用性

---

## 10. 性能优化

### 10.1 查询优化

1. **分页查询**: 使用 limit 和 offset 进行分页
2. **索引覆盖**: 为常用查询字段创建复合索引
3. **查询缓存**: 对频繁查询结果进行缓存
4. **延迟加载**: 对大型内容字段实现延迟加载

### 10.2 存储优化

1. **压缩存储**: 对文本内容进行压缩存储
2. **分库分表**: 对大数据量表进行分库分表
3. **冷热数据分离**: 对活跃数据和历史数据进行分离存储

### 10.3 同步优化

1. **增量同步**: 仅同步变更数据
2. **并发同步**: 支持多个同步任务并行执行
3. **断点续传**: 同步过程中支持断点续传

---

## 11. 数据迁移

### 11.1 版本升级

- 使用数据库版本控制工具（如 Flyway 或 Liquibase）
- 每次 schema 变更创建对应的迁移脚本
- 支持回滚操作，确保数据安全

### 11.2 数据导入导出

- 支持 JSON、CSV、Markdown 等格式的导入导出
- 导入过程中支持数据验证和转换
- 导出过程中支持数据过滤和格式化
