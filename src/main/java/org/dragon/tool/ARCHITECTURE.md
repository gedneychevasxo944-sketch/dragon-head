# Tool 模块架构说明

> 本文档根据代码和注释整理，描述 `tool/` 模块的完整设计，包括领域模型、生命周期管理、运行时注册、执行调用、LLM 适配、MCP 集成与存储层。

---

## 一、领域模型层（Domain Layer）

### 核心对象关系

```
ToolDO  ──publishedVersionId──▶  ToolVersionDO
  │                                  │
  │ toolType                         │ executionConfig（JSON，各类型格式不同）
  │ visibility                       │ parameters / requiredParams / aliases
  │ builtin                          │ storageType / storageInfo（文件存储元信息）
  │ status                           └──────────────────────────────────────────
  │
  └──── ToolBindingDO（scopeType: WORKSPACE | CHARACTER）
```

| 对象 | 说明 |
|------|------|
| `ToolDO` | 工具主记录，含名称、类型、可见性、是否内置（builtin）、已发布版本指针（publishedVersionId） |
| `ToolVersionDO` | 工具版本，存储 LLM 声明（name/description/parameters）和执行配置（executionConfig），每次更新 INSERT 新版本 |
| `ToolBindingDO` | 绑定关系，有记录即绑定生效，删除记录即解绑，无 enabled 中间状态 |
| `ToolExecutionRecordDO` | 单次执行快照：输入、校验结果、权限结果、执行事件、输出或大结果存储元信息 |

### 工具类型（ToolType）

| 类型 | 说明 | executionConfig 示例 |
|------|------|----------------------|
| `ATOMIC` | Java 内建工具，平台直接执行 | `{"className":"org.dragon.tool.runtime.tools.BashTool"}` |
| `HTTP` | 调用第三方 HTTP 接口 | `{"url":"https://...","method":"POST","headers":{},"bodyTemplate":"..."}` |
| `MCP` | 通过 MCP 协议连接外部 Server | `{"serverName":"github","mcpToolName":"search_code"}` |
| `CODE` | 用户上传的脚本（Python/Shell 等） | `{"language":"python","scriptContent":"...","entrypoint":"main"}` |
| `SKILL` | 桥接 Skill 系统 | `{"skillName":"code-review"}` |
| `AGENT` | 调用子 Agent（预留） | `{"agentDefinition":"..."}` |
| `COMPOSITE` | 多工具组合宏工具（预留） | `{"steps":[...]}` |

### 工具名命名规范

- **ATOMIC/HTTP/CODE/SKILL**：自定义全局唯一名称（如 `bash`、`file_read`）
- **MCP**：固定格式 `mcp__{serverName}__{toolName}`（如 `mcp__github__search_code`）

### 工具状态与版本状态流转

```
工具状态：  [注册] → DRAFT → ACTIVE ⇄ DISABLED
版本状态：  draft → published → deprecated
```

---

## 二、可见性规则（Visibility）

`ToolRegistry.getTools(characterId, workspaceId)` 驱动可见性计算。
新架构**已移除** `ToolVisibilityContext` 对象，直接用 `characterId + workspaceId` 参数传递上下文：

```
当前 Character 可用工具 =
  内置工具（builtin=true, status=ACTIVE）         ← 平台预置，所有 Agent 默认可用，无需绑定
  ∪ workspace 绑定工具（scopeType=WORKSPACE）      ← 该 workspace 下全员共享
  ∪ character 绑定工具（scopeType=CHARACTER）      ← 该 character 专属工具
  → by toolId 去重（LinkedHashMap：builtin > workspace > character 优先级）
  → 每个取已发布的 ACTIVE 版本
```

`ToolDO.visibility` 与 `builtin` 正交：
- `visibility`：控制"谁能在广场发现/绑定该工具"（PUBLIC/WORKSPACE/PRIVATE）
- `builtin`：控制"是否默认对所有 Agent 可用"（无需绑定）

---

## 三、生命周期管理（Management Services）

| Service | 职责 |
|---------|------|
| `ToolRegisterService` | 注册新工具（ToolDO + V1 draft）；更新（创建新 draft 版本）；保存草稿（幂等）；ATOMIC 工具不经此 Service |
| `ToolLifeCycleService` | 发布（draft→published，更新 publishedVersionId，旧版本→deprecated）；禁用；启用 |
| `ToolBindingService` | workspace/character 维度绑定/解绑，操作后发布精确 `ToolChangeEvent` 失效缓存 |
| `ToolQueryService` | 管理面查询，复用 `ToolRegistry` 的 Caffeine 缓存 |

所有状态变更操作均通过 `ApplicationEventPublisher` 发布 `ToolChangeEvent`，触发 `ToolRegistry` 缓存失效。

---

## 四、运行时对象体系（Runtime Object Model）

### 4.1 ToolDefinition — DB 到运行时的边界

```
ToolDO + ToolVersionDO
    │
    ▼  ToolRegistry.buildToolDefinition()（DB 层到 Definition 层的唯一边界）
    ▼
ToolDefinition（纯内存对象，无 DB 依赖）
    ├── 标识：toolId / toolType / version
    ├── LLM 声明：name / description / parameters(JSON) / requiredParams(JSON) / aliases
    ├── 执行配置：executionConfig（JsonNode，各类型专有）
    └── 存储信息：storageType / storageInfo（CODE/SKILL 类型关联文件）
```

设计意图：`ToolFactory` 和 `Tool` 实现类只依赖 `ToolDefinition`，与 DB 实体（`ToolDO`、`ToolVersionDO`）完全解耦，对应 `SkillRegistry.buildDefinitionFromVersion()` 的设计思路。

### 4.2 Tool 接口 — 模型可调用的工具抽象

```
Tool<I, O>
  必须实现：
    call(I input, ToolUseContext context, Consumer<ToolProgress> progress)
        → CompletableFuture<ToolResult<O>>
    mapToolResultToToolResultBlockParam(O output, String toolUseId)
        → ToolResultBlockParam

  可选方法（均有默认实现）：
    validateInput()      → ValidationResult   （默认 ok）
    checkPermissions()   → PermissionResult   （默认 allow）
    isConcurrencySafe()  → boolean            （默认 true）
    isReadOnly()         → boolean            （默认 true）
    isDestructive()      → boolean            （默认 false）
    interruptBehavior()  → CANCEL | BLOCK     （默认 BLOCK）
    getMaxResultSizeChars() → long            （默认 50_000）
    getAliases()         → List<String>       （默认空列表）
    parseInput(JsonNode) → I                  （默认 ObjectMapper 转换）
```

### 4.3 AbstractTool — 标准基类

`AbstractTool<I, O>` 实现 `Tool<I, O>`，子类只需实现：
- `doCall(I input, ToolUseContext ctx, Consumer<ToolProgress> progress)` — 核心业务逻辑
- `mapToolResultToToolResultBlockParam(O output, String toolUseId)` — 结果格式转换

基类提供：
- 取消前置检查（`context.isAborted()`，返回 `ToolResult.fail("cancelled")`）
- 进度回调包装（自动打印 trace 日志）
- 执行耗时日志（成功/失败均记录）
- 默认 `parseInput(JsonNode)`：若 `I == JsonNode` 直接返回；否则 `ObjectMapper.convertValue`

### 4.4 ToolResult — 工具执行结果

```
ToolResult<T>
  data: T                           — 原始强类型结果（供日志/存储/结构化输出）
  resultBlock: ToolResultBlockParam — 已转换好的 API 格式块（优先由 doCall 填充）
  newMessages: List<Map>            — 需注入对话的额外消息（Skill inline 模式等）
  contextModifier: Function          — 修改后续执行上下文（临时扬权等）
  success: boolean
  error: String（失败时）
```

静态工厂：`ToolResult.ok(data)` / `ToolResult.ok(data, resultBlock)` /
`ToolResult.ok(data, resultBlock, newMessages, contextModifier)` / `ToolResult.fail(error)`

### 4.5 ToolResultBlockParam — API 格式结果块

```json
{
  "type": "tool_result",
  "tool_use_id": "toolu_xxx",
  "content": "文本" | [{"type":"text","text":"..."}, {"type":"image","source":{...}}, ...],
  "is_error": false
}
```

静态工厂：`ofText(toolUseId, content)` / `ofBlocks(toolUseId, blocks)` / `ofError(toolUseId, message)`

内容块类型：`TextBlock`（type=text, text）、`ImageBlock`（type=image, source:{type, mediaType, data}）

### 4.6 ToolUseContext — 工具执行上下文

每次工具调用由框架创建，包含：

| 字段组 | 关键字段 | 说明 |
|--------|----------|------|
| 多租户隔离 | `tenantId` / `characterId` / `workspaceId` / `sessionId` | 与 ToolRegistry 缓存 key 保持一致 |
| 工具调用标识 | `toolUseId` / `agentId` / `agentType` | 当前调用 ID 及所属 Agent |
| 取消控制 | `abortController` | 调用 `abort()` 取消进行中的工具 |
| 状态管理 | `userModified` / `preserveToolUseResults` | 权限确认和子 Agent 场景 |
| 运行限制 | `fileReadingLimits` / `globLimits` | 资源配额控制 |
| 内容预算 | `contentReplacementState` | 工具结果预算管理（跨调用共享） |
| 文件缓存 | `readFileState` | 文件读取状态缓存（ConcurrentHashMap） |
| 权限追踪 | `toolDecisions` | 每次调用的权限决策记录（toolUseId → ToolDecision） |
| 查询追踪 | `queryTracking` | 链路追踪（chainId / depth） |

便捷方法：`isAborted()` / `createSubagentContext(subagentId)`（复用父上下文，新建 AbortController）

### 4.7 ToolProgress — 进度报告

```
ToolProgress { toolUseId, data: ToolProgressData, parentToolUseId }
```

内置进度数据类型：`BashProgress` / `SkillProgress` / `AgentProgress` /
`TaskOutputProgress` / `WebSearchProgress` / `HookProgress`

### 4.8 PermissionResult — 权限检查结果

```
behavior: ALLOW | DENY | ASK
matchedRule / ruleSource / message
acceptFeedback / contentBlocks（ALLOW 时附加给 LLM 的额外内容）
decisionReason（ASK 时建议规则列表）
```

### 4.9 ValidationResult — 输入校验结果

```
valid: boolean
message: String（失败时）
errorCode: Integer（可选）
```

---

## 五、运行时注册层（ToolRegistry + ToolFactory）

### ToolFactory 体系

工厂接口 `ToolFactory` 按 `ToolType` 路由，负责根据 `ToolDefinition` 构建 `Tool` 实例（**接收 ToolDefinition 而非 ToolVersionDO**，Factory 与 DB 完全解耦）：

| Factory | 工具类型 | isSingleton | 说明 |
|---------|----------|-------------|------|
| `AtomicToolFactory` | ATOMIC | `true` | 查 `toolInstanceMap`（className → Spring Bean 单例），启动时通过 `register()` 注入 |
| `HttpToolFactory` | HTTP | `true` | `new HttpTool(definition, sharedHttpClient)`，所有实例共享 HttpClient 连接池 |
| `McpToolFactory` | MCP | `true` | `new McpTool(definition, mcpHttpClient, serverConfig, toolName)` |
| `CodeToolFactory` | CODE | `true` | `new CodeTool(definition)` |
| `SkillToolFactory` | SKILL | `true` | `new SkillTool(definition, skillRegistry, skillFilter, skillExecutor, skillName)` |
| `AgentToolFactory` | AGENT | `false` | 每次调用重建实例，防止会话级状态污染 |

`isSingleton()=true` → 实例放入 `cache` 复用；`false` → 只缓存 ToolDefinition 快照（`definitionCache`），每次 `findByName` 重新 `create()`。

### ToolRegistry 双缓存机制

```
双缓存结构（Caffeine，均 TTL=5min，maxSize=1000）：
  cache
    key  = "characterId:workspaceId"
    value = List<Tool<JsonNode, ?>>（仅 isSingleton=true 的 Tool 实例）
  definitionCache
    key  = "characterId:workspaceId"
    value = List<ToolDefinition>（仅 isSingleton=false 的 Definition 快照）

getTools(characterId, workspaceId)
  │
  ├── 查 cache（命中 → 直接返回 singleton Tool 列表）
  │     cache miss ↓
  │
  ├── loadAndMergeDefinitions()          ← 三源并集 + DO→Definition 转换
  │     ① ToolStore.findAllBuiltin()
  │     ② ToolBindingStore.findByWorkspace(workspaceId)
  │     ③ ToolBindingStore.findByCharacter(characterId)
  │     → LinkedHashMap 按 toolId 去重（builtin > workspace > character）
  │     → resolvePublishedVersion(ToolDO) → ToolVersionDO
  │     → buildToolDefinition(ToolDO, ToolVersionDO) → ToolDefinition
  │
  ├── isSingleton=true  → factory.create(definition) → 写 cache
  └── isSingleton=false → definition 快照写 definitionCache

findByName(characterId, workspaceId, name)
  ├── 先从 cache 中按 name/aliases 匹配（singleton 工具，含别名支持）
  └── cache miss → 从 definitionCache 找 ToolDefinition → factory.create() 重建实例

getDefinitions(characterId, workspaceId)
  └── 直接读 definitionCache（miss 时调 loadAndMergeDefinitions）
      返回全量 ToolDefinition（singleton + non-singleton 合并）
      供 buildToolDeclarations 使用

buildToolDeclarations(characterId, workspaceId)
  ├── getDefinitions() → List<ToolDefinition>
  └── 每个 ToolDefinition → buildToolDeclarationMap()：
        {
          "name":        工具名称,
          "description": 工具描述,
          "input_schema": {
            "type":       "object",
            "properties": { 参数名: {type, description, ...} },
            "required":   [必填参数, ...]   // 有必填参数时才出现
          }
        }
  返回 List<Map<String,Object>>，直接赋值给 LLMRequest.tools
  （各 LLMCaller 实现自行读取 input_schema 字段组装厂商格式，无需 LlmAdapterRegistry）
```

### 缓存失效机制（ToolChangeEvent）

`ToolChangeEvent` 的三个工厂方法覆盖全部失效场景，`ToolRegistry.onToolChange(@EventListener)` 统一处理，**每次失效同时清 `cache` 和 `definitionCache`**：

| 工厂方法 | 触发场景 | 失效范围 |
|----------|----------|----------|
| `ofCharacter(characterId)` | character 绑定/解绑 | 精确失效 key 以 `characterId:` 开头的条目 |
| `ofWorkspace(workspaceId)` | workspace 绑定/解绑 | 精确失效 key 以 `:workspaceId` 结尾的条目 |
| `ofAll()` | 内置工具变更、MCP 重载、Tool 状态变更（builtin） | `invalidateAll()`，全量清空两个缓存 |

---

## 六、工具调用执行流程（ToolExecutionService）

`ToolExecutionService` 是 Agent 调用工具的**唯一入口**，只负责 `runToolUse`（工具声明注入已移至 `ToolRegistry.buildToolDeclarations`）。

### runToolUse 主链路

```
runToolUse(ToolCallRequest, characterId, workspaceId, ToolUseContext, progressConsumer)
  │
  ├─ 1. abort 检查（ToolUseContext.isAborted()）
  │       已中止 → 返回 [Tool execution cancelled] MessageUpdate
  │
  ├─ 2. 可见性检查
  │       ToolRegistry.findByName(characterId, workspaceId, toolName)
  │       未找到 → 返回 error MessageUpdate（工具不可见/未绑定）
  │
  ├─ 3. 创建执行记录
  │       ToolExecutionRecordService.startExecution(toolName, tenantId, sessionId, toolUseId, input)
  │       → status=RUNNING
  │
  ├─ 4. 异步执行（CachedThreadPool）→ executeWithLifecycle()
  │       │
  │       ├─ 4a. 权限检查（checkPermissions → ToolUseContext 规则）
  │       │       DENY → recordPermission + 返回 permission denied MessageUpdate
  │       │       （TODO: 完整规则匹配逻辑待实现，当前默认 allow）
  │       │
  │       ├─ 4b. tool.parseInput(rawParams) → 强类型 Input
  │       │       （AbstractTool 默认实现：JsonNode → POJO via ObjectMapper）
  │       │
  │       ├─ 4c. tool.call(input, context, progress) → CompletableFuture<ToolResult>.get()
  │       │
  │       └─ 4d. fillResultBlockIfAbsent()
  │               若 result.resultBlock == null（ATOMIC 工具 doCall 仅返回 ok(data)）：
  │               调用 tool.mapToolResultToToolResultBlockParam(data, toolUseId)
  │               异常则 fallback 到 ObjectMapper 序列化 data
  │
  ├─ 5. 大结果落存（主链路唯一落存点）
  │       resultBlock.content 为 String 且 length > 50,000 字符
  │         → ToolResultService.store(StorageContext, content) → PersistedResult
  │         → resultBlock 替换为 <persisted-output> 预览消息
  │         → storageMeta 写入执行记录
  │
  ├─ 6. 更新执行记录
  │       executionRecordService.completeWithResult(executionId, result.data, storageMeta)
  │
  └─ 7. 包装 MessageUpdate 返回给 Agent 主循环
          contentBlocks = [resultBlock, acceptFeedback?, permission.contentBlocks?]
          MessageUpdate {
            message:         {role:"user", content: contentBlocks}
            contextModifier: ContextModifierUpdate（toolUseId + modifier Function）
            newMessages:     List<Map>（Skill inline 等工具注入的额外消息）
          }
```

### MessageUpdate 结构

```
MessageUpdate
  message:         Map{role:"user", content:[ToolResultBlockParam, ...]}
  contextModifier: ContextModifierUpdate{toolUseId, modifier:Function<ToolUseContext>}
  newMessages:     List<Map>（可选，额外注入对话的消息）
```

### 与 Storage 的关系

```
ToolExecutionService（主链路唯一落存入口）
  ├─▶ ToolResultService          大结果内容 → S3 / LocalFile / Redis
  └─▶ ToolExecutionRecordService 执行元数据 → MySQL（仅写记录，不存实际内容）
```

---

## 七、LLM 适配层（Adapter Layer）

平台内部使用 `UnifiedToolDeclaration` 作为统一中间格式，通过 `LlmToolAdapter` 与各厂商格式双向转换。

### 工具声明注入（inject tools）

```
ToolRegistry.buildToolDeclarations(characterId, workspaceId)
  └─▶ List<Map<String,Object>>（input_schema 格式）
        直接赋值给 LLMRequest.tools（不再经过 LlmAdapterRegistry 热路径）
          │
          ▼
      各 LLMCaller 实现（Anthropic / OpenAI / Kimi / Deepseek / Minimax 等）
          自行读取 name、description、input_schema 字段，组装为厂商 API 格式
```

旧路径（非热路径，供独立场景使用）：
```
ToolDefinition → ToolDeclarationBuilder.build() → UnifiedToolDeclaration
  → AnthropicToolAdapter.toProviderFormat()
      {"name","description","input_schema":{"type":"object","properties":{...},"required":[...]}}
  → OpenAiToolAdapter.toProviderFormat()
      {"type":"function","function":{"name","description","parameters":{"type":"object"}}}
```
`LlmAdapterRegistry` 现仅保留供 `UnifiedToolDeclaration` 相关独立使用场景（非运行时热路径）。

### 解析 LLM 返回的 tool_call

```
Anthropic: {"type":"tool_use","id":"toulu_xxx","name":"...","input":{...}}
           → input 直接是 JSON 对象，无需二次解析

OpenAI:    {"id":"call_xxx","type":"function","function":{"name":"...","arguments":"{...}"}}
           → arguments 是 JSON 字符串，需 ObjectMapper 二次解析

Kimi / Deepseek:
           LLM 可能将 tool_calls 嵌入 content 字段（JSON 字符串），
           LLMCaller 需检测 content 中是否含 tool_calls 字段，提取后按 OpenAI 格式解析

统一输出：ToolCallRequest{toolCallId, toolName, parameters:JsonNode}
```

### 格式化 tool_result（返回给 LLM）

```
Anthropic: ToolResultBlockParam 直接序列化
           {"type":"tool_result","tool_use_id":"...","content":"...","is_error":bool}

OpenAI / Kimi / Deepseek:
           {"role":"tool","tool_call_id":"...","content":"..."}
           错误时 content 加前缀："Error: ..."
```

### 扩展新 LLM 厂商

实现 `LlmToolAdapter` 接口：`supportedProvider()` / `toProviderFormat()` / `parseToolCall()` / `toToolResultFormat()`，调用 `LlmAdapterRegistry.register()` 注册即可。

---

## 八、内置工具与 Character 专属工具

### 平台内置工具（ATOMIC 类型，builtin=true）

位于 `tool/runtime/tools/` 目录，均继承 `AbstractTool<I, O>`，Spring 启动时由 `AtomicToolFactory.register()` 注入：

| 工具类 | name | 说明 |
|--------|------|------|
| `BashTool` | `bash` | Shell 命令执行 |
| `FileReadTool` | `file_read` | 文件读取（支持行号） |
| `FileWriteTool` | `file_write` | 文件写入 |
| `FileEditTool` | `file_edit` | 文件编辑（字符串替换） |
| `GlobTool` | `glob` | 文件路径模式匹配 |
| `GrepTool` | `grep` | 内容正则搜索 |
| `WebSearchTool` | `web_search` | 网络搜索 |
| `WebFetchTool` | `web_fetch` | 网页内容获取 |
| `TodoWriteTool` | `todo_write` | 任务列表管理 |
| `AskUserQuestionTool` | `ask_user_question` | 向用户提问（requiresUserInteraction=true） |
| `BrowserTool` | `browser` | 浏览器自动化（含 CDP / Playwright / Extension Relay） |
| `EnterPlanModeTool` | `enter_plan_mode` | 进入计划模式 |
| `ExitPlanModeTool` | `exit_plan_mode` | 退出计划模式 |
| `HttpTool` | 动态 | HTTP 接口调用（TYPE=HTTP，由 HttpToolFactory 创建） |
| `McpTool` | 动态 | MCP 协议工具（TYPE=MCP，由 McpToolFactory 创建） |
| `SkillTool` | 动态 | Skill 桥接工具（TYPE=SKILL，由 SkillToolFactory 创建） |
| `AgentTool` | 动态 | 子 Agent 工具（TYPE=AGENT，每次重建实例） |

### Character 专属工具（通过 Flyway SQL 注册，builtin=false）

位于 `tool/runtime/tools/character/` 目录，同样继承 `AbstractTool`，按 Character 职能分组。
通过 Flyway 迁移脚本（`V105__insert_character_tools.sql`）批量插入 `tools` + `tool_versions` 表，
绑定关系在 `tool_bindings` 中以 `scope_type=CHARACTER` 记录（只对指定 Character 可见）。

**HR Character**（`character/hr/`）：

| 工具类 | name | 说明 |
|--------|------|------|
| `AssignDutyTool` | `assign_duty` | 为 Character 分配职责描述 |
| `EvaluateCharacterTool` | `evaluate_character` | 评估 Character 表现 |
| `HireCharacterTool` | `hire_character` | 招聘新 Character |
| `FireCharacterTool` | `fire_character` | 解雇 Character |
| `ListCandidatesTool` | `list_candidates` | 列出候选 Character 列表 |

**Observer Character**（`character/observer/`）：

| 工具类 | name | 说明 |
|--------|------|------|
| `GetRecentTasksTool` | `get_recent_tasks` | 获取最近任务列表 |
| `GetCharacterStateTool` | `get_character_state` | 获取 Character 当前状态 |
| `GetWorkspaceStateTool` | `get_workspace_state` | 获取 Workspace 整体状态 |
| `GetEvaluationRecordsTool` | `get_evaluation_records` | 获取评估记录 |
| `ExploreObservationNeedsTool` | `explore_observation_needs` | 探索观察需求 |

**PromptWriter Character**（`character/prompt_writer/`）：

| 工具类 | name | 说明 |
|--------|------|------|
| `GetWorkspaceCommonSenseTool` | `get_workspace_common_sense` | 获取 Workspace 常识信息 |

---

## 九、MCP 集成（MCP Integration）

### 应用启动加载流程

```
McpServerService.loadAllToMemory()
  │
  ├─ DB 查所有 enabled=true 的 McpServerDO
  ├─ 转换为 McpServerConfig（运行时连接配置）
  ├─ McpToolFactory.registerServerConfig(config)   ← 更新内存 serverConfigMap
  │
  └─▶ McpToolLoader.loadAll(configs)
        并发（固定线程池5）对每个 server：
          ① McpHttpClient.initialize(config)   JSON-RPC: method=initialize（握手+验证能力）
          ② McpHttpClient.listTools(config)    JSON-RPC: method=tools/list（拉取工具定义）
          ③ 每个工具注册：
              fullToolName = "mcp__" + normalize(serverName) + "__" + normalize(toolName)
              toolId = "mcp_" + normalize(serverName) + "_" + normalize(toolName)
              存 ToolDO（visibility=PUBLIC, status=ACTIVE）
              存 ToolVersionDO（version=1, status=PUBLISHED）
              发布 ToolChangeEvent.ofAll（全量缓存失效）
          单个 server 失败不影响其他 server（容错设计）
```

### 运行时 MCP 工具调用

```
McpTool.doCall(params, context, progress)
  └─▶ McpHttpClient.callTool(serverConfig, mcpToolName, arguments)
            JSON-RPC POST {url}
            method: tools/call
            params: {name: mcpToolName, arguments: {…}}
            → 提取 result.content 数组中 type=text 的 block，多个换行拼接
```

### name 不可变约束

`McpServerDO.name` **一经创建不允许修改**。原因：所有由该 Server 同步产生的工具名（`mcp__{name}__{toolName}`）均以 `name` 为前缀，修改会导致：
1. 历史工具名映射断裂，`ToolRegistry` 缓存脏数据
2. LLM 发出的 `tool_call` 无法路由到正确工具

如需更换名称，应创建新的 MCP Server 并手动迁移绑定关系。`McpServerService.update()` 接口不接受 `name` 参数强制保证此约束。

### 手动同步

```
McpServerService.syncTools(id)
  → McpToolFactory.registerServerConfig(latestConfig)   ← 刷新内存配置
  → McpToolLoader.reloadServer(config)                  ← 重新 initialize + tools/list + 注册
```

---

## 十、存储层（Result Storage）

### 存储后端自动配置

通过 `application.yml` 的 `tool-result.storage.type` 自动选择后端：

| 配置值 | 实现类 | 适用场景 |
|--------|--------|----------|
| `local-file`（默认） | `LocalFileStorageBackend` | 单机部署 |
| `s3` | `S3StorageBackend` | 生产环境，跨节点访问 |
| `redis` | `RedisStorageBackend` | 需要 TTL 自动过期 |
| `in-memory` | `InMemoryStorageBackend` | **仅限测试** |

### 大结果落存逻辑

```
ToolResultService.processResult(context, content, threshold=50,000字符)
  ├─ content 为空 → 返回 "(tool completed with no output)"
  ├─ length ≤ threshold → 直接返回原内容（不落存）
  └─ length > threshold → backend.store(context, content) → PersistedResult
                           返回 <persisted-output> 格式预览消息：
                           ┌─────────────────────────────────────────────┐
                           │ <persisted-output>                          │
                           │ Output too large (2.5MB). Full output       │
                           │ saved to: s3://bucket/path                  │
                           │                                             │
                           │ Preview (first 2KB):                        │
                           │ ... 内容前 2KB ...                          │
                           │ ...                                         │
                           │ </persisted-output>                         │
                           └─────────────────────────────────────────────┘
```

`StorageContext` 隔离维度：`tenantId` / `sessionId` / `toolUseId`。

清理接口：`cleanupSession(tenantId, sessionId)` / `cleanupTenant(tenantId)`。

---

## 十一、整体架构图

```
┌────────────────────────────────────────────────────────────────────────────┐
│                            管理面（Management）                             │
│                                                                            │
│  ToolRegisterService ──▶ ToolStore / ToolVersionStore                      │
│  ToolLifeCycleService ──▶ 状态流转（draft→active→disabled）                │
│  ToolBindingService ──▶ ToolBindingStore + ToolChangeEvent（精确缓存失效）  │
│  McpServerService ──▶ McpServerStore + McpToolLoader（工具同步）           │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│                        运行时热路径（Runtime Hot Path）                     │
│                                                                            │
│  Agent 主循环                                                               │
│    │                                                                       │
│    ├─ ToolRegistry.buildToolDeclarations(characterId, workspaceId)        │
│    │       definitionCache 命中 → List<ToolDefinition>                     │
│    │       miss → loadAndMergeDefinitions (三源并集 + DO→Definition)       │
│    │       → List<Map> input_schema 格式 → LLMRequest.tools               │
│    │       （各 LLMCaller 自行适配厂商格式，不经 LlmAdapterRegistry）       │
│    │                                                                       │
│    └─ ToolExecutionService.runToolUse(req, characterId, workspaceId, ctx) │
│         │                                                                  │
│         ├─ ToolRegistry.findByName()                                       │
│         │    cache 命中 → singleton Tool 实例                               │
│         │    miss → definitionCache → ToolFactory.create(ToolDefinition)  │
│         │              AtomicFactory / HttpFactory / McpFactory /          │
│         │              CodeFactory / SkillFactory / AgentFactory           │
│         │                                                                  │
│         ├─ ToolExecutionRecordService（写执行记录 RUNNING）                 │
│         ├─ tool.parseInput() → tool.call() → ToolResult                   │
│         ├─ ToolResultService（大结果落存 S3/LocalFile/Redis）               │
│         └─ ToolExecutionRecordService（更新执行记录 SUCCESS/FAILED）        │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│                              MCP 集成                                      │
│                                                                            │
│  启动时：McpServerService → McpToolLoader → McpHttpClient                  │
│              JSON-RPC: initialize → tools/list → 注册到 ToolStore          │
│  运行时：McpTool.doCall → McpHttpClient.callTool                           │
│              JSON-RPC: tools/call → 返回文本结果                            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 十二、关键设计决策

1. **ToolDefinition 层隔离 DB**：Factory 和 Tool 实现类只依赖 `ToolDefinition`，不感知 `ToolDO`/`ToolVersionDO`，对齐 SkillDefinition 设计模式，DB 结构变更不影响运行时代码。

2. **双缓存设计**：`cache`（singleton Tool 实例）+ `definitionCache`（non-singleton ToolDefinition 快照）分离，缓存失效时同步清除两个缓存，不存在遗漏场景。

3. **buildToolDeclarations 内化到 ToolRegistry**：工具声明组装不再经过 `LlmAdapterRegistry`，热路径调用链缩短，各 `LLMCaller` 自行读取 `input_schema` 字段完成厂商格式转换。

4. **两条路径分离**：管理面（低频写）和运行时热路径（高频读）完全解耦，运行时走 Caffeine 缓存（TTL=5min），不直接查库。

5. **ToolFactory 是扩展点**：新增工具类型只需实现 `ToolFactory` 和对应 `Tool` 子类注册到 `ToolRegistry`，不需改变执行链路。

6. **大结果落存唯一入口**：`ToolExecutionService.processResult()` 是大结果落存的**唯一入口**，`ToolExecutionRecordService` 只写元数据，职责清晰。

7. **MCP name 不可变**：架构级约束，`McpServerService` 接口层强制保证，是数据一致性的核心保障。

8. **缓存失效精确化**：绑定变更发精确事件（characterId/workspaceId），内置工具或 MCP 变更才触发全量失效（`ofAll()`），避免不必要的缓存穿透。

