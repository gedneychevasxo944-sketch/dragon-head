# Workspace 模块对 Memory 依赖接口文档

## 1. 概述

Workspace 模块负责管理团队共享记忆，与 Memory 模块紧密集成，通过一系列接口实现工作空间记忆的存储、检索和管理功能。

## 2. 接口列表

### 2.1 Workspace 记忆管理接口

#### 2.1.1 获取工作空间记忆列表

**接口地址**: `/api/v1/workspaces/memories`
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

#### 2.1.2 获取工作空间记忆详情

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory`
**请求方法**: GET
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**: `WorkspaceMemory`

#### 2.1.3 获取工作空间记忆内容

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/content`
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

#### 2.1.4 同步工作空间记忆

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/sync`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**: `{ success: boolean, message?: string }`

#### 2.1.5 获取工作空间记忆配置

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/config`
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

#### 2.1.6 更新工作空间记忆配置

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/config`
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

#### 2.1.7 添加工作空间记忆内容

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/content`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**请求数据**:
```typescript
interface CreateWorkspaceKnowledgeItemRequest {
  title: string;
  content: string;
  summary?: string;
  sourceType: "document" | "chat" | "manual" | "imported" | "generated";
  tags?: string[];
}
```

**响应数据**: `KnowledgeItem`

#### 2.1.8 更新工作空间记忆内容

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/content/{itemId}`
**请求方法**: PUT
**路径参数**:
- `workspaceId`: 工作空间 ID
- `itemId`: 知识项 ID

**请求数据**:
```typescript
interface UpdateWorkspaceKnowledgeItemRequest {
  title?: string;
  content?: string;
  summary?: string;
  tags?: string[];
  indexedStatus?: "indexed" | "pending" | "failed";
}
```

**响应数据**: `KnowledgeItem`

#### 2.1.9 删除工作空间记忆内容

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/content/{itemId}`
**请求方法**: DELETE
**路径参数**:
- `workspaceId`: 工作空间 ID
- `itemId`: 知识项 ID

**响应数据**: `{ success: boolean }`

---

### 2.2 Workspace 记忆检索接口

#### 2.2.1 检索工作空间记忆

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/retrieval`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**请求数据**:
```typescript
interface WorkspaceRetrievalRequest {
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
interface WorkspaceRetrievalResponse {
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

#### 2.2.2 工作空间记忆检索测试

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/retrieval/test`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**请求数据**:
```typescript
interface WorkspaceRetrievalTestRequest {
  query: string;
  expectedResults?: string[];
  maxResults?: number;
  minScore?: number;
}
```

**响应数据**:
```typescript
interface WorkspaceRetrievalTestResponse {
  results: RetrievalResult[];
  total: number;
  latency: number;
  accuracy?: number;
  precision?: number;
  recall?: number;
}
```

---

### 2.3 Workspace 记忆运维接口

#### 2.3.1 获取工作空间记忆运行时状态

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/status`
**请求方法**: GET
**路径参数**:
- `workspaceId`: 工作空间 ID

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

#### 2.3.2 执行工作空间记忆系统检查

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/probe`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**:
```typescript
interface ProbeResult {
  success: boolean;
  latency?: number;
  error?: string;
  details?: string;
}
```

#### 2.3.3 重建工作空间记忆索引

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/reindex`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**: `{ success: boolean, message?: string }`

#### 2.3.4 清除工作空间记忆缓存

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/clear-cache`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**: `{ success: boolean, clearedCount: number }`

---

## 3. Workspace 与 Memory 的绑定接口

#### 3.1 获取工作空间绑定的记忆文件

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/bindings`
**请求方法**: GET
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**:
```typescript
interface WorkspaceMemoryBindingsResponse {
  bindings: Binding[];
  total: number;
}
```

#### 3.2 绑定记忆文件到工作空间

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/bindings`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**请求数据**:
```typescript
interface BindMemoryToWorkspaceRequest {
  fileId: string;
  mountType: "full" | "selective" | "rule";
  selectedChunkIds?: string[];
  mountRules?: MountRule[];
}
```

**响应数据**: `Binding`

#### 3.3 解绑工作空间记忆文件

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/bindings/{bindingId}`
**请求方法**: DELETE
**路径参数**:
- `workspaceId`: 工作空间 ID
- `bindingId`: 绑定 ID

**响应数据**: `{ success: boolean }`

---

## 4. 错误码

| 错误码 | 描述 |
|--------|------|
| 0 | 成功 |
| 1001 | 参数无效 |
| 1002 | 工作空间未找到 |
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

### 示例 1: 获取工作空间记忆列表

```bash
curl -X GET "http://localhost:8080/api/v1/workspaces/memories?page=1&pageSize=5&status=synced"
```

### 示例 2: 获取工作空间记忆详情

```bash
curl -X GET "http://localhost:8080/api/v1/workspaces/ws-123/memory"
```

### 示例 3: 检索工作空间记忆

```bash
curl -X POST "http://localhost:8080/api/v1/workspaces/ws-123/memory/retrieval" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "产品需求文档",
    "maxResults": 3,
    "minScore": 0.8
  }'
```

---

## 6. Workspace 与 Memory 的架构

### 6.1 Workspace Memory 架构

Workspace Memory 采用三层架构：
1. **WorkspaceMemory**: 工作空间记忆的元数据和状态管理
2. **Binding**: 工作空间与记忆文件的关联关系
3. **KnowledgeItem**: 工作空间记忆的具体内容项

### 6.2 数据流动

```
外部数据源 → SourceDocument → MemoryFile → MemoryChunk → KnowledgeItem →
    (Binding) → WorkspaceMemory
```

### 6.3 绑定策略

#### 6.3.1 完全绑定（Full Mount）

```json
{
  "fileId": "file-123",
  "mountType": "full"
}
```

**特点**:
- 绑定整个文件的所有内容
- 文件内容变化会自动同步到工作空间
- 适用于需要完整文件内容的场景

#### 6.3.2 选择性绑定（Selective Mount）

```json
{
  "fileId": "file-123",
  "mountType": "selective",
  "selectedChunkIds": ["chunk-1", "chunk-2"]
}
```

**特点**:
- 只绑定文件中的特定片段
- 可以精确控制工作空间中的记忆内容
- 适用于只需要部分文件内容的场景

#### 6.3.3 规则绑定（Rule Mount）

```json
{
  "fileId": "file-123",
  "mountType": "rule",
  "mountRules": [
    {
      "type": "tag",
      "operator": "include",
      "value": "产品需求"
    },
    {
      "type": "content",
      "operator": "exclude",
      "value": "内部文档"
    }
  ]
}
```

**特点**:
- 基于规则自动选择需要绑定的内容
- 支持标签、内容和时间规则
- 适用于动态内容管理场景

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

- 工作空间记忆内容加密存储
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

---

## 9. 扩展接口

### 9.1 工作空间记忆统计接口

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/stats`
**请求方法**: GET
**路径参数**:
- `workspaceId`: 工作空间 ID

**响应数据**:
```typescript
interface WorkspaceMemoryStats {
  totalFiles: number;
  totalChunks: number;
  totalItems: number;
  indexedCount: number;
  pendingCount: number;
  failedCount: number;
  lastSyncTime: string;
  averageScore: number;
  topTags: string[];
}
```

### 9.2 工作空间记忆使用报告

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/reports`
**请求方法**: GET
**路径参数**:
- `workspaceId`: 工作空间 ID

**请求参数**:
- `period`: 报告周期（可选，值为 day/week/month/quarter/year）
- `format`: 报告格式（可选，值为 json/csv/html，默认 json）

**响应数据**:
```typescript
interface WorkspaceMemoryReport {
  period: string;
  totalQueries: number;
  successfulQueries: number;
  averageLatency: number;
  topQueries: { query: string; count: number }[];
  topItems: { id: string; title: string; hitCount: number }[];
  usageTrend: { date: string; count: number }[];
}
```

---

## 10. 迁移接口

### 10.1 导出工作空间记忆

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/export`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**请求数据**:
```typescript
interface ExportWorkspaceMemoryRequest {
  format: "json" | "csv" | "markdown";
  includeMetadata?: boolean;
  includeContent?: boolean;
  sources?: string[];
}
```

**响应数据**: `{ url: string, filename: string }`

### 10.2 导入工作空间记忆

**接口地址**: `/api/v1/workspaces/{workspaceId}/memory/import`
**请求方法**: POST
**路径参数**:
- `workspaceId`: 工作空间 ID

**请求数据**:
```typescript
interface ImportWorkspaceMemoryRequest {
  file: File;
  format: "json" | "csv" | "markdown";
  mergeStrategy?: "replace" | "merge" | "skip";
}
```

**响应数据**: `{ success: boolean, importedCount: number, skippedCount: number, errors: string[] }`
