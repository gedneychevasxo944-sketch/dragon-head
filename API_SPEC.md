# Adeptify 后端接口文档

> 本文档基于前端 Mock 数据类型定义生成，供后端接口实现参考。
>
> **约定**：
> - Base URL: `/api/v1`
> - 所有请求/响应 Content-Type 为 `application/json`
> - 分页参数统一使用 `page`（从 1 开始）和 `pageSize`（默认 20）
> - 时间字段统一使用 ISO 8601 格式字符串（`2024-01-01T00:00:00.000Z`）
> - 通用响应结构：
    >   ```json
>   { "code": 0, "message": "success", "data": { ... } }
>   ```
> - 通用分页响应：
    >   ```json
>   { "code": 0, "message": "success", "data": { "list": [...], "total": 100, "page": 1, "pageSize": 20 } }
>   ```

---

## 目录

1. [Studio — Character（角色）](#1-studio--character角色)
2. [Studio — Trait（特征片段）](#2-studio--trait特征片段)
3. [Studio — Template（内置模板）](#3-studio--template内置模板)
4. [Studio — Deployment（派驻记录）](#4-studio--deployment派驻记录)
5. [Workspace（工作空间）](#5-workspace工作空间)
6. [Workspace — Member（团队成员）](#6-workspace--member团队成员)
7. [Workspace — Skill Binding（技能绑定）](#7-workspace--skill-binding技能绑定)
8. [Workspace — Memory（记忆配置）](#8-workspace--memory记忆配置)
9. [Workspace — Observer（观测者绑定）](#9-workspace--observer观测者绑定)
10. [Workspace — Task（任务）](#10-workspace--task任务)
11. [Workspace — Audit Log（审计日志）](#11-workspace--audit-log审计日志)
12. [Workspace — Material（素材）](#12-workspace--material素材)
13. [Workspace — Permission（权限）](#13-workspace--permission权限)
14. [Skill（技能）](#14-skill技能)
15. [Tool（工具）](#15-tool工具)
16. [Memory — Source（数据源）](#16-memory--source数据源)
17. [Memory — File（记忆文件）](#17-memory--file记忆文件)
18. [Memory — Chunk（记忆片段）](#18-memory--chunk记忆片段)
19. [Memory — Binding（绑定关系）](#19-memory--binding绑定关系)
20. [Memory — Retrieval（检索）](#20-memory--retrieval检索)
21. [Memory — Config（记忆配置）](#21-memory--config记忆配置)
22. [Observer（观测者）](#22-observer观测者)
23. [Observer — Evaluation（评价记录）](#23-observer--evaluation评价记录)
24. [Observer — Plan（优化计划）](#24-observer--plan优化计划)
25. [Observer — Action（优化动作）](#25-observer--action优化动作)
26. [Observer — Governance Log（治理日志）](#26-observer--governance-log治理日志)
27. [Config（配置中心）](#27-config配置中心)
28. [Logs — Event（事件日志）](#28-logs--event事件日志)
29. [Logs — Trace（链路追踪）](#29-logs--trace链路追踪)
30. [Logs — Health（健康状态）](#30-logs--health健康状态)
31. [Logs — Audit（审计记录）](#31-logs--audit审计记录)
32. [Chat（AI 对话）](#32-chatai-对话)
33. [Lab — Scene Plugin（场景插件）](#33-lab--scene-plugin场景插件)
34. [Lab — Task Network（任务网络）](#34-lab--task-network任务网络)
35. [Lab — Topology（拓扑图）](#35-lab--topology拓扑图)
36. [Lab — Behavior Arena（行为演练场）](#36-lab--behavior-arena行为演练场)
37. [Lab — Skill Composer（技能组合器）](#37-lab--skill-composer技能组合器)
38. [Lab — Timeline（时间线回放）](#38-lab--timeline时间线回放)

---

## 1. Studio — Character（角色）

**所属页面**：`/studio/characters`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `source` | `built_in_derived` / `whiteboard_new` / `trait_composed` / `workspace_copied` |
| `status` | `active` / `inactive` / `archived` |

---

#### 1.1 获取角色列表

- **URL**：`GET /api/v1/studio/characters`
- **作用**：分页获取角色列表，支持按名称、状态、来源筛选

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | number | 否 | 页码，默认 1 |
| `pageSize` | number | 否 | 每页数量，默认 20 |
| `search` | string | 否 | 按名称/描述/ID 搜索 |
| `status` | string | 否 | 状态筛选：`active` / `inactive` / `archived` / `all` |
| `source` | string | 否 | 来源筛选：`built_in_derived` / `whiteboard_new` / `trait_composed` / `workspace_copied` / `all` |

**响应 `data`**

```json
{
  "list": [Character],
  "total": 100,
  "page": 1,
  "pageSize": 20
}
```

**Character 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 唯一 ID |
| `name` | string | 角色名称 |
| `description` | string | 角色描述 |
| `avatar` | string? | 头像 URL |
| `source` | CharacterSource | 创建来源 |
| `status` | CharacterStatus | 状态 |
| `traits` | string[] | Trait ID 列表 |
| `traitConfigs` | object | Trait 配置（key 为 traitId） |
| `skills` | CharacterSkillRef[] | 技能引用列表 |
| `promptTemplate` | string | Prompt 模板 |
| `defaultTools` | string[] | 默认工具 ID 列表 |
| `isRunning` | boolean | 是否运行中 |
| `deployedCount` | number | 已派驻 Workspace 数量 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

**CharacterSkillRef 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `skillId` | string | 技能 ID |
| `version` | string? | 固定版本，不填则使用最新 |
| `priority` | number | 优先级（数字越小越高） |
| `config` | object? | 覆盖配置 |

---

#### 1.2 创建角色

- **URL**：`POST /api/v1/studio/characters`
- **作用**：创建一个新的 AI 角色

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | 角色名称 |
| `description` | string | ✅ | 角色描述 |
| `avatar` | string | 否 | 头像 URL |
| `source` | CharacterSource | ✅ | 创建来源 |
| `traits` | string[] | 否 | Trait ID 列表 |
| `traitConfigs` | object | 否 | Trait 配置 |
| `skills` | CharacterSkillRef[] | 否 | 技能引用 |
| `promptTemplate` | string | 否 | Prompt 模板 |
| `defaultTools` | string[] | 否 | 默认工具列表 |
| `templateId` | string | 否 | 从模板初始化时传入模板 ID |

**响应 `data`**：返回创建后的完整 Character 对象

---

#### 1.3 获取角色详情

- **URL**：`GET /api/v1/studio/characters/:id`
- **作用**：获取单个角色的完整信息

**Path 参数**：`id` — 角色 ID

**响应 `data`**：完整 Character 对象

---

#### 1.4 更新角色

- **URL**：`PUT /api/v1/studio/characters/:id`
- **作用**：全量更新角色信息

**Path 参数**：`id` — 角色 ID

**请求体**：与创建相同（所有字段均可选，只传需要更新的字段）

**响应 `data`**：更新后的完整 Character 对象

---

#### 1.5 删除角色

- **URL**：`DELETE /api/v1/studio/characters/:id`
- **作用**：删除角色（需确认无活跃派驻）

**Path 参数**：`id` — 角色 ID

**响应 `data`**：`{ "success": true }`

---

#### 1.6 获取角色统计

- **URL**：`GET /api/v1/studio/stats`
- **作用**：获取 Studio 仪表板统计数据

**响应 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalCharacters` | number | 角色总数 |
| `activeCharacters` | number | 活跃角色数 |
| `runningCharacters` | number | 运行中角色数 |
| `totalTraits` | number | Trait 总数 |
| `totalDeployments` | number | 派驻记录总数 |

---

#### 1.7 独立运行角色（发送消息）

- **URL**：`POST /api/v1/studio/characters/:id/run`
- **作用**：向指定角色发送对话消息（用于 `/studio/run/[id]` 页面的独立测试）

**Path 参数**：`id` — 角色 ID

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `message` | string | ✅ | 用户消息内容 |
| `sessionId` | string | 否 | 会话 ID，不传则自动创建 |

**响应 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| `sessionId` | string | 会话 ID |
| `reply` | string | AI 回复内容 |
| `timestamp` | string | 回复时间 |

---

## 2. Studio — Trait（特征片段）

**所属页面**：`/studio/traits`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `type` | `personality` / `config` |
| `category` | `沟通风格` / `决策风格` / `风险偏好` / `协作风格` / `语言语气` / `行事原则` / `工具白名单` / `默认能力` / `mind配置` |

---

#### 2.1 获取 Trait 列表

- **URL**：`GET /api/v1/studio/traits`
- **作用**：分页获取 Trait 列表，支持按类型/分类筛选

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | number | 否 | 页码 |
| `pageSize` | number | 否 | 每页数量 |
| `search` | string | 否 | 按名称搜索 |
| `type` | string | 否 | `personality` / `config` / `all` |
| `category` | string | 否 | 分类筛选 |

**响应 `data`**：`{ "list": [Trait], "total": number }`

**Trait 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 唯一 ID |
| `name` | string | 名称 |
| `type` | TraitType | 类型 |
| `category` | TraitCategory | 分类 |
| `description` | string | 描述 |
| `content` | string | 具体内容（注入 Prompt 的片段） |
| `usedByCount` | number | 被多少 Character 使用 |
| `enabled` | boolean | 是否启用 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

---

#### 2.2 创建 Trait

- **URL**：`POST /api/v1/studio/traits`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | 名称 |
| `type` | TraitType | ✅ | 类型 |
| `category` | TraitCategory | ✅ | 分类 |
| `description` | string | ✅ | 描述 |
| `content` | string | ✅ | 具体内容 |
| `enabled` | boolean | 否 | 默认 `true` |

**响应 `data`**：完整 Trait 对象

---

#### 2.3 获取 Trait 详情

- **URL**：`GET /api/v1/studio/traits/:id`

---

#### 2.4 更新 Trait

- **URL**：`PUT /api/v1/studio/traits/:id`

**请求体**：与创建相同（字段均可选）

---

#### 2.5 删除 Trait

- **URL**：`DELETE /api/v1/studio/traits/:id`

---

## 3. Studio — Template（内置模板）

**所属页面**：`/studio/templates`

---

#### 3.1 获取模板列表

- **URL**：`GET /api/v1/studio/templates`
- **作用**：获取所有内置角色模板（一般不分页，总数量较少）

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `category` | string | 否 | 分类筛选：`助手` / `分析` / `客服` / `创作` / `开发` / `研究` / `全部` |

**响应 `data`**：`[BuiltInTemplate]`

**BuiltInTemplate 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 模板 ID |
| `name` | string | 模板名称 |
| `description` | string | 描述 |
| `category` | string | 分类 |
| `scenario` | string | 适用场景 |
| `preview` | string | 预览文字 |
| `defaultTraits` | string[] | 默认 Trait ID 列表 |
| `defaultConfig` | object | 默认配置（如 temperature、maxTokens） |

---

#### 3.2 从模板创建角色

- **URL**：`POST /api/v1/studio/templates/:id/derive`
- **作用**：基于模板快速派生一个新 Character

**Path 参数**：`id` — 模板 ID

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | 新角色名称 |
| `description` | string | 否 | 描述（默认继承模板） |

**响应 `data`**：新创建的完整 Character 对象

---

## 4. Studio — Deployment（派驻记录）

**所属页面**：`/studio/deployments`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `status` | `running` / `idle` / `error` |

---

#### 4.1 获取派驻记录列表

- **URL**：`GET /api/v1/studio/deployments`
- **作用**：获取所有角色的派驻记录

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | number | 否 | 页码 |
| `pageSize` | number | 否 | 每页数量 |
| `characterId` | string | 否 | 按角色 ID 筛选 |
| `workspaceId` | string | 否 | 按工作空间 ID 筛选 |
| `status` | string | 否 | `running` / `idle` / `error` / `all` |

**响应 `data`**：`{ "list": [Deployment], "total": number }`

**Deployment 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 派驻 ID |
| `characterId` | string | 角色 ID |
| `characterName` | string | 角色名称 |
| `workspaceId` | string | Workspace ID |
| `workspaceName` | string | Workspace 名称 |
| `role` | string | 角色承担的职责 |
| `position` | string | 职位名称 |
| `level` | number | 级别（1-10） |
| `status` | DeploymentStatus | 运行状态 |
| `deployedAt` | string | 派驻时间 |
| `lastActiveAt` | string | 最后活跃时间 |
| `hasOverrides` | boolean | 是否有配置覆盖 |

---

#### 4.2 派驻角色到 Workspace

- **URL**：`POST /api/v1/studio/deployments`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `characterId` | string | ✅ | 角色 ID |
| `workspaceId` | string | ✅ | 目标 Workspace ID |
| `role` | string | ✅ | 职责描述 |
| `position` | string | ✅ | 职位名称 |
| `level` | number | 否 | 级别，默认 3 |

**响应 `data`**：完整 Deployment 对象

---

#### 4.3 撤销派驻

- **URL**：`DELETE /api/v1/studio/deployments/:id`

---

## 5. Workspace（工作空间）

**所属页面**：`/workspaces`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `status` | `active` / `inactive` / `archived` |
| `teamStatus` | `complete` / `incomplete` / `not_initialized` |

---

#### 5.1 获取 Workspace 列表

- **URL**：`GET /api/v1/workspaces`
- **作用**：分页获取工作空间列表

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | number | 否 | 页码 |
| `pageSize` | number | 否 | 每页数量 |
| `search` | string | 否 | 按名称搜索 |
| `status` | string | 否 | `active` / `inactive` / `archived` / `all` |
| `teamStatus` | string | 否 | `complete` / `incomplete` / `not_initialized` / `all` |
| `hasObserver` | boolean | 否 | 是否绑定 Observer |

**响应 `data`**：`{ "list": [Workspace], "total": number }`

**Workspace 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 唯一 ID |
| `name` | string | 名称 |
| `description` | string | 描述 |
| `owner` | string | 所有者名称 |
| `ownerAvatar` | string? | 所有者头像 |
| `collaborators` | `{name, avatar?}[]` | 协作者列表 |
| `status` | WorkspaceStatus | 状态 |
| `memberCount` | number | 成员（角色）数量 |
| `teamStatus` | TeamStatus | 团队初始化状态 |
| `teamTotalSlots` | number | 团队总席位 |
| `teamFilledSlots` | number | 已填充席位 |
| `skillCount` | number | 绑定技能数量 |
| `activeTaskCount` | number | 活跃任务数 |
| `pendingApprovalCount` | number | 待审批数量 |
| `errorCount` | number | 错误数量 |
| `hasObserver` | boolean | 是否有 Observer |
| `observerId` | string? | Observer ID |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

---

#### 5.2 创建 Workspace

- **URL**：`POST /api/v1/workspaces`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | 名称 |
| `description` | string | ✅ | 描述 |
| `personality` | WorkspacePersonality | 否 | 性格配置 |
| `properties` | WorkspaceProperty[] | 否 | 自定义属性 |

**WorkspacePersonality 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `workingStyle` | string | `AGGRESSIVE` / `CONSERVATIVE` / `COLLABORATIVE` / `INNOVATIVE` / `ANALYTICAL` |
| `decisionPattern` | string | `DEMOCRATIC` / `AUTOCRATIC` / `CONSENSUS` / `CONSULTATIVE` |
| `riskTolerance` | number | 0-1 之间 |
| `collaborationPreference` | string | 协作偏好描述 |
| `coreValues` | string | 核心价值观 |
| `behaviorGuidelines` | string | 行为准则 |
| `personalityDescription` | string | 性格描述 |

**响应 `data`**：完整 Workspace 对象

---

#### 5.3 获取 Workspace 详情

- **URL**：`GET /api/v1/workspaces/:id`

---

#### 5.4 更新 Workspace 设置

- **URL**：`PUT /api/v1/workspaces/:id/settings`
- **作用**：更新 Workspace 基本信息和性格设置

**请求体**：WorkspaceSettings 对象的可选字段（name / description / personality / properties / status）

---

#### 5.5 删除 Workspace

- **URL**：`DELETE /api/v1/workspaces/:id`

---

## 6. Workspace — Member（团队成员）

**所属页面**：`/workspaces`（Team 标签页）

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `sourceType` | `existing_character` / `derived_definition` / `workspace_created` / `whiteboard_new` |
| `ownership` | `user_asset` / `workspace_owned` |
| `status` | `active` / `inactive` |

---

#### 6.1 获取 Workspace 成员列表

- **URL**：`GET /api/v1/workspaces/:workspaceId/members`
- **作用**：获取工作空间的 AI 团队成员

**响应 `data`**：`[WorkspaceMember]`

**WorkspaceMember 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 成员 ID |
| `characterId` | string | 关联的 Character ID |
| `characterName` | string | 角色名称 |
| `characterAvatar` | string? | 角色头像 |
| `sourceType` | MemberSourceType | 加入方式 |
| `ownership` | MemberOwnershipType | 所有权 |
| `role` | string | 角色职责 |
| `position` | string | 职位名称 |
| `level` | number | 级别 |
| `weight` | number | 权重 |
| `priority` | number | 优先级 |
| `quota` | number | 配额 |
| `reputation` | number | 声誉评分 |
| `tags` | string[] | 标签 |
| `status` | string | 状态 |
| `lastActiveAt` | string | 最后活跃时间 |

---

#### 6.2 获取团队席位列表

- **URL**：`GET /api/v1/workspaces/:workspaceId/team-positions`
- **作用**：获取团队的席位定义（哪些职位已填充/空缺）

**响应 `data`**：`[TeamPosition]`

**TeamPosition 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 席位 ID |
| `roleName` | string | 职责名称 |
| `rolePackage` | string | 角色套餐 |
| `assignedCharacterId` | string? | 已分配的角色 ID |
| `assignedCharacterName` | string? | 已分配的角色名 |
| `assignedCharacterAvatar` | string? | 已分配的角色头像 |
| `status` | string | `filled` / `vacant` |
| `isWorkspaceOwned` | boolean | 是否为 Workspace 私有角色 |

---

#### 6.3 添加成员

- **URL**：`POST /api/v1/workspaces/:workspaceId/members`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `characterId` | string | ✅ | 角色 ID |
| `role` | string | ✅ | 职责 |
| `position` | string | ✅ | 职位 |
| `level` | number | 否 | 级别 |
| `sourceType` | MemberSourceType | ✅ | 来源方式 |

---

#### 6.4 移除成员

- **URL**：`DELETE /api/v1/workspaces/:workspaceId/members/:memberId`

---

## 7. Workspace — Skill Binding（技能绑定）

**所属页面**：`/workspaces`（Skills 标签页）

---

#### 7.1 获取已绑定技能列表

- **URL**：`GET /api/v1/workspaces/:workspaceId/skills`

**响应 `data`**：`[WorkspaceSkillBinding]`

**WorkspaceSkillBinding 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `skillId` | string | 技能 ID |
| `skillName` | string | 技能名称 |
| `skillCategory` | string | 分类 |
| `latestVersion` | string | 最新版本 |
| `currentVersion` | string | 当前使用版本 |
| `useLatest` | boolean | 是否跟随最新版本 |
| `enabled` | boolean | 是否启用 |
| `runtimeState` | SkillRuntimeStatus | 运行时状态 |
| `boundAt` | string | 绑定时间 |

---

#### 7.2 绑定技能

- **URL**：`POST /api/v1/workspaces/:workspaceId/skills`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `skillId` | string | ✅ | 技能 ID |
| `pinnedVersion` | string | 否 | 固定版本 |
| `useLatest` | boolean | 否 | 是否跟随最新版本，默认 true |

---

#### 7.3 解绑技能

- **URL**：`DELETE /api/v1/workspaces/:workspaceId/skills/:skillId`

---

#### 7.4 更新技能绑定配置

- **URL**：`PUT /api/v1/workspaces/:workspaceId/skills/:skillId`

**请求体**

| 字段 | 类型 | 说明 |
|------|------|------|
| `pinnedVersion` | string? | 固定版本 |
| `useLatest` | boolean? | 是否跟随最新 |
| `enabled` | boolean? | 是否启用 |

---

## 8. Workspace — Memory（记忆配置）

**所属页面**：`/workspaces`（Memory 标签页）

---

#### 8.1 获取 Workspace 记忆配置信息

- **URL**：`GET /api/v1/workspaces/:workspaceId/memory`

**响应 `data`**：MemoryInfo 对象

**MemoryInfo 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `backend` | string | 存储后端 |
| `provider` | string | 向量化提供商 |
| `model` | string | 嵌入模型名 |
| `sourceRange` | string | 数据源范围描述 |
| `fileCount` | number | 文件数量 |
| `chunkCount` | number | 片段数量 |
| `dirtyFlag` | boolean | 是否有未同步变更 |
| `ftsStatus` | string | 全文搜索状态：`enabled` / `disabled` / `error` |
| `vectorStatus` | string | 向量搜索状态：`ready` / `pending` / `error` |
| `embeddingStatus` | string | 向量化状态：`ready` / `pending` / `error` |
| `lastSyncAt` | string | 最后同步时间 |
| `lastSyncStatus` | string | `success` / `failed` |

---

#### 8.2 触发记忆同步

- **URL**：`POST /api/v1/workspaces/:workspaceId/memory/sync`

**响应 `data`**：`{ "success": true, "syncId": "string" }`

---

## 9. Workspace — Observer（观测者绑定）

**所属页面**：`/workspaces`（Observer 标签页）

---

#### 9.1 获取 Workspace Observer 信息

- **URL**：`GET /api/v1/workspaces/:workspaceId/observer`

**响应 `data`**：ObserverInfo 对象

**ObserverInfo 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `observerId` | string? | Observer ID |
| `observerName` | string? | Observer 名称 |
| `status` | string | `active` / `paused` / `unbound` |
| `evaluationMode` | string | `auto` / `manual` |
| `optimizationThreshold` | number | 触发优化的分数阈值 |
| `autoOptimization` | boolean | 是否自动优化 |
| `routineCheck` | boolean | 是否定期检查 |
| `cycleStrategy` | string | `daily` / `weekly` / `monthly` |
| `pendingApprovalCount` | number | 待审批数量 |

---

#### 9.2 绑定 Observer

- **URL**：`POST /api/v1/workspaces/:workspaceId/observer`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `observerId` | string | ✅ | Observer ID |
| `evaluationMode` | string | 否 | `auto` / `manual` |
| `autoOptimization` | boolean | 否 | 是否自动优化 |

---

#### 9.3 解绑 Observer

- **URL**：`DELETE /api/v1/workspaces/:workspaceId/observer`

---

## 10. Workspace — Task（任务）

**所属页面**：`/workspaces`（Tasks 标签页）

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `status` | `pending` / `running` / `completed` / `failed` / `cancelled` |

---

#### 10.1 获取 Workspace 任务列表

- **URL**：`GET /api/v1/workspaces/:workspaceId/tasks`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | 状态筛选 |
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |

**响应 `data`**：`{ "list": [WorkspaceTask], "total": number }`

**WorkspaceTask 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `taskId` | string | 任务 ID |
| `summary` | string | 任务摘要 |
| `status` | TaskStatus | 状态 |
| `owner` | string | 执行者 |
| `createdAt` | string | 创建时间 |
| `startTime` | string? | 开始时间 |
| `endTime` | string? | 结束时间 |
| `resultSummary` | string? | 结果摘要 |
| `hasError` | boolean | 是否有错误 |

---

## 11. Workspace — Audit Log（审计日志）

**所属页面**：`/workspaces`（Logs 标签页）

---

#### 11.1 获取 Workspace 审计日志

- **URL**：`GET /api/v1/workspaces/:workspaceId/audit-logs`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `targetType` | string | `workspace` / `member` / `skill` / `observer` / `task` |
| `actionType` | string | `create` / `update` / `delete` / `bind` / `unbind` / `execute` / `approve` |
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |

**响应 `data`**：`{ "list": [AuditLog], "total": number }`

**AuditLog 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 日志 ID |
| `targetType` | LogTargetType | 操作对象类型 |
| `targetId` | string | 对象 ID |
| `targetName` | string | 对象名称 |
| `actionType` | LogActionType | 操作类型 |
| `operator` | string | 操作人 ID |
| `createdAt` | string | 操作时间 |
| `detailsSummary` | string | 操作详情摘要 |

---

## 12. Workspace — Material（素材）

**所属页面**：`/workspaces`（Materials 标签页）

---

#### 12.1 获取素材列表

- **URL**：`GET /api/v1/workspaces/:workspaceId/materials`

**响应 `data`**：`[WorkspaceMaterial]`

**WorkspaceMaterial 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `materialId` | string | 素材 ID |
| `filename` | string | 文件名 |
| `type` | string | `document` / `image` / `data` / `other` |
| `size` | number | 文件大小（bytes） |
| `sizeFormatted` | string | 格式化后的大小（如 "2.3 MB"） |
| `uploader` | string | 上传者 |
| `uploadTime` | string | 上传时间 |

---

#### 12.2 上传素材

- **URL**：`POST /api/v1/workspaces/:workspaceId/materials`
- **Content-Type**：`multipart/form-data`

**请求体（Form Data）**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | File | ✅ | 上传的文件 |

---

#### 12.3 删除素材

- **URL**：`DELETE /api/v1/workspaces/:workspaceId/materials/:materialId`

---

## 13. Workspace — Permission（权限）

**所属页面**：`/workspaces`（Permissions 标签页）

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `role` | `owner` / `admin` / `operator` / `viewer` |

---

#### 13.1 获取权限成员列表

- **URL**：`GET /api/v1/workspaces/:workspaceId/permissions`

**响应 `data`**：`[WorkspaceMemberPermission]`

**WorkspaceMemberPermission 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | string | 用户 ID |
| `userName` | string | 用户名 |
| `userAvatar` | string? | 头像 |
| `role` | MemberRole | 角色权限 |
| `addedAt` | string | 加入时间 |

---

#### 13.2 添加成员权限

- **URL**：`POST /api/v1/workspaces/:workspaceId/permissions`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | ✅ | 用户 ID |
| `role` | MemberRole | ✅ | 权限角色 |

---

#### 13.3 更新成员权限

- **URL**：`PUT /api/v1/workspaces/:workspaceId/permissions/:userId`

**请求体**：`{ "role": MemberRole }`

---

#### 13.4 移除成员权限

- **URL**：`DELETE /api/v1/workspaces/:workspaceId/permissions/:userId`

---

## 14. Skill（技能）

**所属页面**：`/skills`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `visibility` | `PRIVATE` / `WORKSPACE` / `PUBLIC` |
| `assetState` | `DRAFT` / `PUBLISHED` / `DISABLED` |
| `runtimeStatus` | `NOT_LOADED` / `LOADING` / `LOADED` / `ERROR` |
| `contentStructure.type` | `form` / `document` / `hybrid` |

---

#### 14.1 获取技能列表

- **URL**：`GET /api/v1/skills`
- **作用**：分页获取技能列表

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |
| `search` | string | 搜索 |
| `visibility` | string | `PRIVATE` / `WORKSPACE` / `PUBLIC` / `all` |
| `assetState` | string | `DRAFT` / `PUBLISHED` / `DISABLED` / `all` |
| `runtimeStatus` | string | 运行时状态筛选 |
| `category` | string | 分类筛选 |

**响应 `data`**：`{ "list": [Skill], "total": number }`

**Skill 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 唯一 ID |
| `name` | string | 名称 |
| `category` | string | 分类 |
| `tags` | string[] | 标签 |
| `description` | string | 描述 |
| `creator` | string | 创建者 |
| `creatorAvatar` | string? | 创建者头像 |
| `visibility` | SkillVisibility | 可见性 |
| `enabled` | boolean | 是否启用 |
| `currentVersion` | string | 当前版本 |
| `contentStructure` | SkillContentStructure | 内容结构 |
| `runtimeState` | SkillRuntimeStatus | 运行时状态 |
| `runtimeError` | string? | 运行时错误信息 |
| `workspaceBindings` | WorkspaceSkillBinding[] | Workspace 绑定列表 |
| `characterBindings` | `{characterId, characterName}[]` | 角色绑定列表 |
| `versions` | SkillVersion[] | 版本历史 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

---

#### 14.2 创建技能

- **URL**：`POST /api/v1/skills`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | 名称 |
| `category` | string | ✅ | 分类 |
| `description` | string | ✅ | 描述 |
| `visibility` | SkillVisibility | ✅ | 可见性 |
| `tags` | string[] | 否 | 标签 |
| `contentStructure` | SkillContentStructure | ✅ | 内容结构 |

---

#### 14.3 获取技能详情

- **URL**：`GET /api/v1/skills/:id`

---

#### 14.4 更新技能

- **URL**：`PUT /api/v1/skills/:id`

---

#### 14.5 发布技能版本

- **URL**：`POST /api/v1/skills/:id/publish`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `version` | string | ✅ | 版本号（如 `1.0.0`） |
| `changelog` | string | ✅ | 变更日志 |

**响应 `data`**：完整 Skill 对象（含新版本）

---

#### 14.6 删除技能

- **URL**：`DELETE /api/v1/skills/:id`

---

#### 14.7 保存技能草稿

- **URL**：`PUT /api/v1/skills/:id/draft`

**请求体**：`{ "content": SkillContentStructure }`

---

## 15. Tool（工具）

**所属页面**：`/tools`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `toolType` | `SCRIPT` / `API` / `MCP` / `FUNCTION` |
| `invocationType` | `python` / `shell` / `rest` / `graphql` / `mcp` / `native` |
| `visibility` | `PRIVATE` / `WORKSPACE` / `PUBLIC` |
| `runtimeState` | `NOT_REGISTERED` / `REGISTERED` / `ACTIVE` / `ERROR` |

---

#### 15.1 获取工具列表

- **URL**：`GET /api/v1/tools`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |
| `search` | string | 搜索 |
| `visibility` | string | 可见性筛选 |
| `toolType` | string | 类型筛选 |
| `runtimeStatus` | string | 运行时状态筛选 |
| `category` | string | 分类筛选 |

**响应 `data`**：`{ "list": [Tool], "total": number }`

**Tool 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 唯一 ID |
| `name` | string | 名称 |
| `category` | string | 分类 |
| `tags` | string[] | 标签 |
| `description` | string | 描述 |
| `creator` | string | 创建者 |
| `toolType` | ToolType | 工具类型 |
| `invocationType` | ToolInvocationType | 调用方式 |
| `visibility` | ToolVisibility | 可见性 |
| `enabled` | boolean | 是否启用 |
| `currentVersion` | string | 当前版本 |
| `parameters` | ToolParameter[] | 参数定义 |
| `returnValue` | ToolReturnValue | 返回值定义 |
| `dependencies` | ToolDependency[] | 依赖列表 |
| `versions` | ToolVersion[] | 版本历史 |
| `runtimeState` | ToolRuntimeStatus | 运行时状态 |
| `runtimeError` | string? | 运行时错误 |
| `skillBindings` | `{skillId, skillName}[]` | 技能绑定 |
| `characterBindings` | `{characterId, characterName}[]` | 角色绑定 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

**ToolParameter 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | string | 参数名 |
| `type` | string | `string` / `number` / `boolean` / `object` / `array` / `file` |
| `description` | string | 描述 |
| `required` | boolean | 是否必填 |
| `defaultValue` | any? | 默认值 |
| `validation` | object? | 校验规则（pattern/min/max/enum） |

---

#### 15.2 创建工具

- **URL**：`POST /api/v1/tools`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | 名称 |
| `category` | string | ✅ | 分类 |
| `description` | string | ✅ | 描述 |
| `toolType` | ToolType | ✅ | 类型 |
| `invocationType` | ToolInvocationType | ✅ | 调用方式 |
| `visibility` | ToolVisibility | ✅ | 可见性 |
| `parameters` | ToolParameter[] | 否 | 参数定义 |
| `returnValue` | ToolReturnValue | 否 | 返回值定义 |
| `definition` | ToolDefinition | ✅ | 实现定义 |

**ToolDefinition 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `invocationType` | ToolInvocationType | 调用方式 |
| `content` | string | 脚本代码/API 配置/MCP 配置/函数签名 |
| `requirements` | string[]? | 依赖包 |
| `timeout` | number? | 超时时间（ms） |
| `retry` | `{maxAttempts, backoffMs}`? | 重试配置 |

---

#### 15.3 获取工具详情

- **URL**：`GET /api/v1/tools/:id`

---

#### 15.4 更新工具

- **URL**：`PUT /api/v1/tools/:id`

---

#### 15.5 发布工具版本

- **URL**：`POST /api/v1/tools/:id/publish`

**请求体**：`{ "version": string, "changelog": string, "definition": ToolDefinition }`

---

#### 15.6 删除工具

- **URL**：`DELETE /api/v1/tools/:id`

---

## 16. Memory — Source（数据源）

**所属页面**：`/memory/sources`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `sourceType` | `file` / `url` / `api` / `chat` / `fused` |
| `status` | `active` / `error` / `disabled` / `syncing` |

---

#### 16.1 获取数据源列表

- **URL**：`GET /api/v1/memory/sources`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `search` | string | 搜索 |
| `sourceType` | string | 类型筛选 |
| `status` | string | 状态筛选 |

**响应 `data`**：`[SourceDocument]`

**SourceDocument 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 数据源 ID |
| `title` | string | 名称 |
| `sourcePath` | string | 数据源路径（文件路径/URL/API endpoint） |
| `sourceType` | string | 类型 |
| `backend` | string | 存储后端 |
| `provider` | string | 向量化提供商 |
| `enabled` | boolean | 是否启用 |
| `status` | string | 状态 |
| `lastIndexedAt` | string | 最后索引时间 |
| `itemCount` | number | 索引条目数 |
| `fileCount` | number | 文件数量 |
| `errorMessage` | string? | 错误信息 |
| `isFusedSource` | boolean? | 是否为融合数据源 |

---

#### 16.2 添加数据源

- **URL**：`POST /api/v1/memory/sources`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `title` | string | ✅ | 名称 |
| `sourceType` | string | ✅ | 类型 |
| `sourcePath` | string | ✅ | 路径/URL |
| `backend` | string | ✅ | 存储后端 |
| `provider` | string | ✅ | 向量化提供商 |

---

#### 16.3 获取数据源详情（含文件列表）

- **URL**：`GET /api/v1/memory/sources/:sourceId`

**响应 `data`**：SourceDocument 对象（含 `files` 字段）

---

#### 16.4 触发数据源同步

- **URL**：`POST /api/v1/memory/sources/:sourceId/sync`

---

#### 16.5 删除数据源

- **URL**：`DELETE /api/v1/memory/sources/:sourceId`

---

## 17. Memory — File（记忆文件）

**所属页面**：`/memory/content`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `fileType` | `markdown` / `json` / `text` / `other` |
| `syncStatus` | `synced` / `syncing` / `pending` / `failed` / `disabled` |
| `healthStatus` | `healthy` / `warning` / `error` / `unknown` |

---

#### 17.1 获取记忆文件列表

- **URL**：`GET /api/v1/memory/files`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `sourceId` | string | 按数据源筛选 |
| `search` | string | 搜索 |
| `syncStatus` | string | 同步状态筛选 |
| `healthStatus` | string | 健康状态筛选 |
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |

**响应 `data`**：`{ "list": [MemoryFile], "total": number }`

**MemoryFile 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 文件 ID |
| `sourceId` | string | 所属数据源 ID |
| `title` | string | 文件标题 |
| `description` | string? | 描述 |
| `filePath` | string | 文件路径 |
| `fileType` | string | 文件类型 |
| `chunkCount` | number | 片段数量 |
| `totalSize` | number | 总大小（bytes） |
| `syncStatus` | MemorySyncStatus | 同步状态 |
| `healthStatus` | MemoryHealthStatus | 健康状态 |
| `bindings` | Binding[] | 绑定关系列表 |
| `lastSyncAt` | string | 最后同步时间 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

---

#### 17.2 获取记忆文件详情

- **URL**：`GET /api/v1/memory/files/:fileId`

---

## 18. Memory — Chunk（记忆片段）

**所属页面**：`/memory/content`

---

#### 18.1 获取记忆片段列表

- **URL**：`GET /api/v1/memory/chunks`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `fileId` | string | 按文件 ID 筛选 |
| `search` | string | 按内容/标题搜索 |
| `indexedStatus` | string | `indexed` / `pending` / `failed` / `all` |
| `tags` | string | 标签（逗号分隔） |
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |

**响应 `data`**：`{ "list": [MemoryChunk], "total": number }`

**MemoryChunk 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 片段 ID |
| `fileId` | string | 所属文件 ID |
| `title` | string | 标题 |
| `content` | string | 内容 |
| `summary` | string | 摘要 |
| `tags` | string[] | 标签 |
| `indexedStatus` | string | 索引状态 |
| `relations` | string[] | 关联片段 ID |
| `sourceLocation` | `{startLine, endLine}`? | 在源文件中的位置 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |
| `fusedFrom` | FusedFrom[]? | 融合来源（仅融合片段有） |

**FusedFrom 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `chunkId` | string | 来源片段 ID |
| `chunkTitle` | string | 来源片段标题 |
| `sourceName` | string | 来源名称 |
| `fusedAt` | string | 融合时间 |

---

#### 18.2 创建/编辑记忆片段

- **URL**：`POST /api/v1/memory/chunks` 或 `PUT /api/v1/memory/chunks/:chunkId`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileId` | string | ✅（创建时） | 所属文件 ID |
| `title` | string | ✅ | 标题 |
| `content` | string | ✅ | 内容 |
| `summary` | string | 否 | 摘要 |
| `tags` | string[] | 否 | 标签 |

---

#### 18.3 批量打标签

- **URL**：`POST /api/v1/memory/chunks/batch-tag`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `chunkIds` | string[] | ✅ | 片段 ID 列表 |
| `tags` | string[] | ✅ | 要添加的标签 |
| `action` | string | ✅ | `add` / `remove` / `replace` |

---

#### 18.4 批量压缩（融合）片段

- **URL**：`POST /api/v1/memory/chunks/compress`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `chunkIds` | string[] | ✅ | 待压缩片段 ID |
| `targetFileId` | string | ✅ | 目标融合文件 ID |
| `title` | string | 否 | 融合片段标题 |

---

#### 18.5 删除记忆片段

- **URL**：`DELETE /api/v1/memory/chunks/:chunkId`

---

## 19. Memory — Binding（绑定关系）

**所属页面**：`/memory`（Characters / Workspaces 标签页）

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `targetType` | `character` / `workspace` |
| `mountType` | `full` / `selective` / `rule` |

---

#### 19.1 获取绑定列表

- **URL**：`GET /api/v1/memory/bindings`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `fileId` | string | 按文件筛选 |
| `targetType` | string | 按挂载目标类型筛选 |
| `targetId` | string | 按挂载目标 ID 筛选 |

**响应 `data`**：`[Binding]`

**Binding 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 绑定 ID |
| `fileId` | string | 记忆文件 ID |
| `targetType` | MemoryScope | 挂载目标类型 |
| `targetId` | string | 挂载目标 ID |
| `targetName` | string | 挂载目标名称 |
| `mountType` | MountType | 挂载方式 |
| `selectedChunkIds` | string[]? | 选择性挂载的片段 ID |
| `mountRules` | MountRule[]? | 规则挂载的规则列表 |
| `mountedAt` | string | 挂载时间 |
| `mountedBy` | string | 挂载操作者 |

---

#### 19.2 创建绑定

- **URL**：`POST /api/v1/memory/bindings`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileId` | string | ✅ | 文件 ID |
| `targetType` | MemoryScope | ✅ | 目标类型 |
| `targetId` | string | ✅ | 目标 ID |
| `mountType` | MountType | ✅ | 挂载方式 |
| `selectedChunkIds` | string[]? | 否 | 选择性挂载时必填 |
| `mountRules` | MountRule[]? | 否 | 规则挂载时必填 |

---

#### 19.3 删除绑定

- **URL**：`DELETE /api/v1/memory/bindings/:bindingId`

---

## 20. Memory — Retrieval（检索）

**所属页面**：`/memory/retrieval`

---

#### 20.1 检索记忆

- **URL**：`POST /api/v1/memory/retrieval/search`
- **作用**：向量/全文混合检索

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `query` | string | ✅ | 查询文本 |
| `scopeType` | string | 否 | `character` / `workspace` / `all` |
| `scopeId` | string | 否 | 指定作用域 ID |
| `topK` | number | 否 | 返回条数，默认 10 |
| `minScore` | number | 否 | 最低相关度，0-1 |

**响应 `data`**：`[RetrievalResult]`

**RetrievalResult 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 结果 ID |
| `path` | string | 来源路径 |
| `source` | string | 来源名称 |
| `score` | number | 相关度分数（0-1） |
| `startLine` | number | 起始行 |
| `endLine` | number | 结束行 |
| `snippet` | string | 内容摘要 |
| `citation` | string? | 引用信息 |
| `fileId` | string? | 所属文件 ID |
| `chunkId` | string? | 所属片段 ID |

---

## 21. Memory — Config（记忆配置）

**所属页面**：`/memory/config`

---

#### 21.1 获取 Character 记忆配置

- **URL**：`GET /api/v1/memory/config/character/:characterId`

**响应 `data`**：CharacterMemoryConfig 对象

---

#### 21.2 更新 Character 记忆配置

- **URL**：`PUT /api/v1/memory/config/character/:characterId`

**请求体（部分字段）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `enabled` | boolean | 是否启用 |
| `privateMemoryEnabled` | boolean | 私有记忆开关 |
| `defaultSources` | string[] | 默认数据源 |
| `queryParams` | object | 查询参数（maxResults/minScore/includeSessionRecords） |

---

#### 21.3 获取 Workspace 记忆配置

- **URL**：`GET /api/v1/memory/config/workspace/:workspaceId`

---

#### 21.4 更新 Workspace 记忆配置

- **URL**：`PUT /api/v1/memory/config/workspace/:workspaceId`

**请求体（部分字段）**

| 字段 | 类型 | 说明 |
|------|------|------|
| `enabled` | boolean | 是否启用 |
| `backend` | string | 存储后端 |
| `provider` | string | 向量化提供商 |
| `model` | string | 嵌入模型 |
| `syncStrategy` | string | `auto` / `manual` / `scheduled` |
| `queryStrategy` | object | 查询策略（useVector/useFts/useCache） |

---

#### 21.5 获取记忆运行时状态

- **URL**：`GET /api/v1/memory/runtime-status`

**响应 `data`**：RuntimeStatus 对象

---

## 22. Observer（观测者）

**所属页面**：`/observers`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `targetType` | `character` / `workspace` |
| `status` | `active` / `inactive` / `paused` |
| `executionMode` | `auto` / `manual` / `plan_approval` |

---

#### 22.1 获取 Observer 列表

- **URL**：`GET /api/v1/observers`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `search` | string | 搜索 |
| `targetType` | string | 监控对象类型 |
| `status` | string | 状态筛选 |
| `executionMode` | string | 执行模式筛选 |
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |

**响应 `data`**：`{ "list": [Observer], "total": number }`

**Observer 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | Observer ID |
| `name` | string | 名称 |
| `description` | string | 描述 |
| `targetType` | TargetType | 监控对象类型 |
| `targetId` | string | 监控对象 ID |
| `targetName` | string | 监控对象名称 |
| `status` | ObserverStatus | 状态 |
| `executionMode` | ExecutionMode | 执行模式 |
| `lastEvaluationAt` | string? | 最后评价时间 |
| `lastScore` | number? | 最新评分 |
| `pendingPlansCount` | number | 待审批计划数 |
| `pendingActionsCount` | number | 待执行动作数 |
| `autoApprovalEnabled` | boolean | 是否自动审批 |
| `reviewerIds` | string[] | 审批者 ID 列表 |
| `reviewerNames` | string[] | 审批者名称列表 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

---

#### 22.2 创建 Observer

- **URL**：`POST /api/v1/observers`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | 名称 |
| `description` | string | 否 | 描述 |
| `targetType` | TargetType | ✅ | 监控对象类型 |
| `targetId` | string | ✅ | 监控对象 ID |
| `executionMode` | ExecutionMode | ✅ | 执行模式 |
| `autoApprovalEnabled` | boolean | 否 | 默认 false |
| `reviewerIds` | string[] | 否 | 审批者列表 |

---

#### 22.3 获取 Observer 详情

- **URL**：`GET /api/v1/observers/:id`

---

#### 22.4 更新 Observer

- **URL**：`PUT /api/v1/observers/:id`

---

#### 22.5 删除 Observer

- **URL**：`DELETE /api/v1/observers/:id`

---

#### 22.6 触发评价

- **URL**：`POST /api/v1/observers/:id/evaluate`
- **作用**：手动触发一次评价分析

**响应 `data`**：`{ "evaluationId": "string", "status": "running" }`

---

## 23. Observer — Evaluation（评价记录）

**所属页面**：`/observers/[id]`

---

#### 23.1 获取评价记录列表

- **URL**：`GET /api/v1/observers/:observerId/evaluations`

**Query 参数**：`page` / `pageSize` / `status`

**响应 `data`**：`{ "list": [EvaluationRecord], "total": number }`

**EvaluationRecord 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 评价 ID |
| `observerId` | string | Observer ID |
| `targetType` | TargetType | 监控对象类型 |
| `targetId` | string | 监控对象 ID |
| `overallScore` | number | 综合评分（0-100） |
| `findings` | Finding[] | 发现问题列表 |
| `evidenceRefs` | string[] | 证据引用 |
| `analysis` | string | 分析报告 |
| `unsafeFlags` | string[] | 安全问题标记 |
| `status` | EvaluationRecordStatus | 状态：`completed` / `failed` / `running` |
| `createdAt` | string | 评价时间 |

**Finding 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | string | 问题类型 |
| `severity` | string | `low` / `medium` / `high` |
| `title` | string | 标题 |
| `description` | string | 描述 |

---

## 24. Observer — Plan（优化计划）

**所属页面**：`/observers/[id]`

---

#### 24.1 获取优化计划列表

- **URL**：`GET /api/v1/observers/:observerId/plans`

**响应 `data`**：`{ "list": [OptimizationPlan], "total": number }`

**OptimizationPlan 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 计划 ID |
| `observerId` | string | Observer ID |
| `evaluationId` | string | 关联评价 ID |
| `targetType` | TargetType | 目标类型 |
| `targetId` | string | 目标 ID |
| `title` | string | 计划标题 |
| `summary` | string | 计划摘要 |
| `status` | PlanStatus | 状态 |
| `items` | OptimizationPlanItem[] | 计划动作列表 |
| `approver` | string? | 审批者 ID |
| `approverName` | string? | 审批者名称 |
| `approvalComment` | string? | 审批意见 |
| `approvedAt` | string? | 审批时间 |
| `executedAt` | string? | 执行时间 |
| `createdAt` | string | 创建时间 |

---

#### 24.2 审批计划

- **URL**：`POST /api/v1/observers/:observerId/plans/:planId/approve`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `approved` | boolean | ✅ | 是否批准 |
| `comment` | string | 否 | 审批意见 |

---

#### 24.3 执行计划

- **URL**：`POST /api/v1/observers/:observerId/plans/:planId/execute`

---

## 25. Observer — Action（优化动作）

**所属页面**：`/observers/[id]`

---

#### 25.1 获取优化动作列表

- **URL**：`GET /api/v1/observers/:observerId/actions`

**Query 参数**：`status` / `page` / `pageSize`

**响应 `data`**：`{ "list": [OptimizationAction], "total": number }`

**OptimizationAction 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 动作 ID |
| `evaluationId` | string | 关联评价 ID |
| `observerId` | string | Observer ID |
| `targetType` | TargetType | 目标类型 |
| `targetId` | string | 目标 ID |
| `actionType` | string | 动作类型（如 `update_prompt` / `add_trait`） |
| `parameters` | object | 动作参数 |
| `status` | ActionStatus | 状态 |
| `executionMode` | ExecutionMode | 执行模式 |
| `result` | string? | 执行结果 |
| `executedAt` | string? | 执行时间 |
| `createdAt` | string | 创建时间 |

---

#### 25.2 回滚动作

- **URL**：`POST /api/v1/observers/:observerId/actions/:actionId/rollback`

---

## 26. Observer — Governance Log（治理日志）

**所属页面**：`/observers/[id]`

---

#### 26.1 获取治理日志

- **URL**：`GET /api/v1/observers/:observerId/governance-logs`

**响应 `data`**：`{ "list": [GovernanceLog], "total": number }`

**GovernanceLog 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 日志 ID |
| `observerId` | string | Observer ID |
| `action` | string | 动作描述 |
| `operator` | string | 操作者 ID |
| `operatorName` | string | 操作者名称 |
| `target` | string | 操作目标描述 |
| `details` | string | 详细信息 |
| `createdAt` | string | 时间 |

---

## 27. Config（配置中心）

**所属页面**：`/config`、`/config/browser`、`/config/structured`、`/config/advanced`

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `domain` | `STUDIO` / `CHARACTER` / `WORKSPACE` / `WORKSPACE_MEMBER` / `SKILL` / `WORKSPACE_SKILL` / `MEMORY_SCOPE` / `OBSERVER` |
| `dataType` | `STRING` / `NUMBER` / `BOOLEAN` / `ENUM` / `LIST` / `OBJECT` / `JSON` |
| `effectMode` | `IMMEDIATE` / `RELEASE` |
| `changeType` | `CREATE` / `UPDATE` / `DELETE` / `RELEASE` / `ROLLBACK` |

---

#### 27.1 获取配置项列表

- **URL**：`GET /api/v1/config/items`
- **作用**：获取配置项列表，支持按域、作用域、草稿状态筛选

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `domain` | string | 配置域筛选 |
| `scopeType` | string | 作用域类型筛选 |
| `scopeId` | string | 特定作用域 ID |
| `search` | string | 按 key/name 搜索 |
| `isDraft` | boolean | 是否只看草稿 |
| `hasOverride` | boolean | 是否有覆盖 |
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |

**响应 `data`**：`{ "list": [ConfigItem], "total": number }`

**ConfigItem 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 配置项 ID |
| `domain` | ConfigDomain | 所属域 |
| `scopeType` | ConfigScopeType | 作用域类型 |
| `scopeId` | string? | 特定作用域 ID |
| `key` | string | 配置键 |
| `name` | string | 配置名称 |
| `description` | string? | 描述 |
| `dataType` | ConfigDataType | 数据类型 |
| `defaultValue` | any | 默认值 |
| `currentValue` | any? | 当前值 |
| `effectiveValue` | any? | 生效值 |
| `source` | string? | 来源链 |
| `editMode` | EditMode | 编辑模式（`STRUCTURED` / `KV`） |
| `effectMode` | EffectMode | 生效策略 |
| `validationRules` | ValidationRule[]? | 校验规则 |
| `options` | ConfigOption[]? | 枚举选项 |
| `isDraft` | boolean? | 是否为草稿 |
| `draftValue` | any? | 草稿值 |
| `publishedValue` | any? | 已发布值 |
| `lastModified` | string? | 最后修改时间 |
| `modifiedBy` | string? | 修改者 |
| `group` | string? | 分组名称 |

---

#### 27.2 更新配置项（保存草稿）

- **URL**：`PUT /api/v1/config/items/:id`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `value` | any | ✅ | 新的配置值 |
| `saveAsDraft` | boolean | 否 | 是否保存为草稿（默认 false） |

---

#### 27.3 发布配置草稿

- **URL**：`POST /api/v1/config/items/:id/publish`

**响应 `data`**：更新后的 ConfigItem 对象

---

#### 27.4 回滚配置

- **URL**：`POST /api/v1/config/items/:id/rollback`

**请求体**：`{ "changeRecordId": "string" }` 回滚到指定变更记录的版本

---

#### 27.5 获取生效链

- **URL**：`GET /api/v1/config/items/:id/effect-chain`
- **作用**：获取某配置项从全局到具体作用域的生效链节点

**响应 `data`**：`[EffectChainNode]`

---

#### 27.6 获取变更记录

- **URL**：`GET /api/v1/config/change-records`

**Query 参数**：`domain` / `configItemId` / `page` / `pageSize`

**响应 `data`**：`{ "list": [ConfigChangeRecord], "total": number }`

---

#### 27.7 获取影响分析

- **URL**：`GET /api/v1/config/items/:id/impact`
- **作用**：分析某配置项的变更会影响哪些作用域

**响应 `data`**：ImpactAnalysis 对象

---

## 28. Logs — Event（事件日志）

**所属页面**：`/logs`（Log View 标签页）

### 枚举值

| 字段 | 枚举值 |
|------|--------|
| `targetType` | `character` / `workspace` / `skill` / `memory` / `observer` / `task` |
| `eventType` | `create` / `update` / `execute` / `load` / `fail` / `approve` / `rollback` / `delete` / `sync` / `deploy` / `evaluate` / `generate_plan` |
| `severity` | `info` / `warning` / `error` / `critical` |

---

#### 28.1 获取事件日志列表

- **URL**：`GET /api/v1/logs/events`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `targetType` | string | 对象类型筛选 |
| `targetId` | string | 对象 ID 筛选 |
| `eventType` | string | 事件类型筛选 |
| `severity` | string | 严重程度筛选 |
| `search` | string | 搜索 |
| `dateStart` | string | 开始时间 |
| `dateEnd` | string | 结束时间 |
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |

**响应 `data`**：`{ "list": [EventRecord], "total": number }`

---

#### 28.2 获取事件详情

- **URL**：`GET /api/v1/logs/events/:id`

**响应 `data`**：完整 EventRecord 对象（含 `details`）

---

## 29. Logs — Trace（链路追踪）

**所属页面**：`/logs`（Trace 标签页）

---

#### 29.1 获取链路列表

- **URL**：`GET /api/v1/logs/traces`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `targetType` | string | 对象类型 |
| `targetId` | string | 对象 ID |
| `traceId` | string | Trace ID 精确查找 |
| `status` | string | `running` / `completed` / `failed` / `all` |
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |

**响应 `data`**：`{ "list": [TraceListItem], "total": number }`

**TraceListItem 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | ID |
| `traceId` | string | Trace ID |
| `rootNodeName` | string | 根节点名称 |
| `rootNodeType` | ObservationTargetType | 根节点类型 |
| `status` | string | 状态 |
| `nodeCount` | number | 节点数量 |
| `startTime` | string | 开始时间 |
| `endTime` | string? | 结束时间 |

---

#### 29.2 获取链路详情（含树形节点）

- **URL**：`GET /api/v1/logs/traces/:traceId`

**响应 `data`**：完整 Trace 对象（含嵌套 TraceNode 树）

---

## 30. Logs — Health（健康状态）

**所属页面**：`/logs`（Health 标签页）

---

#### 30.1 获取健康状态列表

- **URL**：`GET /api/v1/logs/health`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `targetType` | string | 对象类型 |
| `status` | string | `all` / `healthy` / `degraded` / `failed` |

**响应 `data`**：`{ "list": [HealthListItem], "stats": HealthStats }`

---

#### 30.2 获取全局健康统计

- **URL**：`GET /api/v1/logs/health/stats`

**响应 `data`**：HealthStats 对象

| 字段 | 类型 | 说明 |
|------|------|------|
| `totalCharacters` | number | 角色总数 |
| `activeCharacters` | number | 活跃角色数 |
| `totalWorkspaces` | number | Workspace 总数 |
| `activeWorkspaces` | number | 活跃 Workspace 数 |
| `failedSkills` | number | 失败技能数 |
| `memoryAnomalies` | number | 记忆异常数 |
| `pendingApprovals` | number | 待审批数 |
| `recentFailedTasks` | number | 近期失败任务数 |
| `highPriorityExceptions` | number | 高优先级异常数 |

---

## 31. Logs — Audit（审计记录）

**所属页面**：`/logs`（Audit 标签页）

---

#### 31.1 获取审计记录

- **URL**：`GET /api/v1/logs/audit`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `targetType` | string | 对象类型 |
| `operator` | string | 操作者 |
| `action` | string | 操作类型 |
| `dateStart` | string | 开始时间 |
| `dateEnd` | string | 结束时间 |
| `page` | number | 页码 |
| `pageSize` | number | 每页数量 |

**响应 `data`**：`{ "list": [AuditRecord], "total": number }`

---

## 32. Chat（AI 对话）

**所属页面**：`/chat`

---

#### 32.1 发送消息

- **URL**：`POST /api/v1/chat/messages`
- **作用**：向 AI 发送消息，获取回复

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `sessionId` | string | 否 | 会话 ID，不传则自动创建 |
| `message` | string | ✅ | 用户消息 |
| `characterId` | string | 否 | 指定角色 ID |

**响应 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| `sessionId` | string | 会话 ID |
| `messageId` | string | 消息 ID |
| `role` | string | `assistant` |
| `content` | string | AI 回复内容 |
| `timestamp` | string | 时间 |

---

#### 32.2 获取会话历史

- **URL**：`GET /api/v1/chat/sessions/:sessionId/messages`

**响应 `data`**：`[{ id, role, content, timestamp }]`

---

#### 32.3 获取 AI 连接状态

- **URL**：`GET /api/v1/chat/status`

**响应 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| `connected` | boolean | 是否已连接 |
| `model` | string | 当前使用模型 |
| `status` | string | `ready` / `busy` / `error` |

---

## 33. Lab — Scene Plugin（场景插件）

**所属页面**：`/lab/scene-plugin`

---

#### 33.1 获取场景列表

- **URL**：`GET /api/v1/lab/scenes`

**响应 `data`**：`[Scene]`

**Scene 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 场景 ID |
| `name` | string | 场景名称 |
| `description` | string | 描述 |
| `type` | string | 类型 ID |
| `typeName` | string | 类型名称 |
| `config` | SceneConfig | 场景配置（入口条件/角色/规则/奖励/Hooks） |
| `status` | string | `draft` / `published` / `archived` |
| `instanceCount` | number | 实例数量 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

---

#### 33.2 创建/更新场景

- **URL**：`POST /api/v1/lab/scenes` 或 `PUT /api/v1/lab/scenes/:id`

---

#### 33.3 获取场景实例列表

- **URL**：`GET /api/v1/lab/scenes/:sceneId/instances`

**响应 `data`**：`[SceneInstance]`

---

## 34. Lab — Task Network（任务网络）

**所属页面**：`/lab/task-network`

---

#### 34.1 获取任务列表

- **URL**：`GET /api/v1/lab/tasks`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `status` | string | 状态筛选 |
| `priority` | string | 优先级筛选 |

**响应 `data`**：`[Task]`（含 schedule / supervision / acceptance 等嵌套对象）

---

#### 34.2 获取任务详情

- **URL**：`GET /api/v1/lab/tasks/:id`

---

#### 34.3 更新任务状态

- **URL**：`PUT /api/v1/lab/tasks/:id/status`

**请求体**：`{ "status": TaskStatus }`

---

#### 34.4 提交验收结果

- **URL**：`POST /api/v1/lab/tasks/:id/acceptance`

**请求体**：`{ "results": AcceptanceResult[], "approved": boolean, "approvedBy": string }`

---

## 35. Lab — Topology（拓扑图）

**所属页面**：`/lab/topology`

---

#### 35.1 获取拓扑图数据

- **URL**：`GET /api/v1/lab/topology`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `nodeTypes` | string | 节点类型过滤（逗号分隔：`workspace,character,skill,tool,memory`） |
| `edgeTypes` | string | 边类型过滤（逗号分隔） |
| `workspaceId` | string | 只展示特定 Workspace 的关系图 |

**响应 `data`**：TopologyGraph 对象

**TopologyGraph 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `nodes` | TopologyNode[] | 节点列表 |
| `edges` | TopologyEdge[] | 边列表 |

**TopologyNode 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 节点 ID |
| `type` | NodeType | 节点类型：`workspace` / `character` / `skill` / `tool` / `memory` |
| `name` | string | 节点名称 |
| `description` | string? | 描述 |
| `size` | string? | `small` / `medium` / `large` |
| `status` | string? | `active` / `inactive` / `warning` |
| `connectionCount` | number? | 连接数量 |

**TopologyEdge 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 边 ID |
| `source` | string | 源节点 ID |
| `target` | string | 目标节点 ID |
| `type` | EdgeType | 边类型：`uses` / `manages` / `collaborates` / `owns` / `depends_on` |
| `weight` | number? | 连接强度（0-1） |
| `label` | string? | 标签 |

---

## 36. Lab — Behavior Arena（行为演练场）

**所属页面**：`/lab/behavior-arena`

---

#### 36.1 获取演练场景列表

- **URL**：`GET /api/v1/lab/arena/scenarios`

**响应 `data`**：`[ArenaScenario]`

---

#### 36.2 创建演练场景

- **URL**：`POST /api/v1/lab/arena/scenarios`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | 名称 |
| `description` | string | ✅ | 描述 |
| `type` | string | ✅ | `dialogue` / `decision` / `conflict` / `negotiation` |
| `config` | ArenaConfig | ✅ | 演练配置 |

**ArenaConfig 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `environment` | string | 环境描述 |
| `characters` | CharacterInArena[] | 参与角色列表 |
| `expectedOutcomes` | string[] | 预期结果 |
| `evaluationCriteria` | string[] | 评估标准 |

---

#### 36.3 运行演练

- **URL**：`POST /api/v1/lab/arena/scenarios/:scenarioId/run`

**响应 `data`**：SimulationRun 对象

---

#### 36.4 获取演练结果

- **URL**：`GET /api/v1/lab/arena/runs/:runId`

**响应 `data`**：完整 SimulationRun 对象（含对话日志/决策点/反馈）

---

## 37. Lab — Skill Composer（技能组合器）

**所属页面**：`/lab/skill-composer`

---

#### 37.1 获取技能图列表

- **URL**：`GET /api/v1/lab/skill-graphs`

**响应 `data`**：`[SkillGraph]`

---

#### 37.2 保存技能图

- **URL**：`POST /api/v1/lab/skill-graphs` 或 `PUT /api/v1/lab/skill-graphs/:id`

**请求体**：SkillGraph 对象（含 nodes 和 edges）

---

#### 37.3 生成执行计划

- **URL**：`POST /api/v1/lab/skill-graphs/:id/plan`

**响应 `data`**：ExecutionPlan 对象

---

#### 37.4 执行技能图

- **URL**：`POST /api/v1/lab/skill-graphs/:id/execute`

**响应 `data`**：`{ "runId": "string", "status": "running" }`

---

## 38. Lab — Timeline（时间线回放）

**所属页面**：`/lab/timeline`

---

#### 38.1 获取可回放会话列表

- **URL**：`GET /api/v1/lab/timeline/sessions`

**Query 参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| `targetType` | string | 对象类型筛选 |
| `targetId` | string | 对象 ID 筛选 |
| `status` | string | `running` / `completed` / `failed` |

**响应 `data`**：`[TimelineSession]`（不含 steps 详细内容）

**TimelineSession 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 会话 ID |
| `traceId` | string | Trace ID |
| `name` | string | 名称 |
| `description` | string | 描述 |
| `targetType` | ReplayerTargetType | 对象类型 |
| `targetId` | string | 对象 ID |
| `targetName` | string | 对象名称 |
| `status` | string | 状态 |
| `startTime` | string | 开始时间 |
| `endTime` | string? | 结束时间 |
| `totalSteps` | number | 总步骤数 |
| `completedSteps` | number | 已完成步骤数 |
| `currentStepIndex` | number | 当前步骤索引 |

---

#### 38.2 获取会话详情（含所有步骤）

- **URL**：`GET /api/v1/lab/timeline/sessions/:sessionId`

**响应 `data`**：完整 TimelineSession 对象（含嵌套 `steps` 树）

**TimelineStep 对象**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 步骤 ID |
| `traceId` | string | Trace ID |
| `name` | string | 步骤名称 |
| `targetType` | ReplayerTargetType | 对象类型 |
| `targetId` | string | 对象 ID |
| `targetName` | string | 对象名称 |
| `eventType` | string | 事件类型 |
| `status` | StepStatus | `pending` / `running` / `completed` / `failed` / `skipped` |
| `startedAt` | string | 开始时间 |
| `completedAt` | string? | 完成时间 |
| `duration` | number? | 耗时（ms） |
| `input` | object? | 输入数据 |
| `output` | object? | 输出数据 |
| `error` | string? | 错误信息 |
| `parentId` | string? | 父步骤 ID |
| `children` | TimelineStep[]? | 子步骤 |
| `message` | string? | 描述信息 |
| `operator` | string? | 操作者 |

---

*文档生成时间：2026-04-03*
*版本：v1.0*