# Java Memory System Architecture

## 1. 文档目标

本文基于 `analysis/04j-memory-system-architecture.md` 对 Claude Code memory 体系的抽象，总结并设计一套 **Java 服务端版本** 的完整记忆框架。

这套 Java 方案保留原始架构中的几个关键思想：

- `MEMORY.md` 是索引，不直接承载长正文
- 具体 memory 独立为 topic 文件
- 记忆按作用域拆分，而不是塞进一个总库
- session memory 与长期 memory 分离
- 上层 agent / team / workflow 通过统一接口访问 memory

同时根据你的目标，对作用域做了重组：

- `character`：类似单独 agent 的长期记忆空间
- `workspace`：组织多个 character 协同工作的共享记忆空间
- `session memory`：区分当前会话短期记忆与长期记忆

> 为了避免术语歧义，文中逻辑概念统一使用 `character`。如果你最终落地目录名需要使用 `<charactor>`，只需要在路径常量层改名即可，设计本身不受影响。

---

## 2. 总体结论

这套 Java 版本不建议做成单一数据库表的“黑盒知识库”，而建议做成 **文件索引 + 结构化元数据 + 服务接口封装** 的双层体系：

- **文件层**：仍保留 `MEMORY.md + mem/*.md` 的可见结构，方便调试、导入导出、离线排查
- **领域层**：通过 Java 类、接口和服务编排 memory 的读写、索引、提取、召回、归档
- **会话层**：把 session memory 独立出来，不与 character / workspace 长期记忆混写
- **编排层**：面向上层 agent runtime、workflow、协作引擎提供统一 API

整体上，这会是一套 **多层作用域、统一接口、可扩展存储后端、支持 session 分离** 的 Java memory framework。

整体结构可以抽象为：

```text
                    ┌────────────────────────────────────┐
                    │         Memory Orchestration       │
                    │ Character / Workspace / Session    │
                    └────────────────┬───────────────────┘
                                     │
          ┌──────────────────────────┼──────────────────────────┐
          │                          │                          │
          ▼                          ▼                          ▼
┌────────────────────┐   ┌────────────────────┐   ┌────────────────────┐
│ Character Memory   │   │ Workspace Memory   │   │ Session Memory     │
│ 单 agent 长期记忆   │   │ 多 agent 共享记忆   │   │ 当前会话摘要/状态   │
└─────────┬──────────┘   └─────────┬──────────┘   └─────────┬──────────┘
          │                        │                        │
          ▼                        ▼                        ▼
┌────────────────────┐   ┌────────────────────┐   ┌────────────────────┐
│ <character>/       │   │ <workspace>/       │   │ sessions/<sid>/    │
│ MEMORY.md          │   │ MEMORY.md          │   │ session-memory.md  │
│ mem/*.md           │   │ mem/*.md           │   │ checkpoints/*.json │
└─────────┬──────────┘   └─────────┬──────────┘   └─────────┬──────────┘
          │                        │                        │
          └──────────────┬─────────┴──────────────┬─────────┘
                         ▼                        ▼
               ┌────────────────────┐   ┌────────────────────┐
               │ Index / Recall     │   │ Extract / Summarize│
               │ 索引、检索、召回     │   │ 提取、摘要、固化     │
               └────────────────────┘   └────────────────────┘
```

---

## 3. 目录与存储规范

## 3.1 目录约定

按你的要求，三层 memory 建议采用如下物理结构：

```text
memory-root/
├── workspaces/
│   └── {workspaceId}/
│       ├── MEMORY.md
│       ├── mem/
│       │   ├── architecture_decisions.md
│       │   ├── release_constraints.md
│       │   └── external_refs.md
│       ├── metadata.json
│       └── archive/
│
├── characters/
│   └── {characterId}/
│       ├── MEMORY.md
│       ├── mem/
│       │   ├── user_style.md
│       │   ├── preferred_planning.md
│       │   └── domain_habits.md
│       ├── metadata.json
│       └── archive/
│
└── sessions/
    └── {sessionId}/
        ├── session-memory.md
        ├── context-window.json
        ├── events.jsonl
        ├── extracted/
        │   ├── candidate-001.md
        │   └── candidate-002.md
        └── checkpoints/
```

其中：

- `characters/{characterId}/MEMORY.md`：character 长期记忆索引
- `characters/{characterId}/mem/*.md`：character 长期 topic memory
- `workspaces/{workspaceId}/MEMORY.md`：workspace 长期共享索引
- `workspaces/{workspaceId}/mem/*.md`：workspace 共享 topic memory
- `sessions/{sessionId}/session-memory.md`：当前会话摘要

## 3.2 设计原则

### 原则一：索引与正文分离

- `MEMORY.md` 只存索引条目
- 每个具体 memory 存在单独 `.md` 文件中
- 索引条目引用 topic 文件，而不承载正文

### 原则二：长期记忆与短期会话分离

- `character` / `workspace` 保存 durable memory
- `session` 保存会话状态、压缩摘要、中间抽取结果
- session 不能直接等价于长期记忆，必须经过提取与判定才能固化

### 原则三：作用域先决

任何一条 memory 在写入前，都要先明确作用域：

- 只对单个 character 有意义 → `character`
- 对多个 character 协作有意义 → `workspace`
- 只对当前会话临时有意义 → `session`

### 原则四：文件层与元数据层并存

建议 Java 服务端内部同时维护：

- markdown 文件
- 结构化元数据（数据库或内存索引）

这样既保留可见性，又保证检索和治理能力。

---

## 4. 总体分层设计

为了让这套 Java 框架后续容易扩展，建议采用如下包结构。

```text
com.example.memory/
├── api/                 # 对外服务接口层
├── application/         # 应用编排层
├── domain/              # 领域模型层
├── extractor/           # 提取与固化层
├── recall/              # 检索与召回层
├── repository/          # 存储抽象层
├── session/             # session memory 专用逻辑
├── storage/             # 文件系统/数据库/对象存储实现
├── index/               # MEMORY.md 索引维护
├── parser/              # markdown/frontmatter/parser
├── policy/              # 记忆分类、去重、路由策略
├── model/               # DTO / request / response
└── config/              # 配置与路径规则
```

从架构角度看，可以分成 7 层：

1. **Config 层**：定义根目录、作用域目录规则、阈值与开关
2. **Domain 层**：定义 MemoryEntry、MemoryScope、SessionSnapshot 等核心对象
3. **Repository 层**：抽象 Character / Workspace / Session 的读写接口
4. **Index 层**：维护 `MEMORY.md` 的索引读写和一致性
5. **Recall/Extractor 层**：负责召回、提取、摘要、固化
6. **Application 层**：负责编排不同 repository 与策略
7. **API 层**：面向上层 agent runtime、workflow、controller 暴露稳定接口

---

## 5. 整体类和接口结构体系设计

下面按“总-分”的方式，先给出完整类图级别的设计，再分别细化。

## 5.1 核心领域对象

### 1）`MemoryScope`

```java
public enum MemoryScope {
    CHARACTER,
    WORKSPACE,
    SESSION
}
```

职责：

- 定义 memory 所属作用域
- 用于路由、鉴权、目录选择和召回过滤

### 2）`MemoryType`

```java
public enum MemoryType {
    USER,
    FEEDBACK,
    PROJECT,
    REFERENCE,
    SESSION_SUMMARY,
    WORKSPACE_DECISION,
    CHARACTER_PROFILE
}
```

职责：

- 定义 memory 的语义类型
- 支持后续召回策略和提取策略定制

说明：

- 前四类继承 Claude memory 的原始语义
- 后三类是 Java 方案里为 session / workspace / character 做的扩展型标签
- 实际落地时也可以拆成“基础类型 + 扩展标签”的双字段模型

### 3）`MemoryId`

```java
public final class MemoryId {
    private final String value;
}
```

职责：

- 统一标识一条 memory
- 用于 repository、索引、引用关系和审计

### 4）`MemoryEntry`

```java
public class MemoryEntry {
    private MemoryId id;
    private String title;
    private String description;
    private MemoryType type;
    private MemoryScope scope;
    private String ownerId;      // characterId / workspaceId / sessionId
    private String fileName;
    private String filePath;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, String> tags;
}
```

职责：

- 表示一条完整的 memory
- 同时承载索引所需元数据与正文内容
- 与 markdown 文件一一映射

### 5）`MemoryIndexItem`

```java
public class MemoryIndexItem {
    private MemoryId memoryId;
    private String title;
    private String relativePath;
    private String summaryLine;
    private MemoryType type;
    private Instant updatedAt;
}
```

职责：

- 表示 `MEMORY.md` 中的一条索引记录
- 不承载全文，只承载导航信息

### 6）`SessionSnapshot`

```java
public class SessionSnapshot {
    private String sessionId;
    private String characterId;
    private String workspaceId;
    private String summary;
    private String currentGoal;
    private List<String> recentDecisions;
    private List<String> unresolvedQuestions;
    private Instant updatedAt;
}
```

职责：

- 表示当前会话抽象摘要
- 供 compact、resume、manual summary 使用

### 7）`MemoryQuery`

```java
public class MemoryQuery {
    private String text;
    private String workspaceId;
    private String characterId;
    private String sessionId;
    private Set<MemoryScope> scopes;
    private Set<MemoryType> types;
    private int limit;
}
```

职责：

- 抽象召回请求
- 允许上层指定查询词、作用域、类型、返回上限

### 8）`MemorySearchResult`

```java
public class MemorySearchResult {
    private MemoryEntry memory;
    private double score;
    private String reason;
}
```

职责：

- 表达召回结果与打分原因
- 便于上层 agent runtime 决定是否注入 prompt

---

## 5.2 核心接口体系

### 1）`MemoryStore`

```java
public interface MemoryStore {
    MemoryEntry save(MemoryEntry entry);
    Optional<MemoryEntry> findById(MemoryId id);
    List<MemoryEntry> listByOwner(MemoryScope scope, String ownerId);
    void delete(MemoryId id);
}
```

职责：

- 抽象最基础的 memory 持久化操作
- 为不同作用域 store 提供统一能力

### 2）`MemoryIndexStore`

```java
public interface MemoryIndexStore {
    List<MemoryIndexItem> loadIndex(MemoryScope scope, String ownerId);
    void rebuildIndex(MemoryScope scope, String ownerId);
    void appendIndexItem(MemoryScope scope, String ownerId, MemoryIndexItem item);
    void removeIndexItem(MemoryScope scope, String ownerId, MemoryId memoryId);
}
```

职责：

- 维护 `MEMORY.md`
- 支持增量追加与全量重建

### 3）`MemoryRepository`

```java
public interface MemoryRepository {
    MemoryEntry create(MemoryEntry entry);
    MemoryEntry update(MemoryEntry entry);
    Optional<MemoryEntry> get(MemoryScope scope, String ownerId, MemoryId memoryId);
    List<MemoryEntry> list(MemoryScope scope, String ownerId);
    void archive(MemoryScope scope, String ownerId, MemoryId memoryId);
}
```

职责：

- 在 `MemoryStore` 之上提供更业务化的 CRUD
- 同时联动索引维护、archive、校验等逻辑

### 4）`MemoryRecallService`

```java
public interface MemoryRecallService {
    List<MemorySearchResult> recall(MemoryQuery query);
}
```

职责：

- 统一召回入口
- 支持 keyword recall / vector recall / hybrid recall

### 5）`MemoryExtractionService`

```java
public interface MemoryExtractionService {
    List<MemoryEntry> extractCandidates(SessionSnapshot snapshot, List<String> conversationEvents);
    List<MemoryEntry> persistApprovedCandidates(List<MemoryEntry> candidates);
}
```

职责：

- 从 session 中抽取可长期保存的 memory candidate
- 决定哪些候选需要固化为 character 或 workspace memory

### 6）`SessionMemoryService`

```java
public interface SessionMemoryService {
    SessionSnapshot load(String sessionId);
    SessionSnapshot update(String sessionId, List<String> dialogueEvents);
    void checkpoint(String sessionId, SessionSnapshot snapshot);
    void clear(String sessionId);
}
```

职责：

- 管理会话摘要与 checkpoint
- 不直接负责长期 memory 的归档

### 7）`MemoryRoutingPolicy`

```java
public interface MemoryRoutingPolicy {
    MemoryScope route(MemoryEntry candidate, SessionSnapshot sessionSnapshot);
}
```

职责：

- 决定一条候选记忆最终应落到 character、workspace 还是保留在 session

### 8）`MemoryDedupPolicy`

```java
public interface MemoryDedupPolicy {
    Optional<MemoryEntry> findDuplicate(MemoryEntry candidate, List<MemoryEntry> existing);
}
```

职责：

- 处理 memory 去重、合并、更新

### 9）`MemoryFrameworkFacade`

```java
public interface MemoryFrameworkFacade {
    List<MemorySearchResult> recallForAgent(AgentMemoryContext context, String query);
    SessionSnapshot updateSession(String sessionId, List<String> events);
    List<MemoryEntry> flushSessionToLongTerm(String sessionId);
    MemoryEntry saveCharacterMemory(String characterId, MemoryEntry entry);
    MemoryEntry saveWorkspaceMemory(String workspaceId, MemoryEntry entry);
}
```

职责：

- 提供上层统一入口
- 避免业务侧直接操作底层多个 service

---

## 6. 分层详细设计：每层类和接口应该包含什么

下面按层展开，给出建议的类、接口及其方法分工。

## 6.1 Config 层

### `MemorySystemProperties`

```java
public class MemorySystemProperties {
    private String rootDir;
    private int maxIndexLines;
    private int maxIndexBytes;
    private int sessionUpdateTokenThreshold;
    private int recallLimit;
    private boolean enableHybridRecall;
}
```

职责：

- 管理全局配置
- 替代硬编码常量

### `MemoryPathResolver`

```java
public interface MemoryPathResolver {
    Path resolveCharacterRoot(String characterId);
    Path resolveCharacterIndex(String characterId);
    Path resolveCharacterMemDir(String characterId);

    Path resolveWorkspaceRoot(String workspaceId);
    Path resolveWorkspaceIndex(String workspaceId);
    Path resolveWorkspaceMemDir(String workspaceId);

    Path resolveSessionRoot(String sessionId);
    Path resolveSessionMemoryFile(String sessionId);
}
```

职责：

- 统一目录和文件路径计算
- 保证路径规则集中管理

### `DefaultMemoryPathResolver`

职责：

- 根据 `MemorySystemProperties.rootDir` 输出标准路径
- 如果未来目录从本地磁盘切到对象存储映射层，也只需替换它

---

## 6.2 Parser / Index 层

### `MemoryMarkdownParser`

```java
public interface MemoryMarkdownParser {
    MemoryEntry parse(Path memoryFile);
    String render(MemoryEntry entry);
}
```

职责：

- markdown <-> MemoryEntry 双向转换
- 建议支持 frontmatter

建议 frontmatter 形态：

```markdown
---
id: mem-123
name: user_style
summary: User prefers concise responses
scope: CHARACTER
type: FEEDBACK
updated_at: 2026-04-01T12:30:00Z
---

User prefers concise responses.

Why: ...
How to apply: ...
```

### `MemoryIndexParser`

```java
public interface MemoryIndexParser {
    List<MemoryIndexItem> parseIndex(Path indexFile);
    String renderIndex(List<MemoryIndexItem> items);
}
```

职责：

- 解析和生成 `MEMORY.md`

建议索引格式：

```markdown
# MEMORY

- [User style](mem/user_style.md) - concise response preference
- [Planning style](mem/planning_style.md) - prefers milestone-first plans
```

### `MemoryIndexService`

```java
public class MemoryIndexService {
    List<MemoryIndexItem> load(MemoryScope scope, String ownerId);
    void rebuild(MemoryScope scope, String ownerId);
    void add(MemoryScope scope, String ownerId, MemoryEntry entry);
    void remove(MemoryScope scope, String ownerId, MemoryId memoryId);
}
```

职责：

- 维护索引一致性
- 保证正文文件和 `MEMORY.md` 对齐

---

## 6.3 Repository 层

这里建议按作用域分 repository，而不是所有 scope 混成一个类。

### 1）`CharacterMemoryRepository`

```java
public interface CharacterMemoryRepository {
    MemoryEntry create(String characterId, MemoryEntry entry);
    MemoryEntry update(String characterId, MemoryEntry entry);
    Optional<MemoryEntry> get(String characterId, MemoryId memoryId);
    List<MemoryEntry> list(String characterId);
    void delete(String characterId, MemoryId memoryId);
    void rebuildIndex(String characterId);
}
```

职责：

- 操作 `<character>/MEMORY.md`
- 操作 `<character>/mem/*.md`
- 管 character 私有长期记忆

### 2）`WorkspaceMemoryRepository`

```java
public interface WorkspaceMemoryRepository {
    MemoryEntry create(String workspaceId, MemoryEntry entry);
    MemoryEntry update(String workspaceId, MemoryEntry entry);
    Optional<MemoryEntry> get(String workspaceId, MemoryId memoryId);
    List<MemoryEntry> list(String workspaceId);
    void delete(String workspaceId, MemoryId memoryId);
    void rebuildIndex(String workspaceId);
}
```

职责：

- 操作 `<workspace>/MEMORY.md`
- 操作 `<workspace>/mem/*.md`
- 管共享长期记忆

### 3）`SessionMemoryRepository`

```java
public interface SessionMemoryRepository {
    SessionSnapshot load(String sessionId);
    SessionSnapshot save(String sessionId, SessionSnapshot snapshot);
    void appendEvent(String sessionId, String event);
    List<String> listEvents(String sessionId);
    void checkpoint(String sessionId, SessionSnapshot snapshot);
    void clear(String sessionId);
}
```

职责：

- 管当前会话摘要文件和事件流
- 不管理 `MEMORY.md`

### 4）文件实现类

建议配套实现：

- `FileCharacterMemoryRepository`
- `FileWorkspaceMemoryRepository`
- `FileSessionMemoryRepository`

职责：

- 使用本地文件系统实现
- 写入 markdown、重建 index、维护 archive 目录

---

## 6.4 Recall 层

### `CharacterMemoryRecallService`

```java
public interface CharacterMemoryRecallService {
    List<MemorySearchResult> recall(String characterId, String query, int limit);
}
```

职责：

- 检索某个 character 的长期记忆

### `WorkspaceMemoryRecallService`

```java
public interface WorkspaceMemoryRecallService {
    List<MemorySearchResult> recall(String workspaceId, String query, int limit);
}
```

职责：

- 检索共享工作空间记忆

### `SessionMemoryRecallService`

```java
public interface SessionMemoryRecallService {
    Optional<SessionSnapshot> recall(String sessionId);
}
```

职责：

- 返回当前 session 摘要，而不是 topic memory 列表

### `CompositeMemoryRecallService`

```java
public class CompositeMemoryRecallService implements MemoryRecallService {
    List<MemorySearchResult> recall(MemoryQuery query);
}
```

职责：

- 聚合多个 scope 的 recall 结果
- 按优先级排序，例如：
    - session > character > workspace
    - 或 character > workspace，再加当前 session 摘要作为 system context

### `MemoryRanker`

```java
public interface MemoryRanker {
    List<MemorySearchResult> rank(String query, List<MemoryEntry> candidates, int limit);
}
```

职责：

- 对 recall 候选做排序
- 可支持 BM25、embedding、规则打分等方式

---

## 6.5 Session 层

Session memory 这一层建议单独设计，不要复用 character/workspace 的 topic memory 模式。

### `SessionMemoryService`

建议包含如下方法：

```java
public interface SessionMemoryService {
    SessionSnapshot initialize(String sessionId, String workspaceId, String characterId);
    SessionSnapshot update(String sessionId, List<String> dialogueEvents);
    SessionSnapshot summarize(String sessionId);
    void checkpoint(String sessionId);
    void close(String sessionId);
}
```

职责：

- 初始化 session
- 随对话逐步更新摘要
- 在阈值触发时生成新摘要
- 为 resume 提供 checkpoint

### `SessionCompressionService`

```java
public interface SessionCompressionService {
    String compress(SessionSnapshot snapshot, List<String> recentEvents);
}
```

职责：

- 用于长对话压缩
- 与长期记忆固化职责分离

### `SessionToLongTermBridge`

```java
public interface SessionToLongTermBridge {
    List<MemoryEntry> extractLongTermCandidates(String sessionId);
    List<MemoryEntry> promote(String sessionId);
}
```

职责：

- 把 session 摘要/事件抽取为长期记忆候选
- 再交由 routing policy 决定写入 character 或 workspace

这层是整个设计里最关键的“当前会话”和“长期 agent 记忆”隔离桥梁。

---

## 6.6 Extractor / Policy 层

### `MemoryExtractionService`

应当提供两个阶段：

```java
public interface MemoryExtractionService {
    List<MemoryEntry> extractCandidates(SessionSnapshot snapshot, List<String> events);
    List<MemoryEntry> refineCandidates(List<MemoryEntry> candidates);
}
```

职责：

- 从 session 中抽取 durable memory 候选
- 对候选做归一化、补充标签、精简描述

### `MemoryRoutingPolicy`

建议实现逻辑：

- 偏个人风格、用户偏好、单 character 行为准则 → `CHARACTER`
- 偏协作原则、项目事实、多人共享上下文 → `WORKSPACE`
- 只与本轮临时对话有关 → 保留在 `SESSION`，不落长期

### `DefaultMemoryRoutingPolicy`

可以内置规则：

```text
1. 如果内容只影响某个 character 的响应风格 => CHARACTER
2. 如果内容对多个 character 的协作/项目事实有意义 => WORKSPACE
3. 如果内容未达到 durable 标准 => 不固化，停留 SESSION
```

### `MemoryDedupPolicy`

建议方法：

```java
public interface MemoryDedupPolicy {
    Optional<MemoryEntry> findDuplicate(MemoryEntry candidate, List<MemoryEntry> existing);
    MemoryEntry merge(MemoryEntry existing, MemoryEntry candidate);
}
```

职责：

- 避免重复 memory 文件
- 若已有相同主题，则更新而不是新建

### `MemoryValidationPolicy`

```java
public interface MemoryValidationPolicy {
    void validate(MemoryEntry entry);
}
```

职责：

- 校验 title / description / scope / type / content 是否合法
- 限制过长索引项、非法文件名、空正文等问题

---

## 6.7 Application / Orchestration 层

这一层是上层真正依赖的地方。

### `CharacterMemoryApplicationService`

```java
public class CharacterMemoryApplicationService {
    MemoryEntry save(String characterId, MemoryEntry entry);
    List<MemoryEntry> list(String characterId);
    List<MemorySearchResult> recall(String characterId, String query);
    void rebuildIndex(String characterId);
}
```

### `WorkspaceMemoryApplicationService`

```java
public class WorkspaceMemoryApplicationService {
    MemoryEntry save(String workspaceId, MemoryEntry entry);
    List<MemoryEntry> list(String workspaceId);
    List<MemorySearchResult> recall(String workspaceId, String query);
    void rebuildIndex(String workspaceId);
}
```

### `SessionMemoryApplicationService`

```java
public class SessionMemoryApplicationService {
    SessionSnapshot update(String sessionId, List<String> events);
    List<MemoryEntry> promoteToLongTerm(String sessionId);
}
```

### `MemoryFrameworkFacade`

这是整个框架建议暴露给业务层的总入口。

```java
public class DefaultMemoryFrameworkFacade implements MemoryFrameworkFacade {
    // 聚合 character/workspace/session 服务
}
```

职责：

- 统一对外 API
- 屏蔽底层 repository、index、extractor、routing、dedup 的复杂性

---

## 7. 三个层级分别应该暴露哪些接口

下面更细化到 `character / workspace / session` 三层。

## 7.1 Character 层

### 存储结构

```text
<charactor>/
├── MEMORY.md
└── mem/
    ├── xxx.md
    ├── yyy.md
    └── zzz.md
```

### 建议接口

#### `CharacterMemoryManager`

```java
public interface CharacterMemoryManager {
    MemoryEntry createMemory(String characterId, CreateMemoryRequest request);
    MemoryEntry updateMemory(String characterId, UpdateMemoryRequest request);
    Optional<MemoryEntry> getMemory(String characterId, String memoryId);
    List<MemoryEntry> listMemories(String characterId);
    void deleteMemory(String characterId, String memoryId);
    void rebuildIndex(String characterId);
    List<MemorySearchResult> recall(String characterId, String query, int limit);
}
```

### 职责边界

- 管单 character 的长期个性化记忆
- 管该 character 的偏好、风格、经验规则
- 不保存 workspace 公共规则
- 不保存只属于 session 的临时状态

### 典型内容

- 回复风格偏好
- 规划方式偏好
- 某类问题的固定工作模式
- 特定 agent 的领域记忆

---

## 7.2 Workspace 层

### 存储结构

```text
<workspace>/
├── MEMORY.md
└── mem/
    ├── architecture.md
    ├── release_rules.md
    └── shared_refs.md
```

### 建议接口

#### `WorkspaceMemoryManager`

```java
public interface WorkspaceMemoryManager {
    MemoryEntry createMemory(String workspaceId, CreateMemoryRequest request);
    MemoryEntry updateMemory(String workspaceId, UpdateMemoryRequest request);
    Optional<MemoryEntry> getMemory(String workspaceId, String memoryId);
    List<MemoryEntry> listMemories(String workspaceId);
    void deleteMemory(String workspaceId, String memoryId);
    void rebuildIndex(String workspaceId);
    List<MemorySearchResult> recall(String workspaceId, String query, int limit);
}
```

### 职责边界

- 管共享项目事实、协作约束、共识规则、共享参考资料
- 多个 character 在同一个 workspace 下共同使用
- 不承载某个 character 的个体偏好

### 典型内容

- 项目约束
- 共享架构决策
- 版本发布规则
- 外部系统引用
- workspace 级协作约定

---

## 7.3 Session 层

### 存储结构

```text
sessions/{sessionId}/
├── session-memory.md
├── events.jsonl
├── checkpoints/
└── extracted/
```

### 建议接口

#### `SessionMemoryManager`

```java
public interface SessionMemoryManager {
    SessionSnapshot startSession(StartSessionRequest request);
    SessionSnapshot appendDialogue(String sessionId, List<String> dialogueEvents);
    SessionSnapshot getSnapshot(String sessionId);
    void checkpoint(String sessionId);
    List<MemoryEntry> extractCandidates(String sessionId);
    List<MemoryEntry> promoteToLongTerm(String sessionId);
    void endSession(String sessionId);
}
```

### 职责边界

- 管理当前 session 的上下文压缩和摘要
- 支持 resume / compact / summary
- 负责向长期层输送候选，而不是直接替代长期记忆

### 典型内容

- 当前目标
- 最近完成事项
- 最近争议点
- 未决问题
- 当前上下文窗口的可恢复摘要

---

## 8. 建议的请求/响应模型

为了让上层服务调用简单，建议统一定义 request/response DTO。

### `CreateMemoryRequest`

```java
public class CreateMemoryRequest {
    private String title;
    private String description;
    private MemoryType type;
    private String content;
    private Map<String, String> tags;
}
```

### `UpdateMemoryRequest`

```java
public class UpdateMemoryRequest {
    private String memoryId;
    private String title;
    private String description;
    private String content;
    private Map<String, String> tags;
}
```

### `StartSessionRequest`

```java
public class StartSessionRequest {
    private String sessionId;
    private String workspaceId;
    private String characterId;
    private String initialGoal;
}
```

### `AgentMemoryContext`

```java
public class AgentMemoryContext {
    private String workspaceId;
    private String characterId;
    private String sessionId;
    private boolean includeWorkspaceMemory;
    private boolean includeCharacterMemory;
    private boolean includeSessionSummary;
}
```

职责：

- 上层 agent 在发起 recall 时传入统一上下文
- 简化 facade 的调用方式

---

## 9. 上层服务调用方式

下面给出几个典型调用样例，说明业务方如何使用这套框架。

## 9.1 样例一：创建 character memory

场景：某个 character 在多轮交互中学到“用户喜欢简洁回答”，将其写入 `<character>/mem/user_style.md`，并同步更新 `<character>/MEMORY.md`。

```java
MemoryFrameworkFacade facade = memoryFrameworkFacade();

MemoryEntry entry = new MemoryEntry();
entry.setTitle("User response style");
entry.setDescription("User prefers concise and direct responses");
entry.setType(MemoryType.FEEDBACK);
entry.setScope(MemoryScope.CHARACTER);
entry.setContent("User prefers concise and direct responses.\n\nWhy: Long summaries create noise.\nHow to apply: Default to short answers unless the user asks for detail.");

MemoryEntry saved = facade.saveCharacterMemory("character-001", entry);
System.out.println(saved.getFilePath());
```

预期效果：

- 生成 `characters/character-001/mem/user_response_style.md`
- 在 `characters/character-001/MEMORY.md` 中追加索引

---

## 9.2 样例二：workspace 级共享记忆写入

场景：多个 character 共同工作时，workspace 形成了一条共享规则：所有发布前必须跑集成回归。

```java
CreateMemoryRequest request = new CreateMemoryRequest();
request.setTitle("Release validation rule");
request.setDescription("All releases require integration regression before publish");
request.setType(MemoryType.WORKSPACE_DECISION);
request.setContent("All releases require integration regression before publish.\n\nWhy: Previous release had hidden integration issues.\nHow to apply: Any release task should include regression validation before final approval.");

MemoryEntry entry = workspaceMemoryManager.createMemory("workspace-alpha", request);
```

预期效果：

- 写入 `workspaces/workspace-alpha/mem/release_validation_rule.md`
- 更新 `workspaces/workspace-alpha/MEMORY.md`
- 后续同 workspace 下多个 character recall 时都可命中

---

## 9.3 样例三：更新 session memory

场景：当前会话发生了大量对话，需要更新 session summary。

```java
List<String> events = List.of(
    "User asked to split long-term memory into character and workspace scopes.",
    "Agent proposed MEMORY.md as index file and mem/ as content directory.",
    "Decision: session memory must remain separate from long-term memory."
);

SessionSnapshot snapshot = sessionMemoryService.update("session-20260401-001", events);
System.out.println(snapshot.getSummary());
```

预期效果：

- 更新 `sessions/session-20260401-001/session-memory.md`
- 写入 checkpoint 或 events 日志
- 不直接污染长期 memory

---

## 9.4 样例四：从 session 提取长期记忆候选并固化

场景：session 结束后，把 durable 信息沉淀到 character / workspace。

```java
List<MemoryEntry> promoted = facade.flushSessionToLongTerm("session-20260401-001");

for (MemoryEntry memory : promoted) {
    System.out.println(memory.getScope() + " -> " + memory.getTitle());
}
```

内部预期流程：

1. 读取 `session-memory.md` 和 `events.jsonl`
2. `MemoryExtractionService` 生成候选
3. `MemoryRoutingPolicy` 判断落点：character 或 workspace
4. `MemoryDedupPolicy` 判断是新建还是合并更新
5. repository 写入 topic memory
6. index service 更新相应 `MEMORY.md`

---

## 9.5 样例五：agent 启动前召回可用 memory

场景：character 在执行任务前，需要把 session + character + workspace 三层上下文拼起来。

```java
AgentMemoryContext context = new AgentMemoryContext();
context.setWorkspaceId("workspace-alpha");
context.setCharacterId("character-001");
context.setSessionId("session-20260401-001");
context.setIncludeWorkspaceMemory(true);
context.setIncludeCharacterMemory(true);
context.setIncludeSessionSummary(true);

List<MemorySearchResult> recalls = facade.recallForAgent(
    context,
    "How should I answer architecture questions for this workspace?"
);

for (MemorySearchResult result : recalls) {
    System.out.println(result.getMemory().getTitle() + " => " + result.getReason());
}
```

内部策略建议：

- 先拿 session summary
- 再召回 character memory
- 再召回 workspace memory
- 最后合并排序并控制数量

---

## 10. 推荐的上层编排方式

如果上层有 agent runtime / workflow engine，建议采用下面的调用顺序。

## 10.1 对话开始

```text
startSession()
  -> load character MEMORY.md index
  -> load workspace MEMORY.md index
  -> initialize session-memory.md
```

## 10.2 每轮对话后

```text
appendDialogue()
  -> session update
  -> threshold reached ? summarize() : skip
```

## 10.3 agent 执行动作前

```text
recallForAgent()
  -> session recall
  -> character recall
  -> workspace recall
  -> merge + rank + inject
```

## 10.4 会话结束或定期异步任务

```text
flushSessionToLongTerm()
  -> extract candidates
  -> routing
  -> dedup
  -> persist
  -> update MEMORY.md index
```

---

## 11. 推荐的实现顺序

如果准备从零开始实现，建议按下面顺序推进。

### 第一阶段：文件模型打底

先实现：

- `MemoryPathResolver`
- `MemoryMarkdownParser`
- `MemoryIndexParser`
- `FileCharacterMemoryRepository`
- `FileWorkspaceMemoryRepository`
- `FileSessionMemoryRepository`

这一阶段目标：

- 能正确落目录
- 能写 `MEMORY.md`
- 能写 `mem/*.md`
- 能读回完整 memory

### 第二阶段：应用层跑通

再实现：

- `CharacterMemoryApplicationService`
- `WorkspaceMemoryApplicationService`
- `SessionMemoryApplicationService`
- `MemoryFrameworkFacade`

这一阶段目标：

- 上层可以统一创建、读取、列举 memory
- session 能维护摘要

### 第三阶段：提取与召回

再补：

- `MemoryRecallService`
- `MemoryRanker`
- `MemoryExtractionService`
- `MemoryRoutingPolicy`
- `MemoryDedupPolicy`

这一阶段目标：

- 支持从 session 向长期记忆沉淀
- 支持 runtime recall

### 第四阶段：增强治理

最后补：

- archive / version / audit
- 向量索引
- 敏感信息校验
- 多租户权限
- UI / API 管理页

---

## 11.5 Spring Boot 落地建议：更紧凑的包结构 + 类骨架

如果按真正可落地的 Spring Boot 项目来组织，前面那版分层可以再收紧一些，避免目录过散、类分布过碎。

这里建议采用 **4 层主包 + 1 层基础设施包** 的紧凑结构：

```text
com.example.memory/
├── MemoryApplication.java
├── config/
│   ├── MemoryProperties.java
│   └── MemoryAutoConfiguration.java
├── core/
│   ├── model/
│   │   ├── MemoryEntry.java
│   │   ├── MemoryIndexItem.java
│   │   ├── SessionSnapshot.java
│   │   ├── MemoryScope.java
│   │   └── MemoryType.java
│   ├── policy/
│   │   ├── MemoryRoutingPolicy.java
│   │   ├── MemoryDedupPolicy.java
│   │   └── MemoryRanker.java
│   └── service/
│       ├── MemoryFacade.java
│       ├── CharacterMemoryService.java
│       ├── WorkspaceMemoryService.java
│       ├── SessionMemoryService.java
│       ├── MemoryRecallService.java
│       └── MemoryExtractionService.java
├── storage/
│   ├── path/
│   │   └── MemoryPathResolver.java
│   ├── parser/
│   │   ├── MemoryMarkdownParser.java
│   │   └── MemoryIndexParser.java
│   ├── repo/
│   │   ├── CharacterMemoryRepository.java
│   │   ├── WorkspaceMemoryRepository.java
│   │   └── SessionMemoryRepository.java
│   └── fs/
│       ├── FileCharacterMemoryRepository.java
│       ├── FileWorkspaceMemoryRepository.java
│       ├── FileSessionMemoryRepository.java
│       └── FileMemoryIndexSupport.java
├── app/
│   ├── command/
│   │   ├── CreateCharacterMemoryCommand.java
│   │   ├── CreateWorkspaceMemoryCommand.java
│   │   └── UpdateSessionCommand.java
│   ├── query/
│   │   ├── RecallMemoryQuery.java
│   │   └── GetSessionSnapshotQuery.java
│   └── impl/
│       ├── DefaultMemoryFacade.java
│       ├── DefaultCharacterMemoryService.java
│       ├── DefaultWorkspaceMemoryService.java
│       ├── DefaultSessionMemoryService.java
│       ├── DefaultMemoryRecallService.java
│       └── DefaultMemoryExtractionService.java
└── web/
    ├── MemoryController.java
    ├── SessionController.java
    └── dto/
        ├── CreateMemoryRequest.java
        ├── RecallRequest.java
        └── SessionUpdateRequest.java
```

这一版的核心思想是：

- `core`：只放领域模型、策略接口、服务接口
- `storage`：只放路径、解析器、repository 与文件实现
- `app`：只放 use case 编排与默认实现
- `web`：只放 HTTP 接口与 DTO
- `config`：只放 Spring Boot 配置装配

这样比前面的拆分更紧凑，也更符合中型 Spring Boot 项目的习惯。

### 11.5.1 紧凑版职责划分

#### `config`

职责：

- 读取 `application.yml`
- 装配默认 Bean
- 组织文件版 / 后续数据库版实现切换

建议类：

- `MemoryProperties`
- `MemoryAutoConfiguration`

#### `core.model`

职责：

- 定义稳定领域对象
- 不依赖 Spring MVC / JPA / 文件系统

建议类：

- `MemoryEntry`
- `MemoryIndexItem`
- `SessionSnapshot`
- `MemoryScope`
- `MemoryType`

#### `core.policy`

职责：

- 定义可替换的策略接口
- 后续支持规则版、AI 判定版、混合版

建议接口：

- `MemoryRoutingPolicy`
- `MemoryDedupPolicy`
- `MemoryRanker`

#### `core.service`

职责：

- 暴露统一业务接口
- 上层业务只依赖这里，不依赖 repository

建议接口：

- `MemoryFacade`
- `CharacterMemoryService`
- `WorkspaceMemoryService`
- `SessionMemoryService`
- `MemoryRecallService`
- `MemoryExtractionService`

#### `storage`

职责：

- 屏蔽文件系统与索引格式细节
- 提供 repository 和 parser 的稳定实现

建议类：

- `MemoryPathResolver`
- `MemoryMarkdownParser`
- `MemoryIndexParser`
- `CharacterMemoryRepository`
- `WorkspaceMemoryRepository`
- `SessionMemoryRepository`
- `FileCharacterMemoryRepository`
- `FileWorkspaceMemoryRepository`
- `FileSessionMemoryRepository`

#### `app.impl`

职责：

- 把 repository、policy、parser、session 流程编排起来
- 是真正的用例实现层

建议类：

- `DefaultMemoryFacade`
- `DefaultCharacterMemoryService`
- `DefaultWorkspaceMemoryService`
- `DefaultSessionMemoryService`
- `DefaultMemoryRecallService`
- `DefaultMemoryExtractionService`

#### `web`

职责：

- 提供 REST API
- 做参数校验和响应映射
- 不直接写业务逻辑

建议类：

- `MemoryController`
- `SessionController`
- request/response DTO

---

### 11.5.2 推荐的 Spring Boot 配置骨架

#### `MemoryProperties`

```java
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {
    private String rootDir = "./data/memory";
    private int maxIndexLines = 200;
    private int maxIndexBytes = 25_000;
    private int defaultRecallLimit = 5;
    private boolean enableSessionCheckpoint = true;

    // getters / setters
}
```

建议 `application.yml`：

```yaml
memory:
  root-dir: ./data/memory
  max-index-lines: 200
  max-index-bytes: 25000
  default-recall-limit: 5
  enable-session-checkpoint: true
```

#### `MemoryAutoConfiguration`

```java
@Configuration
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryAutoConfiguration {

    @Bean
    public MemoryPathResolver memoryPathResolver(MemoryProperties properties) {
        return new DefaultMemoryPathResolver(properties);
    }

    @Bean
    public MemoryMarkdownParser memoryMarkdownParser() {
        return new DefaultMemoryMarkdownParser();
    }

    @Bean
    public MemoryIndexParser memoryIndexParser() {
        return new DefaultMemoryIndexParser();
    }

    @Bean
    public CharacterMemoryRepository characterMemoryRepository(
            MemoryPathResolver pathResolver,
            MemoryMarkdownParser markdownParser,
            MemoryIndexParser indexParser) {
        return new FileCharacterMemoryRepository(pathResolver, markdownParser, indexParser);
    }

    @Bean
    public WorkspaceMemoryRepository workspaceMemoryRepository(
            MemoryPathResolver pathResolver,
            MemoryMarkdownParser markdownParser,
            MemoryIndexParser indexParser) {
        return new FileWorkspaceMemoryRepository(pathResolver, markdownParser, indexParser);
    }

    @Bean
    public SessionMemoryRepository sessionMemoryRepository(
            MemoryPathResolver pathResolver,
            MemoryMarkdownParser markdownParser) {
        return new FileSessionMemoryRepository(pathResolver, markdownParser);
    }
}
```

这里的要点是：

- 配置层只做装配，不写业务逻辑
- repository 默认走文件实现
- 后续如果切换数据库，只替换 Bean 即可

---

### 11.5.3 核心接口的 Spring 风格骨架

#### `MemoryFacade`

```java
public interface MemoryFacade {
    MemoryEntry saveCharacterMemory(String characterId, CreateMemoryCommand command);
    MemoryEntry saveWorkspaceMemory(String workspaceId, CreateMemoryCommand command);
    SessionSnapshot updateSession(String sessionId, UpdateSessionCommand command);
    List<MemoryEntry> flushSessionToLongTerm(String sessionId);
    List<MemorySearchResult> recall(RecallMemoryQuery query);
}
```

说明：

- `Facade` 是上层唯一总入口
- 适合给 orchestration、workflow engine、controller 调用

#### `CharacterMemoryService`

```java
public interface CharacterMemoryService {
    MemoryEntry create(String characterId, CreateMemoryCommand command);
    MemoryEntry update(String characterId, UpdateMemoryCommand command);
    Optional<MemoryEntry> get(String characterId, String memoryId);
    List<MemoryEntry> list(String characterId);
    void delete(String characterId, String memoryId);
    void rebuildIndex(String characterId);
}
```

#### `WorkspaceMemoryService`

```java
public interface WorkspaceMemoryService {
    MemoryEntry create(String workspaceId, CreateMemoryCommand command);
    MemoryEntry update(String workspaceId, UpdateMemoryCommand command);
    Optional<MemoryEntry> get(String workspaceId, String memoryId);
    List<MemoryEntry> list(String workspaceId);
    void delete(String workspaceId, String memoryId);
    void rebuildIndex(String workspaceId);
}
```

#### `SessionMemoryService`

```java
public interface SessionMemoryService {
    SessionSnapshot start(String sessionId, String workspaceId, String characterId);
    SessionSnapshot update(String sessionId, UpdateSessionCommand command);
    SessionSnapshot get(String sessionId);
    void checkpoint(String sessionId);
    List<MemoryEntry> extractCandidates(String sessionId);
    List<MemoryEntry> promote(String sessionId);
    void close(String sessionId);
}
```

#### `MemoryRecallService`

```java
public interface MemoryRecallService {
    List<MemorySearchResult> recallCharacter(String characterId, String query, int limit);
    List<MemorySearchResult> recallWorkspace(String workspaceId, String query, int limit);
    List<MemorySearchResult> recallComposite(RecallMemoryQuery query);
}
```

#### `MemoryExtractionService`

```java
public interface MemoryExtractionService {
    List<MemoryEntry> extract(SessionSnapshot snapshot, List<String> events);
    List<MemoryEntry> promote(String sessionId, SessionSnapshot snapshot, List<String> events);
}
```

---

### 11.5.4 默认实现类骨架

#### `DefaultMemoryFacade`

```java
@Service
@RequiredArgsConstructor
public class DefaultMemoryFacade implements MemoryFacade {

    private final CharacterMemoryService characterMemoryService;
    private final WorkspaceMemoryService workspaceMemoryService;
    private final SessionMemoryService sessionMemoryService;
    private final MemoryRecallService memoryRecallService;

    @Override
    public MemoryEntry saveCharacterMemory(String characterId, CreateMemoryCommand command) {
        return characterMemoryService.create(characterId, command);
    }

    @Override
    public MemoryEntry saveWorkspaceMemory(String workspaceId, CreateMemoryCommand command) {
        return workspaceMemoryService.create(workspaceId, command);
    }

    @Override
    public SessionSnapshot updateSession(String sessionId, UpdateSessionCommand command) {
        return sessionMemoryService.update(sessionId, command);
    }

    @Override
    public List<MemoryEntry> flushSessionToLongTerm(String sessionId) {
        return sessionMemoryService.promote(sessionId);
    }

    @Override
    public List<MemorySearchResult> recall(RecallMemoryQuery query) {
        return memoryRecallService.recallComposite(query);
    }
}
```

#### `DefaultCharacterMemoryService`

```java
@Service
@RequiredArgsConstructor
public class DefaultCharacterMemoryService implements CharacterMemoryService {

    private final CharacterMemoryRepository repository;
    private final MemoryValidationPolicy validationPolicy;

    @Override
    public MemoryEntry create(String characterId, CreateMemoryCommand command) {
        MemoryEntry entry = MemoryEntryFactory.forCharacter(characterId, command);
        validationPolicy.validate(entry);
        return repository.create(characterId, entry);
    }

    @Override
    public MemoryEntry update(String characterId, UpdateMemoryCommand command) {
        MemoryEntry current = repository.get(characterId, command.getMemoryId())
                .orElseThrow(() -> new IllegalArgumentException("memory not found"));
        current.setTitle(command.getTitle());
        current.setDescription(command.getDescription());
        current.setContent(command.getContent());
        validationPolicy.validate(current);
        return repository.update(characterId, current);
    }

    @Override
    public Optional<MemoryEntry> get(String characterId, String memoryId) {
        return repository.get(characterId, memoryId);
    }

    @Override
    public List<MemoryEntry> list(String characterId) {
        return repository.list(characterId);
    }

    @Override
    public void delete(String characterId, String memoryId) {
        repository.delete(characterId, memoryId);
    }

    @Override
    public void rebuildIndex(String characterId) {
        repository.rebuildIndex(characterId);
    }
}
```

#### `DefaultSessionMemoryService`

```java
@Service
@RequiredArgsConstructor
public class DefaultSessionMemoryService implements SessionMemoryService {

    private final SessionMemoryRepository sessionRepository;
    private final MemoryExtractionService extractionService;
    private final MemoryRoutingPolicy routingPolicy;
    private final CharacterMemoryRepository characterRepository;
    private final WorkspaceMemoryRepository workspaceRepository;

    @Override
    public SessionSnapshot start(String sessionId, String workspaceId, String characterId) {
        SessionSnapshot snapshot = new SessionSnapshot();
        snapshot.setSessionId(sessionId);
        snapshot.setWorkspaceId(workspaceId);
        snapshot.setCharacterId(characterId);
        snapshot.setSummary("Session initialized.");
        return sessionRepository.save(sessionId, snapshot);
    }

    @Override
    public SessionSnapshot update(String sessionId, UpdateSessionCommand command) {
        SessionSnapshot snapshot = sessionRepository.load(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
        snapshot.setSummary(command.getSummary());
        command.getEvents().forEach(e -> sessionRepository.appendEvent(sessionId, e));
        return sessionRepository.save(sessionId, snapshot);
    }

    @Override
    public List<MemoryEntry> promote(String sessionId) {
        SessionSnapshot snapshot = get(sessionId);
        List<String> events = sessionRepository.listEvents(sessionId);
        List<MemoryEntry> candidates = extractionService.extract(snapshot, events);

        for (MemoryEntry candidate : candidates) {
            MemoryScope scope = routingPolicy.route(candidate, snapshot);
            if (scope == MemoryScope.CHARACTER) {
                characterRepository.create(snapshot.getCharacterId(), candidate);
            } else if (scope == MemoryScope.WORKSPACE) {
                workspaceRepository.create(snapshot.getWorkspaceId(), candidate);
            }
        }
        return candidates;
    }

    @Override
    public SessionSnapshot get(String sessionId) {
        return sessionRepository.load(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
    }

    @Override
    public void checkpoint(String sessionId) {
        sessionRepository.checkpoint(sessionId, get(sessionId));
    }

    @Override
    public List<MemoryEntry> extractCandidates(String sessionId) {
        SessionSnapshot snapshot = get(sessionId);
        return extractionService.extract(snapshot, sessionRepository.listEvents(sessionId));
    }

    @Override
    public void close(String sessionId) {
        checkpoint(sessionId);
    }
}
```

---

### 11.5.5 Repository 骨架

#### `CharacterMemoryRepository`

```java
public interface CharacterMemoryRepository {
    MemoryEntry create(String characterId, MemoryEntry entry);
    MemoryEntry update(String characterId, MemoryEntry entry);
    Optional<MemoryEntry> get(String characterId, String memoryId);
    List<MemoryEntry> list(String characterId);
    void delete(String characterId, String memoryId);
    void rebuildIndex(String characterId);
}
```

#### `FileCharacterMemoryRepository`

```java
@Repository
@RequiredArgsConstructor
public class FileCharacterMemoryRepository implements CharacterMemoryRepository {

    private final MemoryPathResolver pathResolver;
    private final MemoryMarkdownParser markdownParser;
    private final MemoryIndexParser indexParser;

    @Override
    public MemoryEntry create(String characterId, MemoryEntry entry) {
        Path memDir = pathResolver.resolveCharacterMemDir(characterId);
        Path file = memDir.resolve(entry.getFileName());
        FileIO.ensureDirectory(memDir);
        FileIO.writeString(file, markdownParser.render(entry));
        rebuildIndex(characterId);
        return entry;
    }

    @Override
    public MemoryEntry update(String characterId, MemoryEntry entry) {
        Path file = pathResolver.resolveCharacterMemDir(characterId).resolve(entry.getFileName());
        FileIO.writeString(file, markdownParser.render(entry));
        rebuildIndex(characterId);
        return entry;
    }

    @Override
    public Optional<MemoryEntry> get(String characterId, String memoryId) {
        return FileMemoryLookup.findById(pathResolver.resolveCharacterMemDir(characterId), memoryId, markdownParser);
    }

    @Override
    public List<MemoryEntry> list(String characterId) {
        return FileMemoryLookup.list(pathResolver.resolveCharacterMemDir(characterId), markdownParser);
    }

    @Override
    public void delete(String characterId, String memoryId) {
        FileMemoryLookup.delete(pathResolver.resolveCharacterMemDir(characterId), memoryId, markdownParser);
        rebuildIndex(characterId);
    }

    @Override
    public void rebuildIndex(String characterId) {
        List<MemoryEntry> entries = list(characterId);
        Path indexFile = pathResolver.resolveCharacterIndex(characterId);
        String indexContent = FileMemoryIndexSupport.renderIndex(entries, indexParser);
        FileIO.writeString(indexFile, indexContent);
    }
}
```

`Workspace` repository 与之基本同构，只是路径切到 `workspace`。

`Session` repository 则建议只维护：

- `session-memory.md`
- `events.jsonl`
- `checkpoints/`

而不要实现 `MEMORY.md` 索引。

---

### 11.5.6 Web 层骨架

#### `MemoryController`

```java
@RestController
@RequestMapping("/api/memories")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryFacade memoryFacade;

    @PostMapping("/characters/{characterId}")
    public MemoryEntry createCharacterMemory(
            @PathVariable String characterId,
            @RequestBody CreateMemoryRequest request) {
        return memoryFacade.saveCharacterMemory(characterId, request.toCommand());
    }

    @PostMapping("/workspaces/{workspaceId}")
    public MemoryEntry createWorkspaceMemory(
            @PathVariable String workspaceId,
            @RequestBody CreateMemoryRequest request) {
        return memoryFacade.saveWorkspaceMemory(workspaceId, request.toCommand());
    }

    @PostMapping("/recall")
    public List<MemorySearchResult> recall(@RequestBody RecallRequest request) {
        return memoryFacade.recall(request.toQuery());
    }
}
```

#### `SessionController`

```java
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final MemoryFacade memoryFacade;

    @PostMapping("/{sessionId}/update")
    public SessionSnapshot updateSession(
            @PathVariable String sessionId,
            @RequestBody SessionUpdateRequest request) {
        return memoryFacade.updateSession(sessionId, request.toCommand());
    }

    @PostMapping("/{sessionId}/flush")
    public List<MemoryEntry> flush(@PathVariable String sessionId) {
        return memoryFacade.flushSessionToLongTerm(sessionId);
    }
}
```

这里建议保持：

- controller 只接 DTO，不直接组装 `MemoryEntry`
- DTO 转 command/query
- facade 负责真正编排

---

### 11.5.7 一套最小可跑版本的精简实现建议

如果你想先落地 MVP，不建议一开始就实现文档前面那套完整接口矩阵，而是先收敛成下面这些类：

```text
com.example.memory/
├── config/
│   └── MemoryProperties.java
├── core/
│   ├── MemoryEntry.java
│   ├── SessionSnapshot.java
│   ├── MemoryScope.java
│   ├── MemoryType.java
│   └── MemoryFacade.java
├── storage/
│   ├── MemoryPathResolver.java
│   ├── MemoryMarkdownParser.java
│   ├── FileCharacterMemoryRepository.java
│   ├── FileWorkspaceMemoryRepository.java
│   └── FileSessionMemoryRepository.java
├── app/
│   ├── DefaultMemoryFacade.java
│   ├── DefaultMemoryRecallService.java
│   └── DefaultMemoryExtractionService.java
└── web/
    ├── MemoryController.java
    └── SessionController.java
```

MVP 先做到：

- 能写 character memory
- 能写 workspace memory
- 能更新 session summary
- 能从 session 提取候选并手动固化
- 能做基础 recall

等这些稳定后，再逐步补：

- dedup policy
- routing policy
- ranking policy
- archive / version / audit
- hybrid recall

这比一步到位更稳，也更符合 Spring Boot 服务迭代习惯。

---

### 11.5.8 最终建议

如果是你当前这个场景，我建议真正开工时采用下面这套判断标准：

- **如果目标是先跑起来**：使用 `MVP 紧凑版`
- **如果目标是给团队长期维护**：使用 `4 层主包 + 1 层基础设施包`
- **如果后面一定会接数据库 / 向量检索**：保留 `core.service` 与 `storage.repo` 的接口分离，不要直接把业务写死在文件实现类里

一句话总结这节：

> Spring Boot 版不需要把目录拆得过细，最实用的组织方式是 `core + storage + app + web + config`，接口放 `core`，默认实现放 `app`，文件与索引逻辑放 `storage`，这样既紧凑，也保留了后续扩展空间。

---

## 12. 关键设计取舍

## 12.1 为什么仍然保留 `MEMORY.md`

即便是 Java 服务端方案，也建议保留 `MEMORY.md`，原因是：

- 调试方便
- 结构清晰
- 可导入导出
- 与原始 Claude memory 设计保持一致
- 索引文件能天然作为模型 prompt 的轻量入口

## 12.2 为什么 session 不直接写进 character/workspace

因为 session 包含大量“当前轮次有用，但长期不稳定”的上下文，如果不做隔离，会造成：

- 噪声记忆堆积
- 索引膨胀
- 长期记忆被短期状态污染

所以 session 必须先摘要，再提取，再固化。

## 12.3 为什么 character 与 workspace 要分开

这是整个设计的关键边界：

- `character` 表示单 agent 长期人格/偏好/行为记忆
- `workspace` 表示多 agent 协作共享知识

如果混在一起，后续会出现：

- character 私有偏好污染团队规则
- workspace 公共约束被错误当成 agent 个性
- 召回粒度无法控制

---

## 13. 最终总结

综合来看，这套 Java memory framework 的本质是：

> 用 `character`、`workspace`、`session` 三层作用域，把“单 agent 长期记忆”、“多 agent 协作共享记忆”和“当前会话短期记忆”拆开管理，并通过 `MEMORY.md + mem/*.md` 的文件化结构与 Java 服务接口体系结合，形成一套既可治理、又可扩展、还能支持后续召回和摘要固化的完整记忆框架。

从工程实现上，最重要的是记住三件事：

1. `MEMORY.md` 永远是索引，不是正文
2. session memory 永远独立于长期记忆
3. 上层只依赖 facade/application service，不直接操作底层文件和索引

如果按这个方案实现，后续你可以很自然地继续扩展：

- 多租户 memory
- 向量召回
- workspace 团队协作权限
- character persona 管理
- session resume / compact
- memory 审计和归档

这套结构既能保留 Claude memory 的核心优点，也适合演进成 Java 服务端长期可维护的工程体系。