# Memory 模块后端接口文档

## 1. 概述

Memory 模块是系统的核心数据存储和检索组件，负责管理记忆文件、记忆片段、数据源以及与 Character 和 Workspace 的绑定关系。

## 2. 基础信息

- **API 前缀**: `/api/v1/memory`
- **响应格式**: JSON
- **请求格式**: JSON
- **字符编码**: UTF-8
- **时区**: UTC

## 3. 统一响应格式

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

### 错误响应

```json
{
  "code": 1,
  "message": "错误信息",
  "data": null
}
```

## 4. 接口列表

### 4.1 数据源管理接口

#### 4.1.1 获取数据源列表

**接口地址**: `/sources`
**请求方法**: GET
**请求参数**:
- `search`: 搜索关键词（可选）
- `status`: 状态过滤（可选，值为 active/error/disabled/syncing）
- `sourceType`: 源类型过滤（可选，值为 file/url/api/chat/fused/character_memory/workspace_memory）
- `page`: 页码（可选，默认 1）
- `pageSize`: 每页大小（可选，默认 20）

**响应数据**:
```typescript
interface PageResponse<T> {
  list: SourceDocument[];
  total: number;
  page: number;
  pageSize: number;
}

interface SourceDocument {
  id: string;
  title: string;
  sourcePath: string;
  sourceType: "file" | "url" | "api" | "chat" | "fused" | "character_memory" | "workspace_memory";
  backend: string;
  provider: string;
  enabled: boolean;
  status: "active" | "error" | "disabled" | "syncing";
  lastIndexedAt: string;
  itemCount: number;
  fileCount: number;
  errorMessage?: string;
  files?: MemoryFile[];
  isFusedSource?: boolean;
  isBuiltIn?: boolean;
  createdAt: string;
  updatedAt: string;
}
```

#### 4.1.2 获取数据源详情

**接口地址**: `/sources/{sourceId}`
**请求方法**: GET
**路径参数**:
- `sourceId`: 数据源 ID

**响应数据**: `SourceDocument`

#### 4.1.3 创建数据源

**接口地址**: `/sources`
**请求方法**: POST
**请求数据**:
```typescript
interface CreateSourceRequest {
  title: string;
  sourcePath: string;
  sourceType: "file" | "url" | "api" | "chat";
  backend?: string;
  provider?: string;
  enabled?: boolean;
}
```

**响应数据**: `SourceDocument`

#### 4.1.4 更新数据源

**接口地址**: `/sources/{sourceId}`
**请求方法**: PUT
**路径参数**:
- `sourceId`: 数据源 ID

**请求数据**:
```typescript
interface UpdateSourceRequest {
  title?: string;
  sourcePath?: string;
  sourceType?: "file" | "url" | "api" | "chat";
  backend?: string;
  provider?: string;
  enabled?: boolean;
  status?: "active" | "error" | "disabled" | "syncing";
}
```

**响应数据**: `SourceDocument`

#### 4.1.5 删除数据源

**接口地址**: `/sources/{sourceId}`
**请求方法**: DELETE
**路径参数**:
- `sourceId`: 数据源 ID

**响应数据**: `{ success: boolean }`

#### 4.1.6 同步数据源

**接口地址**: `/sources/{sourceId}/sync`
**请求方法**: POST
**路径参数**:
- `sourceId`: 数据源 ID

**响应数据**: `{ success: boolean, message?: string }`

---

### 4.2 记忆文件管理接口

#### 4.2.1 获取文件列表

**接口地址**: `/files`
**请求方法**: GET
**请求参数**:
- `sourceId`: 数据源 ID（可选，若提供则只返回该数据源的文件）
- `page`: 页码（可选，默认 1）
- `pageSize`: 每页大小（可选，默认 20）

**响应数据**:
```typescript
interface PageResponse<MemoryFile> {
  list: MemoryFile[];
  total: number;
  page: number;
  pageSize: number;
}

interface MemoryFile {
  id: string;
  sourceId: string;
  title: string;
  description?: string;
  filePath: string;
  fileType: "markdown" | "json" | "text" | "other";
  chunkCount: number;
  totalSize: number;
  syncStatus: "synced" | "syncing" | "pending" | "failed" | "disabled";
  healthStatus: "healthy" | "warning" | "error" | "unknown";
  bindings: Binding[];
  lastSyncAt: string;
  createdAt: string;
  updatedAt: string;
}
```

#### 4.2.2 获取文件详情

**接口地址**: `/files/{fileId}`
**请求方法**: GET
**路径参数**:
- `fileId`: 文件 ID

**响应数据**: `MemoryFile`

#### 4.2.3 获取文件的片段

**接口地址**: `/files/{fileId}/chunks`
**请求方法**: GET
**路径参数**:
- `fileId`: 文件 ID

**请求参数**:
- `indexedStatus`: 索引状态过滤（可选，值为 indexed/pending/failed）
- `page`: 页码（可选，默认 1）
- `pageSize`: 每页大小（可选，默认 20）

**响应数据**:
```typescript
interface PageResponse<MemoryChunk> {
  list: MemoryChunk[];
  total: number;
  page: number;
  pageSize: number;
}
```

#### 4.2.4 同步文件

**接口地址**: `/files/{fileId}/sync`
**请求方法**: POST
**路径参数**:
- `fileId`: 文件 ID

**响应数据**: `{ success: boolean, message?: string }`

---

### 4.3 记忆片段管理接口

#### 4.3.1 获取片段列表

**接口地址**: `/chunks`
**请求方法**: GET
**请求参数**:
- `fileId`: 文件 ID（可选，若提供则只返回该文件的片段）
- `sourceId`: 数据源 ID（可选，若提供则只返回该数据源的片段）
- `indexedStatus`: 索引状态过滤（可选，值为 indexed/pending/failed）
- `tags`: 标签过滤（可选，多个用逗号分隔）
- `search`: 搜索关键词（可选，搜索标题、内容和摘要）
- `page`: 页码（可选，默认 1）
- `pageSize`: 每页大小（可选，默认 20）

**响应数据**:
```typescript
interface PageResponse<MemoryChunk> {
  list: MemoryChunk[];
  total: number;
  page: number;
  pageSize: number;
}

interface MemoryChunk {
  id: string;
  fileId: string;
  title: string;
  content: string;
  summary: string;
  tags: string[];
  indexedStatus: "indexed" | "pending" | "failed";
  relations: string[];
  sourceLocation?: {
    startLine: number;
    endLine: number;
  };
  createdAt: string;
  updatedAt: string;
  fusedFrom?: FusedFrom[];
}

interface FusedFrom {
  chunkId: string;
  chunkTitle: string;
  sourceName: string;
  fusedAt: string;
}
```

#### 4.3.2 获取片段详情

**接口地址**: `/chunks/{chunkId}`
**请求方法**: GET
**路径参数**:
- `chunkId`: 片段 ID

**响应数据**: `MemoryChunk`

#### 4.3.3 创建片段

**接口地址**: `/chunks`
**请求方法**: POST
**请求数据**:
```typescript
interface CreateChunkRequest {
  fileId: string;
  title: string;
  content: string;
  summary?: string;
  tags?: string[];
  sourceLocation?: {
    startLine: number;
    endLine: number;
  };
}
```

**响应数据**: `MemoryChunk`

#### 4.3.4 更新片段

**接口地址**: `/chunks/{chunkId}`
**请求方法**: PUT
**路径参数**:
- `chunkId`: 片段 ID

**请求数据**:
```typescript
interface UpdateChunkRequest {
  title?: string;
  content?: string;
  summary?: string;
  tags?: string[];
  indexedStatus?: "indexed" | "pending" | "failed";
  sourceLocation?: {
    startLine: number;
    endLine: number;
  };
}
```

**响应数据**: `MemoryChunk`

#### 4.3.5 删除片段

**接口地址**: `/chunks/{chunkId}`
**请求方法**: DELETE
**路径参数**:
- `chunkId`: 片段 ID

**响应数据**: `{ success: boolean }`

#### 4.3.6 批量删除片段

**接口地址**: `/chunks/batch`
**请求方法**: DELETE
**请求数据**:
```typescript
interface BatchDeleteChunksRequest {
  chunkIds: string[];
}
```

**响应数据**: `{ success: boolean, deletedCount: number }`

#### 4.3.7 批量更新片段索引状态

**接口地址**: `/chunks/batch/index`
**请求方法**: PUT
**请求数据**:
```typescript
interface BatchUpdateIndexStatusRequest {
  chunkIds: string[];
  indexedStatus: "indexed" | "pending" | "failed";
}
```

**响应数据**: `{ success: boolean, updatedCount: number }`

---

### 4.4 绑定关系管理接口

#### 4.4.1 获取绑定列表

**接口地址**: `/bindings`
**请求方法**: GET
**请求参数**:
- `fileId`: 文件 ID（可选）
- `targetType`: 目标类型（可选，值为 character/workspace）
- `targetId`: 目标 ID（可选）
- `page`: 页码（可选，默认 1）
- `pageSize`: 每页大小（可选，默认 20）

**响应数据**:
```typescript
interface PageResponse<Binding> {
  list: Binding[];
  total: number;
  page: number;
  pageSize: number;
}

interface Binding {
  id: string;
  fileId: string;
  targetType: "character" | "workspace";
  targetId: string;
  targetName: string;
  mountType: "full" | "selective" | "rule";
  selectedChunkIds?: string[];
  mountRules?: MountRule[];
  mountedAt: string;
  mountedBy: string;
}

interface MountRule {
  id: string;
  type: "tag" | "content" | "time";
  operator: "include" | "exclude";
  value: string;
}
```

#### 4.4.2 创建绑定

**接口地址**: `/bindings`
**请求方法**: POST
**请求数据**:
```typescript
interface CreateBindingRequest {
  fileId: string;
  targetType: "character" | "workspace";
  targetId: string;
  targetName: string;
  mountType: "full" | "selective" | "rule";
  selectedChunkIds?: string[];
  mountRules?: MountRule[];
}
```

**响应数据**: `Binding`

#### 4.4.3 更新绑定

**接口地址**: `/bindings/{bindingId}`
**请求方法**: PUT
**路径参数**:
- `bindingId`: 绑定 ID

**请求数据**:
```typescript
interface UpdateBindingRequest {
  mountType?: "full" | "selective" | "rule";
  selectedChunkIds?: string[];
  mountRules?: MountRule[];
}
```

**响应数据**: `Binding`

#### 4.4.4 删除绑定

**接口地址**: `/bindings/{bindingId}`
**请求方法**: DELETE
**路径参数**:
- `bindingId`: 绑定 ID

**响应数据**: `{ success: boolean }`

---

### 4.5 角色记忆接口

#### 4.5.1 获取角色记忆列表

**接口地址**: `/characters`
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

#### 4.5.2 获取角色记忆详情

**接口地址**: `/characters/{characterId}`
**请求方法**: GET
**路径参数**:
- `characterId`: 角色 ID

**响应数据**: `CharacterMemory`

#### 4.5.3 获取角色记忆内容

**接口地址**: `/characters/{characterId}/content`
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

#### 4.5.4 同步角色记忆

**接口地址**: `/characters/{characterId}/sync`
**请求方法**: POST
**路径参数**:
- `characterId`: 角色 ID

**响应数据**: `{ success: boolean, message?: string }`

#### 4.5.5 获取角色记忆配置

**接口地址**: `/characters/{characterId}/config`
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

#### 4.5.6 更新角色记忆配置

**接口地址**: `/characters/{characterId}/config`
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

---

### 4.6 工作空间记忆接口

#### 4.6.1 获取工作空间记忆列表

**接口地址**: `/workspaces`
**请求方法**: GET
**请求参数**:
- `search`: 搜索关键词（可选，搜索工作空间名称）
- `status`: 同步状态过滤（可选，值为 synced/syncing/pending/failed/disabled/all）
- `healthStatus`: 健康状态过滤（可选，值为 healthy/warning/error/unknown/all）
- `page`: 页码（可选，默认 1）
- `pageSize`: 每页大小（可选，默认 20）

**响应数据**:
```typescript
interface PageResponse<WorkspaceMemory> {
  list: WorkspaceMemory[];
  total: number;
  page: number;
  pageSize: number;
}

interface WorkspaceMemory {
  id: string;
  workspaceId: string;
  workspaceName: string;
  status: "synced" | "syncing" | "pending" | "failed" | "disabled";
  healthStatus: "healthy" | "warning" | "error" | "unknown";
  sharedItemsCount: number;
  sourceDocumentsCount: number;
  lastSyncAt: string;
  errorCount: number;
  totalChunks: number;
  enabledSources: string[];
  hitEffect?: number;
  errorMessage?: string;
  mountedFiles?: MemoryFile[];
}
```

#### 4.6.2 获取工作空间记忆详情

**接口地址**: `/workspaces/{workspaceId}`
**请求方法**: GET
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**: `WorkspaceMemory`

#### 4.6.3 获取工作空间记忆内容

**接口地址**: `/workspaces/{workspaceId}/content`
**请求方法**: GET
**路径参数**:
- `workspaceId`: 工作空间 ID

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
```

#### 4.6.4 同步工作空间记忆

**接口地址**: `/workspaces/{workspaceId}/sync`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**: `{ success: boolean, message?: string }`

#### 4.6.5 获取工作空间记忆配置

**接口地址**: `/workspaces/{workspaceId}/config`
**请求方法**: GET
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**:
```typescript
interface WorkspaceMemoryConfig {
  workspaceId: string;
  enabled: boolean;
  backend: string;
  provider: string;
  model: string;
  sources: string[];
  extraPaths: string[];
  syncStrategy: "auto" | "manual" | "scheduled";
  queryStrategy: {
    useVector: boolean;
    useFts: boolean;
    useCache: boolean;
  };
}
```

#### 4.6.6 更新工作空间记忆配置

**接口地址**: `/workspaces/{workspaceId}/config`
**请求方法**: PUT
**路径参数**:
- `workspaceId`: 工作空间 ID

**请求数据**:
```typescript
interface UpdateWorkspaceMemoryConfigRequest {
  enabled?: boolean;
  backend?: string;
  provider?: string;
  model?: string;
  sources?: string[];
  extraPaths?: string[];
  syncStrategy?: "auto" | "manual" | "scheduled";
  queryStrategy?: {
    useVector?: boolean;
    useFts?: boolean;
    useCache?: boolean;
  };
}
```

**响应数据**: `WorkspaceMemoryConfig`

---

### 4.7 检索接口

#### 4.7.1 检索记忆内容

**接口地址**: `/retrieval/search`
**请求方法**: POST
**请求数据**:
```typescript
interface RetrievalRequest {
  query: string;
  scope: "all" | "character" | "workspace";
  targetId?: string;
  maxResults?: number;
  minScore?: number;
  includeSessionRecords?: boolean;
  sourceTypes?: string[];
  tags?: string[];
}
```

**响应数据**:
```typescript
interface RetrievalResponse {
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

#### 4.7.2 检索测试

**接口地址**: `/retrieval/test`
**请求方法**: POST
**请求数据**:
```typescript
interface RetrievalTestRequest {
  query: string;
  scope: "character" | "workspace";
  targetId: string;
  expectedResults?: string[];
  maxResults?: number;
  minScore?: number;
}
```

**响应数据**:
```typescript
interface RetrievalTestResponse {
  results: RetrievalResult[];
  total: number;
  latency: number;
  accuracy?: number;
  precision?: number;
  recall?: number;
}
```

---

### 4.8 运维接口

#### 4.8.1 获取运行时状态

**接口地址**: `/operations/status`
**请求方法**: GET
**请求参数**:
- `scope`: 范围（可选，值为 character/workspace/all，默认 all）
- `targetId`: 目标 ID（可选，scope 为 character 或 workspace 时必填）

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

#### 4.8.2 执行系统检查

**接口地址**: `/operations/probe`
**请求方法**: POST
**请求参数**:
- `scope`: 范围（可选，值为 character/workspace/all，默认 all）
- `targetId`: 目标 ID（可选，scope 为 character 或 workspace 时必填）

**响应数据**:
```typescript
interface ProbeResult {
  success: boolean;
  latency?: number;
  error?: string;
  details?: string;
}
```

#### 4.8.3 重建索引

**接口地址**: `/operations/reindex`
**请求方法**: POST
**请求参数**:
- `scope`: 范围（可选，值为 character/workspace/all，默认 all）
- `targetId`: 目标 ID（可选，scope 为 character 或 workspace 时必填）
- `sourceId`: 数据源 ID（可选，只重建该数据源的索引）

**响应数据**: `{ success: boolean, message?: string }`

#### 4.8.4 清除缓存

**接口地址**: `/operations/clear-cache`
**请求方法**: POST
**请求参数**:
- `scope`: 范围（可选，值为 character/workspace/all，默认 all）
- `targetId`: 目标 ID（可选，scope 为 character 或 workspace 时必填）

**响应数据**: `{ success: boolean, clearedCount: number }`

---

### 4.9 统计接口

#### 4.9.1 获取全局统计信息

**接口地址**: `/stats/overview`
**请求方法**: GET

**响应数据**:
```typescript
interface MemoryOverviewStats {
  totalCharacterMemories: number;
  totalWorkspaceMemories: number;
  totalKnowledgeItems: number;
  totalChunks: number;
  healthyCount: number;
  warningCount: number;
  errorCount: number;
  lastSyncOverall: string;
  embeddingAvailable: boolean;
  vectorAvailable: boolean;
  abnormalInstances: AbnormalMemory[];
}

interface AbnormalMemory {
  id: string;
  type: "character" | "workspace";
  name: string;
  reason: string;
}
```

#### 4.9.2 获取数据源统计信息

**接口地址**: `/stats/sources`
**请求方法**: GET

**响应数据**:
```typescript
interface SourceStatsResponse {
  sourceTypeDistribution: Record<string, number>;
  statusDistribution: Record<string, number>;
  healthDistribution: Record<string, number>;
  totalFiles: number;
  totalChunks: number;
  averageChunksPerFile: number;
}
```

---

## 5. 错误码

| 错误码 | 描述 |
|--------|------|
| 0 | 成功 |
| 1001 | 参数无效 |
| 1002 | 资源未找到 |
| 1003 | 权限不足 |
| 1004 | 资源已存在 |
| 1005 | 操作失败 |
| 2001 | 数据源同步失败 |
| 2002 | 文件解析失败 |
| 2003 | 索引创建失败 |
| 3001 | 检索服务不可用 |
| 3002 | 检索查询失败 |
| 4001 | 系统错误 |

## 6. 使用示例

### 示例 1: 获取数据源列表

```bash
curl -X GET "http://localhost:8080/api/v1/memory/sources?page=1&pageSize=10&status=active"
```

### 示例 2: 创建数据源

```bash
curl -X POST "http://localhost:8080/api/v1/memory/sources" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "产品文档",
    "sourcePath": "/docs/product",
    "sourceType": "file",
    "enabled": true
  }'
```

### 示例 3: 检索记忆内容

```bash
curl -X POST "http://localhost:8080/api/v1/memory/retrieval/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "用户登录流程",
    "scope": "workspace",
    "targetId": "ws-123",
    "maxResults": 5,
    "minScore": 0.7
  }'
```

---

## 7. 注意事项

1. 所有接口都需要进行身份验证，使用 Bearer Token 方式
2. 接口响应格式统一，使用 `code-message-data` 结构
3. 分页接口使用 `PageResponse` 格式
4. 时间字段格式为 ISO-8601（如：2023-10-01T10:30:00Z）
5. 错误响应会包含 `code` 和 `message` 字段
6. 对于大型数据操作，建议使用分页查询
7. 同步操作可能需要较长时间，建议使用异步处理
