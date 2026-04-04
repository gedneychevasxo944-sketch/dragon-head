# 前端功能 → 后端接口对应文档

> 本文档列出前端页面各功能与后端 REST 接口的一一对应关系。
>
> **Base URL**：`/api/v1`
> **统一响应格式**：`{ "code": 0, "message": "success", "data": { ... } }`
> **分页响应格式**：`{ "code": 0, "message": "success", "data": { "list": [...], "total": 100, "page": 1, "pageSize": 20 } }`
> **生成时间**：2026-04-03
> **版本**：v1.0

---

## 目录

1. [Studio — /studio](#1-studio--studio)
    - [Characters](#11-characters-页)
    - [Traits](#12-traits-页)
    - [Templates](#13-templates-页)
    - [Deployments](#14-deployments-页)
    - [Run（独立运行）](#15-run独立运行-页)
2. [Workspace — /workspaces](#2-workspace--workspaces)
    - [列表](#21-workspace-列表页)
    - [Team（成员）](#22-team-成员)
    - [Skills（技能绑定）](#23-skills-技能绑定)
    - [Memory（记忆）](#24-memory-记忆)
    - [Observer（观测者）](#25-observer-观测者)
    - [Tasks（任务）](#26-tasks-任务)
    - [Logs（审计日志）](#27-logs-审计日志)
    - [Materials（素材）](#28-materials-素材)
    - [Permissions（权限）](#29-permissions-权限)
3. [Skill — /skills](#3-skill--skills)
4. [Tool — /tools](#4-tool--tools)
5. [Memory — /memory](#5-memory--memory)
    - [Sources（数据源）](#51-sources-数据源)
    - [Content（文件与片段）](#52-content-文件与片段)
    - [Retrieval（检索）](#53-retrieval-检索)
    - [Config（记忆配置）](#54-config-记忆配置)
6. [Observer — /observers](#6-observer--observers)
7. [Config — /config](#7-config--config)
8. [Logs — /logs](#8-logs--logs)
9. [Chat — /chat](#9-chat--chat)
10. [Lab — /lab](#10-lab--lab)
    - [Scene Plugin](#101-scene-plugin-场景插件)
    - [Task Network](#102-task-network-任务网络)
    - [Topology](#103-topology-拓扑图)
    - [Behavior Arena](#104-behavior-arena-行为演练场)
    - [Skill Composer](#105-skill-composer-技能组合器)
    - [Timeline](#106-timeline-时间线回放)

---

## 1. Studio — /studio

**Controller**：`StudioController`
**Application**：`StudioApplication`

### 1.1 Characters 页

**前端页面**：`/studio/characters`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载角色列表（分页+搜索） | `GET` | `/api/v1/studio/characters` | Query: `page`, `pageSize`, `search`, `status`, `source` |
| 创建新角色 | `POST` | `/api/v1/studio/characters` | Body: `name`, `description`, `source`, `traits`, `skills` 等 |
| 查看角色详情 | `GET` | `/api/v1/studio/characters/{id}` | - |
| 编辑角色 | `PUT` | `/api/v1/studio/characters/{id}` | Body: 与创建相同（字段均可选） |
| 删除角色 | `DELETE` | `/api/v1/studio/characters/{id}` | - |
| 加载仪表板统计 | `GET` | `/api/v1/studio/stats` | 返回 totalCharacters、activeCharacters 等 |

### 1.2 Traits 页

**前端页面**：`/studio/traits`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载 Trait 列表 | `GET` | `/api/v1/studio/traits` | Query: `page`, `pageSize`, `search`, `type`, `category` |
| 创建 Trait | `POST` | `/api/v1/studio/traits` | Body: `name`, `type`, `category`, `description`, `content` |
| 查看 Trait 详情 | `GET` | `/api/v1/studio/traits/{id}` | - |
| 编辑 Trait | `PUT` | `/api/v1/studio/traits/{id}` | - |
| 删除 Trait | `DELETE` | `/api/v1/studio/traits/{id}` | - |

> ⚠️ **注意**：Trait 系统暂未完整实现，当前接口返回 `501 Not Implemented`。

### 1.3 Templates 页

**前端页面**：`/studio/templates`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载模板列表 | `GET` | `/api/v1/studio/templates` | Query: `category` |
| 从模板派生角色 | `POST` | `/api/v1/studio/templates/{id}/derive` | Body: `name`, `description` |

### 1.4 Deployments 页

**前端页面**：`/studio/deployments`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载派驻记录列表 | `GET` | `/api/v1/studio/deployments` | Query: `page`, `pageSize`, `characterId`, `workspaceId`, `status` |
| 派驻角色到 Workspace | `POST` | `/api/v1/studio/deployments` | Body: `characterId`, `workspaceId`, `role`, `position`, `level` |
| 撤销派驻 | `DELETE` | `/api/v1/studio/deployments/{id}` | - |

### 1.5 Run（独立运行）页

**前端页面**：`/studio/run/[id]`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 向角色发送消息 | `POST` | `/api/v1/studio/characters/{id}/run` | Body: `message`, `sessionId` |

---

## 2. Workspace — /workspaces

**Controller**：`WorkspaceController`
**Application**：`WorkspaceApiApplication`

### 2.1 Workspace 列表页

**前端页面**：`/workspaces`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载 Workspace 列表 | `GET` | `/api/v1/workspaces` | Query: `page`, `pageSize`, `search`, `status`, `teamStatus`, `hasObserver` |
| 创建 Workspace | `POST` | `/api/v1/workspaces` | Body: `name`, `description`, `personality`, `properties` |
| 查看 Workspace 详情 | `GET` | `/api/v1/workspaces/{id}` | - |
| 更新 Workspace 设置 | `PUT` | `/api/v1/workspaces/{id}/settings` | Body: name/description/personality/status |
| 删除 Workspace | `DELETE` | `/api/v1/workspaces/{id}` | - |

### 2.2 Team（成员）

**前端页面**：`/workspaces/[id]`（Team 标签）

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载成员列表 | `GET` | `/api/v1/workspaces/{workspaceId}/members` | - |
| 加载团队席位列表 | `GET` | `/api/v1/workspaces/{workspaceId}/team-positions` | - |
| 添加成员 | `POST` | `/api/v1/workspaces/{workspaceId}/members` | Body: `characterId`, `role`, `position`, `level`, `sourceType` |
| 移除成员 | `DELETE` | `/api/v1/workspaces/{workspaceId}/members/{memberId}` | - |

### 2.3 Skills（技能绑定）

**前端页面**：`/workspaces/[id]`（Skills 标签）

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载已绑定技能 | `GET` | `/api/v1/workspaces/{workspaceId}/skills` | - |
| 绑定技能 | `POST` | `/api/v1/workspaces/{workspaceId}/skills` | Body: `skillId`, `pinnedVersion`, `useLatest` |
| 解绑技能 | `DELETE` | `/api/v1/workspaces/{workspaceId}/skills/{skillId}` | - |
| 更新技能绑定配置 | `PUT` | `/api/v1/workspaces/{workspaceId}/skills/{skillId}` | Body: `pinnedVersion`, `useLatest`, `enabled` |

### 2.4 Memory（记忆）

**前端页面**：`/workspaces/[id]`（Memory 标签）

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 获取记忆配置信息 | `GET` | `/api/v1/workspaces/{workspaceId}/memory` | 返回 backend/provider/fileCount 等 |
| 触发记忆同步 | `POST` | `/api/v1/workspaces/{workspaceId}/memory/sync` | - |

### 2.5 Observer（观测者）

**前端页面**：`/workspaces/[id]`（Observer 标签）

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 获取 Observer 绑定信息 | `GET` | `/api/v1/workspaces/{workspaceId}/observer` | - |
| 绑定 Observer | `POST` | `/api/v1/workspaces/{workspaceId}/observer` | Body: `observerId`, `evaluationMode`, `autoOptimization` |
| 解绑 Observer | `DELETE` | `/api/v1/workspaces/{workspaceId}/observer` | - |

### 2.6 Tasks（任务）

**前端页面**：`/workspaces/[id]`（Tasks 标签）

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载任务列表 | `GET` | `/api/v1/workspaces/{workspaceId}/tasks` | Query: `status`, `page`, `pageSize` |

### 2.7 Logs（审计日志）

**前端页面**：`/workspaces/[id]`（Logs 标签）

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载审计日志 | `GET` | `/api/v1/workspaces/{workspaceId}/audit-logs` | Query: `targetType`, `actionType`, `page`, `pageSize` |

### 2.8 Materials（素材）

**前端页面**：`/workspaces/[id]`（Materials 标签）

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载素材列表 | `GET` | `/api/v1/workspaces/{workspaceId}/materials` | - |
| 上传素材 | `POST` | `/api/v1/workspaces/{workspaceId}/materials` | Content-Type: multipart/form-data, Field: `file` |
| 删除素材 | `DELETE` | `/api/v1/workspaces/{workspaceId}/materials/{materialId}` | - |

### 2.9 Permissions（权限）

**前端页面**：`/workspaces/[id]`（Permissions 标签）

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载权限成员列表 | `GET` | `/api/v1/workspaces/{workspaceId}/permissions` | - |
| 添加成员权限 | `POST` | `/api/v1/workspaces/{workspaceId}/permissions` | Body: `userId`, `role` |
| 更新成员权限 | `PUT` | `/api/v1/workspaces/{workspaceId}/permissions/{userId}` | Body: `{ "role": "admin" }` |
| 移除成员权限 | `DELETE` | `/api/v1/workspaces/{workspaceId}/permissions/{userId}` | - |

---

## 3. Skill — /skills

**Controller**：`SkillController`
**Application**：`SkillApplication`

**前端页面**：`/skills`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载技能列表 | `GET` | `/api/v1/skills` | Query: `page`, `pageSize`, `search`, `visibility`, `assetState`, `runtimeStatus`, `category` |
| 创建技能 | `POST` | `/api/v1/skills` | Body: `name`, `category`, `description`, `visibility`, `contentStructure` |
| 查看技能详情 | `GET` | `/api/v1/skills/{id}` | - |
| 更新技能 | `PUT` | `/api/v1/skills/{id}` | - |
| 发布技能版本 | `POST` | `/api/v1/skills/{id}/publish` | Body: `version`, `changelog` |
| 删除技能 | `DELETE` | `/api/v1/skills/{id}` | - |
| 保存技能草稿 | `PUT` | `/api/v1/skills/{id}/draft` | Body: `{ "content": SkillContentStructure }` |

---

## 4. Tool — /tools

**Controller**：`ToolController`
**Application**：`ToolApplication`

**前端页面**：`/tools`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载工具列表 | `GET` | `/api/v1/tools` | Query: `page`, `pageSize`, `search`, `visibility`, `toolType`, `runtimeStatus`, `category` |
| 创建工具 | `POST` | `/api/v1/tools` | Body: `name`, `category`, `toolType`, `invocationType`, `visibility`, `definition` |
| 查看工具详情 | `GET` | `/api/v1/tools/{id}` | - |
| 更新工具 | `PUT` | `/api/v1/tools/{id}` | - |
| 发布工具版本 | `POST` | `/api/v1/tools/{id}/publish` | Body: `version`, `changelog`, `definition` |
| 删除工具 | `DELETE` | `/api/v1/tools/{id}` | - |

---

## 5. Memory — /memory

**Controller**：`MemoryController`
**Application**：`MemoryApplication`

### 5.1 Sources（数据源）

**前端页面**：`/memory/sources`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载数据源列表 | `GET` | `/api/v1/memory/sources` | Query: `search`, `sourceType`, `status` |
| 添加数据源 | `POST` | `/api/v1/memory/sources` | Body: `title`, `sourceType`, `sourcePath`, `backend`, `provider` |
| 查看数据源详情（含文件列表） | `GET` | `/api/v1/memory/sources/{sourceId}` | - |
| 触发数据源同步 | `POST` | `/api/v1/memory/sources/{sourceId}/sync` | - |
| 删除数据源 | `DELETE` | `/api/v1/memory/sources/{sourceId}` | - |

### 5.2 Content（文件与片段）

**前端页面**：`/memory/content`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载记忆文件列表 | `GET` | `/api/v1/memory/files` | Query: `sourceId`, `search`, `syncStatus`, `healthStatus`, `page`, `pageSize` |
| 查看记忆文件详情 | `GET` | `/api/v1/memory/files/{fileId}` | - |
| 加载记忆片段列表 | `GET` | `/api/v1/memory/chunks` | Query: `fileId`, `search`, `indexedStatus`, `tags`, `page`, `pageSize` |
| 创建记忆片段 | `POST` | `/api/v1/memory/chunks` | Body: `fileId`, `title`, `content`, `summary`, `tags` |
| 编辑记忆片段 | `PUT` | `/api/v1/memory/chunks/{chunkId}` | - |
| 批量打标签 | `POST` | `/api/v1/memory/chunks/batch-tag` | Body: `chunkIds`, `tags`, `action` |
| 批量压缩（融合）片段 | `POST` | `/api/v1/memory/chunks/compress` | Body: `chunkIds`, `targetFileId`, `title` |
| 删除记忆片段 | `DELETE` | `/api/v1/memory/chunks/{chunkId}` | - |

### 5.3 Retrieval（检索）

**前端页面**：`/memory/retrieval`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 向量/全文混合检索 | `POST` | `/api/v1/memory/retrieval/search` | Body: `query`, `scopeType`, `scopeId`, `topK`, `minScore` |

### 5.4 Config（记忆配置）

**前端页面**：`/memory/config`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 获取 Character 记忆配置 | `GET` | `/api/v1/memory/config/character/{characterId}` | - |
| 更新 Character 记忆配置 | `PUT` | `/api/v1/memory/config/character/{characterId}` | Body: `enabled`, `privateMemoryEnabled`, `defaultSources`, `queryParams` |
| 获取 Workspace 记忆配置 | `GET` | `/api/v1/memory/config/workspace/{workspaceId}` | - |
| 更新 Workspace 记忆配置 | `PUT` | `/api/v1/memory/config/workspace/{workspaceId}` | Body: `enabled`, `backend`, `provider`, `model`, `syncStrategy` |
| 获取记忆运行时状态 | `GET` | `/api/v1/memory/runtime-status` | - |
| 加载绑定列表 | `GET` | `/api/v1/memory/bindings` | Query: `fileId`, `targetType`, `targetId` |
| 创建绑定 | `POST` | `/api/v1/memory/bindings` | Body: `fileId`, `targetType`, `targetId`, `mountType` |
| 删除绑定 | `DELETE` | `/api/v1/memory/bindings/{bindingId}` | - |

---

## 6. Observer — /observers

**Controller**：`ObserverController`
**Application**：`ObserverApplication`

**前端页面**：`/observers`、`/observers/[id]`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载 Observer 列表 | `GET` | `/api/v1/observers` | Query: `search`, `targetType`, `status`, `executionMode`, `page`, `pageSize` |
| 创建 Observer | `POST` | `/api/v1/observers` | Body: `name`, `targetType`, `targetId`, `executionMode` |
| 查看 Observer 详情 | `GET` | `/api/v1/observers/{id}` | - |
| 更新 Observer | `PUT` | `/api/v1/observers/{id}` | - |
| 删除 Observer | `DELETE` | `/api/v1/observers/{id}` | - |
| 手动触发评价 | `POST` | `/api/v1/observers/{id}/evaluate` | - |
| 加载评价记录列表 | `GET` | `/api/v1/observers/{observerId}/evaluations` | Query: `page`, `pageSize`, `status` |
| 加载优化计划列表 | `GET` | `/api/v1/observers/{observerId}/plans` | Query: `page`, `pageSize` |
| 审批优化计划 | `POST` | `/api/v1/observers/{observerId}/plans/{planId}/approve` | Body: `approved`, `comment` |
| 执行优化计划 | `POST` | `/api/v1/observers/{observerId}/plans/{planId}/execute` | - |
| 加载优化动作列表 | `GET` | `/api/v1/observers/{observerId}/actions` | Query: `status`, `page`, `pageSize` |
| 回滚优化动作 | `POST` | `/api/v1/observers/{observerId}/actions/{actionId}/rollback` | - |
| 加载治理日志 | `GET` | `/api/v1/observers/{observerId}/governance-logs` | Query: `page`, `pageSize` |

---

## 7. Config — /config

**Controller**：`ConfigController`
**Application**：`ConfigApplication`

**前端页面**：`/config`、`/config/browser`、`/config/structured`、`/config/advanced`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载配置项列表 | `GET` | `/api/v1/config/items` | Query: `domain`, `scopeType`, `scopeId`, `search`, `isDraft`, `hasOverride`, `page`, `pageSize` |
| 更新配置项（保存草稿） | `PUT` | `/api/v1/config/items/{id}` | Body: `value`, `saveAsDraft` |
| 发布配置草稿 | `POST` | `/api/v1/config/items/{id}/publish` | - |
| 回滚配置 | `POST` | `/api/v1/config/items/{id}/rollback` | Body: `{ "changeRecordId": "string" }` |
| 查看生效链 | `GET` | `/api/v1/config/items/{id}/effect-chain` | - |
| 查看变更记录 | `GET` | `/api/v1/config/change-records` | Query: `domain`, `configItemId`, `page`, `pageSize` |
| 查看影响分析 | `GET` | `/api/v1/config/items/{id}/impact` | - |

---

## 8. Logs — /logs

**Controller**：`LogsController`
**Application**：`LogsApplication`

**前端页面**：`/logs`

### Log View 标签

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载事件日志列表 | `GET` | `/api/v1/logs/events` | Query: `targetType`, `targetId`, `eventType`, `severity`, `search`, `dateStart`, `dateEnd`, `page`, `pageSize` |
| 查看事件详情 | `GET` | `/api/v1/logs/events/{id}` | - |

### Trace 标签

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载链路列表 | `GET` | `/api/v1/logs/traces` | Query: `targetType`, `targetId`, `traceId`, `status`, `page`, `pageSize` |
| 查看链路详情（含 TraceNode 树） | `GET` | `/api/v1/logs/traces/{traceId}` | - |

### Health 标签

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载健康状态列表 | `GET` | `/api/v1/logs/health` | Query: `targetType`, `status` |
| 获取全局健康统计 | `GET` | `/api/v1/logs/health/stats` | - |

### Audit 标签

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载审计记录 | `GET` | `/api/v1/logs/audit` | Query: `targetType`, `operator`, `action`, `dateStart`, `dateEnd`, `page`, `pageSize` |

---

## 9. Chat — /chat

**Controller**：`ChatController`
**Application**：`ChatApplication`

**前端页面**：`/chat`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 发送消息给 AI | `POST` | `/api/v1/chat/messages` | Body: `sessionId`, `message`, `characterId` |
| 加载会话历史消息 | `GET` | `/api/v1/chat/sessions/{sessionId}/messages` | - |
| 获取 AI 连接状态 | `GET` | `/api/v1/chat/status` | 返回 `connected`, `model`, `status` |

---

## 10. Lab — /lab

**Controller**：`LabController`
**Application**：`LabApplication`

### 10.1 Scene Plugin（场景插件）

**前端页面**：`/lab/scene-plugin`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载场景列表 | `GET` | `/api/v1/lab/scenes` | - |
| 创建场景 | `POST` | `/api/v1/lab/scenes` | Body: Scene 对象 |
| 更新场景 | `PUT` | `/api/v1/lab/scenes/{id}` | - |
| 加载场景实例列表 | `GET` | `/api/v1/lab/scenes/{sceneId}/instances` | - |

### 10.2 Task Network（任务网络）

**前端页面**：`/lab/task-network`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载任务列表 | `GET` | `/api/v1/lab/tasks` | Query: `status`, `priority` |
| 查看任务详情 | `GET` | `/api/v1/lab/tasks/{id}` | 含 schedule/supervision/acceptance |
| 更新任务状态 | `PUT` | `/api/v1/lab/tasks/{id}/status` | Body: `{ "status": "completed" }` |
| 提交验收结果 | `POST` | `/api/v1/lab/tasks/{id}/acceptance` | Body: `results`, `approved`, `approvedBy` |

### 10.3 Topology（拓扑图）

**前端页面**：`/lab/topology`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载拓扑图数据 | `GET` | `/api/v1/lab/topology` | Query: `nodeTypes`, `edgeTypes`, `workspaceId` |

### 10.4 Behavior Arena（行为演练场）

**前端页面**：`/lab/behavior-arena`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载演练场景列表 | `GET` | `/api/v1/lab/arena/scenarios` | - |
| 创建演练场景 | `POST` | `/api/v1/lab/arena/scenarios` | Body: `name`, `description`, `type`, `config` |
| 运行演练 | `POST` | `/api/v1/lab/arena/scenarios/{scenarioId}/run` | - |
| 查看演练结果 | `GET` | `/api/v1/lab/arena/runs/{runId}` | 含对话日志/决策点/反馈 |

### 10.5 Skill Composer（技能组合器）

**前端页面**：`/lab/skill-composer`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载技能图列表 | `GET` | `/api/v1/lab/skill-graphs` | - |
| 创建技能图 | `POST` | `/api/v1/lab/skill-graphs` | Body: SkillGraph 对象（含 nodes 和 edges） |
| 更新技能图 | `PUT` | `/api/v1/lab/skill-graphs/{id}` | - |
| 生成执行计划 | `POST` | `/api/v1/lab/skill-graphs/{id}/plan` | - |
| 执行技能图 | `POST` | `/api/v1/lab/skill-graphs/{id}/execute` | 返回 `{ "runId", "status" }` |

### 10.6 Timeline（时间线回放）

**前端页面**：`/lab/timeline`

| 前端功能 | HTTP 方法 | 后端接口 | 说明 |
|---------|-----------|---------|------|
| 加载可回放会话列表 | `GET` | `/api/v1/lab/timeline/sessions` | Query: `targetType`, `targetId`, `status` |
| 查看会话详情（含所有步骤） | `GET` | `/api/v1/lab/timeline/sessions/{sessionId}` | 含嵌套 `steps` 树 |

---

## 附录：Controller 汇总

| 模块 | Controller 类 | Application 类 | Base URL |
|------|-------------|----------------|----------|
| Studio | `StudioController` | `StudioApplication` | `/api/v1/studio` |
| Workspace | `WorkspaceController` | `WorkspaceApiApplication` | `/api/v1/workspaces` |
| Skill | `SkillController` | `SkillApplication` | `/api/v1/skills` |
| Tool | `ToolController` | `ToolApplication` | `/api/v1/tools` |
| Memory | `MemoryController` | `MemoryApplication` | `/api/v1/memory` |
| Observer | `ObserverController` | `ObserverApplication` | `/api/v1/observers` |
| Config | `ConfigController` | `ConfigApplication` | `/api/v1/config` |
| Logs | `LogsController` | `LogsApplication` | `/api/v1/logs` |
| Chat | `ChatController` | `ChatApplication` | `/api/v1/chat` |
| Lab | `LabController` | `LabApplication` | `/api/v1/lab` |

---

## 附录：通用规范

### 鉴权
- 所有接口需要 Bearer Token 认证（`Authorization: Bearer <token>`）
- 登录接口：`POST /api/v1/auth/login`

### 错误码
| code | 含义 |
|------|------|
| `0` | 成功 |
| `400` | 请求参数错误 |
| `401` | 未认证 |
| `403` | 无权限 |
| `404` | 资源不存在 |
| `500` | 服务器内部错误 |
| `501` | 功能未实现（占位接口） |

### 分页参数
| 参数 | 默认值 | 说明 |
|------|--------|------|
| `page` | `1` | 页码（从 1 开始） |
| `pageSize` | `20` | 每页数量 |

### 时间格式
所有时间字段使用 ISO 8601 格式：`2024-01-01T00:00:00.000Z`