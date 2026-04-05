# Character 模块对 Memory 依赖接口文档

## 1. 概述

Character 模块负责管理角色记忆，与 Memory 模块紧密集成，通过一系列接口实现角色记忆的存储、检索和管理功能。

## 2. 接口列表

### 2.1 Character 记忆管理接口

#### 2.1.1 获取角色记忆列表

**接口地址**: `/api/v1/characters/memories`
**请求方法**: GET
**请求参数**:
- `search`: 搜索关键词（可选，搜索角色名称）
- `status`: 同步状态过滤（可选，值为 synced/syncing/pending/failed/disabled/all）
- `healthStatus`: 健康状态过滤（可选，值为 healthy/warning/error/unknown/all）
- `page`: 页码（可选，默认 1）
- `pageSize`: 每页大小（可选，默认 20）

**响应数据**:
```typescript
interface PageResponse<CharacterMemory> {
  list: CharacterMemory[];
  total: number;
  page: number;
  pageSize: number;
}

interface CharacterMemory {
  id: string;
  characterId: string;
  characterName: string;
  characterAvatar?: string;
  status: "synced" | "syncing" | "pending" | "failed" | "disabled";
  healthStatus: "healthy" | "warning" | "error" | "unknown";
  knowledgeItemsCount: number;
  sessionRecordsCount: number;
  lastSyncAt: string;
  hitCount: number;
  totalChunks: number;
  errorMessage?: string;
  mountedFiles?: MemoryFile[];
}
```

#### 2.1.2 获取角色记忆详情

**接口地址**: `/api/v1/characters/{characterId}/memory`
**请求方法**: GET
**路径参数**:
- `characterId`: 角色 ID

**响应数据**: `CharacterMemory`

#### 2.1.3 获取角色记忆内容

**接口地址**: `/api/v1/characters/{characterId}/memory/content`
**请求方法**: GET
**路径参数**:
- `characterId`: 角色 ID

**请求参数**:
- `search`: 搜索关键词（可选）
- `sourceType`: 来源类型过滤（可选，值为 document/chat/manual/imported/generated/all）
- `indexedStatus`: 索引状态过滤（可选，值为 indexed/pending/failed/all）
- `tags`: 标签过滤（可选，多个用逗号分隔）
- `page`: 页码（可选，默认 1）
- `pageSize`: 每页大小（可选，默认 20）

**响应数据**:
```typescript
interface PageResponse<KnowledgeItem> {
  list: KnowledgeItem[];
  total: number;
  page: number;
  pageSize: number;
}

interface KnowledgeItem {
  id: string;
  fileId?: string;
  title: string;
  scope: "character" | "workspace";
  ownerId: string;
  ownerName: string;
  sourceType: "document" | "chat" | "manual" | "imported" | "generated";
  tags: string[];
  summary: string;
  content: string;
  indexedStatus: "indexed" | "pending" | "failed";
  lastUpdateAt: string;
  hitCount: number;
  relations: string[];
}
```

#### 2.1.4 同步角色记忆

**接口地址**: `/api/v1/characters/{characterId}/memory/sync`
**请求方法**: POST
**路径参数**:
- `characterId`: 角色 ID

**响应数据**: `{ success: boolean, message?: string }`

#### 2.1.5 获取角色记忆配置

**接口地址**: `/api/v1/characters/{characterId}/memory/config`
**请求方法**: GET
**路径参数**:
- `characterId`: 角色 ID

**响应数据**:
```typescript
interface CharacterMemoryConfig {
  characterId: string;
  enabled: boolean;
  privateMemoryEnabled: boolean;
  defaultSources: string[];
  queryParams: {
    maxResults: number;
    minScore: number;
    includeSessionRecords: boolean;
  };
  session沉淀: boolean;
  preferences: Record<string, string>;
}
```

#### 2.1.6 更新角色记忆配置

**接口地址**: `/api/v1/characters/{characterId}/memory/config`
**请求方法**: PUT
**路径参数**:
- `characterId`: 角色 ID

**请求数据**:
```typescript
interface UpdateCharacterMemoryConfigRequest {
  enabled?: boolean;
  privateMemoryEnabled?: boolean;
  defaultSources?: string[];
  queryParams?: {
    maxResults?: number;
    minScore?: number;
    includeSessionRecords?: boolean;
  };
  session沉淀?: boolean;
  preferences?: Record<string, string>;
}
```

**响应数据**: `CharacterMemoryConfig`

#### 2.1.7 添加角色记忆内容

**接口地址**: `/api/v1/characters/{characterId}/memory/content`
**请求方法**: POST
**路径参数**:
- `characterId`: 角色 ID

**请求数据**:
```typescript
interface CreateCharacterKnowledgeItemRequest {
  title: string;
  content: string;
  summary?: string;
  sourceType: "document" | "chat" | "manual" | "imported" | "generated";
  tags?: string[];
}
```

**响应数据**: `KnowledgeItem`

#### 2.1.8 更新角色记忆内容

**接口地址**: `/api/v1/characters/{characterId}/memory/content/{itemId}`
**请求方法**: PUT
**路径参数**:
- `characterId`: 角色 ID
- `itemId`: 知识项 ID

**请求数据**:
```typescript
interface UpdateCharacterKnowledgeItemRequest {
  title?: string;
  content?: string;
  summary?: string;
  tags?: string[];
  indexedStatus?: "indexed" | "pending" | "failed";
}
```

**响应数据**: `KnowledgeItem`

#### 2.1.9 删除角色记忆内容

**接口地址**: `/api/v1/characters/{characterId}/memory/content/{itemId}`
**请求方法**: DELETE
**路径参数**:
- `characterId`: 角色 ID
- `itemId`: 知识项 ID

**响应数据**: `{ success: boolean }`

---

### 2.2 Character 记忆检索接口

#### 2.2.1 检索角色记忆

**接口地址**: `/api/v1/characters/{characterId}/memory/retrieval`
**请求方法**: POST
**路径参数**:
- `characterId`: 角色 ID

**请求数据**:
```typescript
interface CharacterRetrievalRequest {
  query: string;
  maxResults?: number;
  minScore?: number;
  includeSessionRecords?: boolean;
  sourceTypes?: string[];
  tags?: string[];
}
```

**响应数据**:
```typescript
interface CharacterRetrievalResponse {
  results: RetrievalResult[];
  total: number;
  latency: number;
}

interface RetrievalResult {
  id: string;
  path: string;
  source: string;
  score: number;
  startLine: number;
  endLine: number;
  snippet: string;
  citation?: string;
  fileId?: string;
  chunkId?: string;
}
```

#### 2.2.2 角色记忆检索测试

**接口地址**: `/api/v1/characters/{characterId}/memory/retrieval/test`
**请求方法**: POST
**路径参数**:
- `characterId`: 角色 ID

**请求数据**:
```typescript
interface CharacterRetrievalTestRequest {
  query: string;
  expectedResults?: string[];
  maxResults?: number;
  minScore?: number;
}
```

**响应数据**:
```typescript
interface CharacterRetrievalTestResponse {
  results: RetrievalResult[];
  total: number;
  latency: number;
  accuracy?: number;
  precision?: number;
  recall?: number;
}
```

---

### 2.3 Character 记忆运维接口

#### 2.3.1 获取角色记忆运行时状态

**接口地址**: `/api/v1/characters/{characterId}/memory/status`
**请求方法**: GET
**路径参数**:
- `characterId`: 角色 ID

**响应数据**:
```typescript
interface RuntimeStatus {
  backend: "connected" | "disconnected" | "error";
  provider: string;
  model: string;
  fallbackEnabled: boolean;
  totalFiles: number;
  totalChunks: number;
  dirtyCount: number;
  cacheStatus: "active" | "inactive" | "error";
  ftsStatus: "active" | "inactive" | "error";
  vectorStatus: "active" | "inactive" | "error";
  batchStatus: "idle" | "processing" | "error";
  lastSyncTime: string;
  recentFailures: string[];
}
```

#### 2.3.2 执行角色记忆系统检查

**接口地址**: `/api/v1/characters/{characterId}/memory/probe`
**请求方法**: POST
**路径参数**:
- `characterId`: 角色 ID

**响应数据**:
```typescript
interface ProbeResult {
  success: boolean;
  latency?: number;
  error?: string;
  details?: string;
}
```

#### 2.3.3 重建角色记忆索引

**接口地址**: `/api/v1/characters/{characterId}/memory/reindex`
**请求方法**: POST
**路径参数**:
- `characterId`: 角色 ID

**响应数据**: `{ success: boolean, message?: string }`

#### 2.3.4 清除角色记忆缓存

**接口地址**: `/api/v1/characters/{characterId}/memory/clear-cache`
**请求方法**: POST
**路径参数**:
- `characterId`: 角色 ID

**响应数据**: `{ success: boolean, clearedCount: number }`

---

## 3. Character 与 Memory 的绑定接口

#### 3.1 获取角色绑定的记忆文件

**接口地址**: `/api/v1/characters/{characterId}/memory/bindings`
**请求方法**: GET
**路径参数**:
- `characterId`: 角色 ID

**响应数据**:
```typescript
interface CharacterMemoryBindingsResponse {
  bindings: Binding[];
  total: number;
}
```

#### 3.2 绑定记忆文件到角色

**接口地址**: `/api/v1/characters/{characterId}/memory/bindings`
**请求方法**: POST
**路径参数**:
- `characterId`: 角色 ID

**请求数据**:
```typescript
interface BindMemoryToCharacterRequest {
  fileId: string;
  mountType: "full" | "selective" | "rule";
  selectedChunkIds?: string[];
  mountRules?: MountRule[];
}
```

**响应数据**: `Binding`

#### 3.3 解绑角色记忆文件

**接口地址**: `/api/v1/characters/{characterId}/memory/bindings/{bindingId}`
**请求方法**: DELETE
**路径参数**:
- `characterId`: 角色 ID
- `bindingId`: 绑定 ID

**响应数据**: `{ success: boolean }`

---

## 4. 错误码

| 错误码 | 描述 |
|--------|------|
| 0 | 成功 |
| 1001 | 参数无效 |
| 1002 | 角色未找到 |
| 1003 | 权限不足 |
| 1004 | 记忆内容未找到 |
| 1005 | 操作失败 |
| 2001 | 记忆同步失败 |
| 2002 | 记忆索引失败 |
| 3001 | 检索服务不可用 |
| 3002 | 检索查询失败 |
| 4001 | 系统错误 |

---

## 5. 使用示例

### 示例 1: 获取角色记忆列表

```bash
curl -X GET "http://localhost:8080/api/v1/characters/memories?page=1&pageSize=5&status=synced"
```

### 示例 2: 获取角色记忆详情

```bash
curl -X GET "http://localhost:8080/api/v1/characters/char-123/memory"
```

### 示例 3: 检索角色记忆

```bash
curl -X POST "http://localhost:8080/api/v1/characters/char-123/memory/retrieval" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "用户登录流程",
    "maxResults": 3,
    "minScore": 0.8
  }'
```

---

## 6. 设计说明

### 6.1 Character Memory 架构

Character Memory 采用双层架构：
1. **CharacterMemory**: 角色记忆的元数据和状态管理
2. **KnowledgeItem**: 角色记忆的具体内容项

### 6.2 数据流动

```
外部数据源 → SourceDocument → MemoryFile → MemoryChunk → KnowledgeItem → CharacterMemory
```

### 6.3 索引策略

- 角色记忆内容会自动建立全文索引和向量索引
- 支持增量更新和重建索引
- 索引状态跟踪（indexed/pending/failed）

### 6.4 缓存策略

- 角色记忆内容支持本地缓存和分布式缓存
- 缓存失效机制：内容更新时自动失效
- 支持手动清除缓存

---

## 7. 性能优化建议

### 7.1 查询优化

1. 合理使用分页参数，避免一次性查询大量数据
2. 为频繁查询的字段添加索引
3. 实现查询结果缓存机制

### 7.2 同步优化

1. 支持增量同步，只同步变更内容
2. 同步过程支持断点续传
3. 同步状态实时反馈，提供进度指示

### 7.3 检索优化

1. 实现查询意图识别和语义理解
2. 优化检索结果排序算法
3. 提供查询建议和自动补全功能

---

## 8. 安全考虑

### 8.1 数据安全性

- 角色记忆内容加密存储
- 传输过程使用 HTTPS
- 定期数据备份和恢复机制

### 8.2 访问控制

- 基于角色的访问控制（RBAC）
- 资源级权限管理
- 操作审计和日志记录

### 8.3 数据一致性

- 使用事务保证数据一致性
- 实现数据版本控制
- 提供数据修复和校验工具
