# Skill 模块架构设计文档

> 版本：3.0 | 更新日期：2026-04-05

---

## 一、模块定位

Skill 是一个带有 YAML frontmatter 的 Markdown Prompt 文件（SKILL.md），供 Agent 在执行任务时调用。

模型通过 `tool_use` 机制调用 Skill，将 Skill 的 prompt 内容注入当前对话，或创建独立子 Agent 执行。

---

## 二、核心设计点

### 设计点 1：多来源 Skill 聚合

**问题**：Skill 来自多个渠道，Agent 运行时需要统一获取可用的 Skill 列表。

**方案**：

运行时可用的 Skill = builtin Skill ∪ Character 自有 Skill ∪ Workspace 公共 Skill ∪ Character@Workspace 专属 Skill

| 来源 | 说明 |
|------|------|
| builtin | DB 中 category='builtin' 的全量 active Skill，所有 Character 可见，不走绑定关系 |
| character | bindingType='character'，该 Character 独有 |
| workspace | bindingType='workspace'，Workspace 内所有 Character 共享 |
| character_workspace | bindingType='character_workspace'，特定 Character 在特定 Workspace 下专属 |

**优先级**：用户绑定的 Skill 优先级高于同名的 builtin Skill（同名时覆盖）。

**缓存**：使用 Caffeine L1 缓存，key = `"characterId:workspaceId"`，TTL = 5 分钟。

---

### 设计点 2：目录与内容分离

**问题**：一个 Agent 可能配置数十个 Skill，如果每次对话都把每个 Skill 的完整 prompt 注入 system prompt，会消耗大量 token。

**方案**：

- **目录（Directory）**：只注入 Skill 的 name + whenToUse + argumentHint，让模型知道有哪些 Skill 可用及其使用场景
- **内容（Content）**：在 Skill 执行时才按需加载完整 prompt 正文

**效果**：100 个 Skill 的目录约 2000 token，而展开全部内容可能需要数万 token。

---

### 设计点 3：版本管理

**问题**：Skill 需要支持版本历史，每次更新不能覆盖旧版本内容。

**方案**：

- 每次注册/更新 INSERT 一条新记录，`skillId`（UUID）不变，`version` +1
- `id` 为物理自增主键，`skillId + version` 联合唯一
- 查询"当前版本"：按 `skillId` 取 `version` 最大的记录

**版本策略**：

| 策略 | 说明 |
|------|------|
| latest | 运行时动态取该 skillId 最新 active 版本 |
| fixed | 固定到某个具体版本号，不随更新变化 |

---

### 设计点 4：执行模式

**inline 模式**：

- Skill 的 prompt 作为一条 meta 用户消息注入当前对话
- 模型接收到消息后直接在当前 context 中处理
- 无需创建新 Agent

**fork 模式**：

- 打包执行参数（prompt、allowedTools、model、effort）委托给框架层
- 框架层负责创建独立的 sub-AgentTask
- 父 Agent 等待子任务完成

---

### 设计点 5：工作区物化

**问题**：Skill 可能包含附属文件（如 JSON schema、脚本等），需要物化到本地目录才能正确执行。

**方案**：两层设计

| 层级 | 说明 |
|------|------|
| 模板层 | 存放只读的 Skill 包文件，Caffeine 缓存，TTL=30 分钟，进程内共享 |
| 执行层 | 每次执行从模板复制到独立目录（UUID 路径），执行结束后立即删除 |

**判断逻辑**：只有 SKILL.md 一个文件时直接读文本，无需物化目录；有附属文件（>1个）才需要物化。

---

### 设计点 6：ContextPatch 声明式上下文变更

**问题**：Skill 执行时可能需要临时覆盖 Agent 的工具权限、模型、effort。

**方案**：

- Skill 返回 `ContextPatch` 对象，声明需要变更的字段
- 框架层负责"保存当前上下文 → 应用变更 → 执行 → 恢复"
- SkillTool 本身不直接修改上下文，保持与框架的解耦

**变更字段**：

| 字段 | 说明 |
|------|------|
| additionalAllowedTools | 额外允许的工具列表（追加） |
| modelOverride | 临时覆盖使用的模型 |
| effortOverride | 临时覆盖 effort 级别 |
| skillWorkDir | 工作目录绝对路径（物化时传入） |

---

## 三、数据模型

### 3.1 三层对象映射

```
Entity (Ebean)  ←→  DO (Domain)  ←→  Definition (Runtime)
   ↓                  ↓                ↓
  skills表        领域对象          运行时定义
```

- **Entity**：映射数据库表，负责 DB 字段与 Java 类型的转换
- **DO**：Service/Store 层流通的领域对象
- **Definition**：运行时使用的内存对象，由 SkillRegistry 组装

### 3.2 三个核心实体

**SkillEntity** → `skills` 表

- 版本设计：每次更新 INSERT 新记录，`skillId` 不变，`version` +1
- 关键字段：`skillId`、`name`、`content`、`storageInfo`、`executionContext`、`allowedTools`

**SkillBindingEntity** → `skill_bindings` 表

- 三种绑定关系通过 `bindingType` 区分：character / workspace / character_workspace
- `characterId` 和 `workspaceId` 统一为 String 类型

**SkillUsageEntity** → `skill_usage_logs` 表

- 异步记录 Skill 调用情况（成功/失败、耗时、参数等）

---

## 四、运行时执行链路

### 4.1 完整调用流程

```
模型 tool_use(skill="git-commit", args="fix bug")
    ↓
SkillTool.execute(ToolContext)
    ├── ToolContext 携带: characterId, workspaceId, sessionKey
    │
    ├── SkillRegistry.getSkills(characterId, workspaceId)
    │       └── 从 Caffeine 缓存获取，缓存未命中则查 DB 并聚合
    │
    ├── SkillFilter.filter(skills)
    │       └── 过滤 disableModelInvocation=true 的 Skill
    │
    ├── 查找目标 Skill（名称匹配，支持别名）
    │
    ├── SkillPermissionChecker.check()
    │       └── 权限检查（deny/allow/白名单/ask）
    │
    └── SkillExecutor.execute(skill, args, sessionKey, agentContext)
            ├── 判断是否需要物化（storageInfo.files.size > 1）
            │
            ├── [需要物化]
            │       └── SkillWorkspaceManager.prepareExecDir()
            │               → 模板层缓存未命中则下载
            │               → 从模板复制到执行层（UUID 路径）
            │
            ├── 加载 Skill 内容（直接读取或从物化目录读取）
            ├── injectArgs($ARGUMENTS 占位符替换)
            ├── buildContextPatch()
            │
            ├── [inline 模式]
            │       └── 构建 meta 用户消息，注入当前对话
            │
            └── [fork 模式]
                    └── 打包参数，委托框架层创建子 AgentTask
    ↓
返回 SkillToolData { executionMode, contextPatch, newMessages, persistContent }
    ↓
框架层消费 SkillToolData
    ├── INLINE: 追加 newMessages 到对话
    ├── FORK: 创建子 AgentTask
    └── 应用 contextPatch（临时覆盖工具/模型/effort）
```

### 4.2 ToolContext 框架注入

`ToolContext` 由框架层在调用 Tool 时注入：

| 字段 | 说明 |
|------|------|
| characterId | 当前执行的 Character ID |
| workspaceId | 当前所在的 Workspace ID |
| sessionKey | 当前会话 Key |
| cwd | 当前工作目录 |
| config | 项目配置 |
| parameters | Tool 调用参数（JSON） |

---

## 五、关键组件职责

| 组件 | 所在包 | 核心职责 |
|------|--------|----------|
| SkillTool | tools.builtin | Agent 可调用 Tool 入口，实现 AgentTool 接口，串联全链路 |
| SkillRegistry | skill.runtime | 多来源聚合 + Caffeine L1 缓存，按 characterId/workspaceId 组织 |
| SkillFilter | skill.runtime | 过滤 disableModelInvocation=true 的 Skill |
| SkillPermissionChecker | skill.runtime | 四步权限检查（deny → allow → 白名单 → ask） |
| SkillExecutor | skill.runtime | inline/fork 执行分支、内容加载、ContextPatch 构建 |
| SkillWorkspaceManager | skill.runtime | 两层工作区管理（模板只读缓存 + 执行层 UUID 隔离） |
| SkillDirectoryBuilder | skill.runtime | 静态工具类，构建目录 prompt 和 persist 摘要 |
| SkillToolData | skill.runtime | 执行结果数据结构，供框架层消费 |

---

## 六、包结构

```
org.dragon.skill/
├── config/
│   └── SkillPermissionConfig.java      # 权限规则配置
├── domain/
│   ├── SkillDO.java                    # 领域对象
│   ├── SkillBindingDO.java             # 绑定领域对象
│   ├── SkillUsageDO.java               # 使用记录领域对象
│   └── StorageInfoVO.java              # 存储信息 JSON 映射
├── enums/
│   ├── ExecutionContext.java           # inline / fork
│   ├── PersistMode.java                # full / summary
│   ├── BindingType.java                # character / workspace / character_workspace
│   ├── VersionType.java                # latest / fixed
│   ├── SkillStatus.java                # draft / active / disabled / deleted
│   ├── SkillCategory.java
│   ├── SkillVisibility.java
│   ├── SkillEffort.java
│   ├── CreatorType.java
│   └── StorageType.java
├── exception/
│   ├── SkillException.java              # 基类（500）
│   ├── SkillNotFoundException.java     # 404
│   ├── SkillStatusException.java       # 409
│   └── SkillValidationException.java   # 400
├── runtime/                            # 运行时执行链路
│   ├── SkillRegistry.java              # 多来源聚合 + 缓存
│   ├── SkillFilter.java                # 可见性过滤
│   ├── SkillPermissionChecker.java     # 权限检查
│   ├── SkillExecutor.java              # 执行器
│   ├── SkillWorkspaceManager.java      # 工作区管理
│   ├── SkillDirectoryBuilder.java      # 静态工具类
│   ├── SkillDefinition.java            # 运行时定义
│   ├── SkillToolData.java              # 执行结果
│   ├── ContextPatch.java               # 上下文变更声明
│   ├── AgentContext.java               # Agent 执行上下文
│   ├── SkillChangeEvent.java           # 缓存失效事件
│   ├── SkillPermissionEvent.java       # 权限 ASK 事件
│   └── SkillPermissionResult.java      # 权限结果
├── service/
│   ├── SkillRegisterService.java       # 注册/更新流程
│   ├── SkillLifecycleService.java      # 状态流转
│   ├── SkillQueryService.java          # 分页查询
│   ├── SkillBindingService.java        # 绑定管理
│   ├── SkillStorageService.java        # 存储抽象接口
│   ├── LocalSkillStorageService.java   # 本地存储实现
│   ├── S3SkillStorageService.java      # S3 存储实现
│   └── SkillUsageService.java          # 使用统计
├── store/
│   ├── SkillStore.java                 # 接口
│   ├── MySqlSkillStore.java            # Ebean 实现
│   ├── SkillBindingStore.java          # 接口
│   ├── MySqlSkillBindingStore.java     # Ebean 实现
│   ├── SkillUsageStore.java            # 接口
│   └── MySqlSkillUsageStore.java      # Ebean 实现
├── util/
│   ├── SkillValidator.java             # 静态：ZIP 安全校验
│   └── SkillContentParser.java         # 静态：SKILL.md 解析
└── dto/
    ├── SkillRegisterRequest.java
    ├── SkillRegisterResult.java
    ├── SkillBindingRequest.java
    └── ...

org.dragon.tools.builtin/
└── SkillTool.java                     # AgentTool 接口实现，入口
```

---

## 七、状态机

```
  注册/更新
     │
     ▼
  [draft]
     │  发布(publish)
     ▼
  [active] ◄─────────── 重新发布(republish)
     │                            ▲
     │  下架(disable)             │
     ▼                            │
  [disabled] ─────────────────────┘
     │
     │  删除(delete)
     ▼
  [deleted]  ← 终态，不可逆
```

- `publish` / `disable` / `republish`：只作用于最新版本
- `delete`：将该 skillId 所有版本均标记为 deleted

---

## 八、权限检查

**四步检查顺序**：

1. **deny 规则**：黑名单，命中则立即拒绝
2. **allow 规则**：白名单，命中则立即放行
3. **安全属性白名单**：Skill 只含白名单属性时自动放行
4. **ASK**：以上均未命中，根据策略处置（auto-deny 或 event）

**规则匹配**：

| 格式 | 匹配方式 |
|------|----------|
| `deploy-check` | 精确匹配（忽略前导 `/`） |
| `review:*` | 前缀通配，匹配 `review` 开头的名称 |

**安全属性白名单**：

`skillId`、`name`、`displayName`、`description`、`version`、`whenToUse`、`argumentHint`、`aliases`、`disableModelInvocation`、`userInvocable`、`model`、`effort`、`category`、`persist`、`persistMode`、`storageInfo`、`content`

**非安全属性（需权限）**：

- `allowedTools`（非空）：影响子 Agent 可用工具集
- `executionContext=fork`：创建子 Agent
