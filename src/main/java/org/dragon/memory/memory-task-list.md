# Memory Java 实现任务清单

## 1. 文档目标

本文把以下两份设计文档进一步收敛为一个可执行的 Java 落地任务清单：

- `docs/design/memory-sqllite-impl.md`
- `docs/design/memory-qmd-impl.md`

目标不是重复设计，而是回答更实际的工程推进问题：

- 先做什么，后做什么
- 哪些任务可以定义为 MVP，哪些属于稳定性补齐
- 每个阶段的产出物是什么
- 每个阶段完成后，如何判断已经“可以进入下一阶段”
- 哪些设计分歧必须在编码前拍板

本文默认 Java 版最终要支持两条后端路径：

- builtin / sqlite
- qmd + builtin fallback

---

## 2. 总体实施策略

### 2.1 推荐顺序

推荐按以下顺序推进，而不是一开始同时实现所有能力：

1. 统一 API / DTO / manager factory。
2. 先打通 SQLite MVP。
3. 再补齐 SQLite 稳定性能力。
4. 在统一入口下接入 QMD 基础设施。
5. 再补 QMD 搜索、同步、repair 与 fallback。
6. 最后做跨后端一致性和测试矩阵收口。

### 2.2 为什么先做 SQLite

原因不是 SQLite 更重要，而是它更适合作为 Java 版的基础落地路径：

- 不依赖外部 `qmd` 进程，调试面更小
- 更容易先把统一接口、DTO、状态结构跑通
- QMD fallback 目标本身就是 builtin，因此 builtin 必须先稳定
- QMD 的很多对外语义要和 builtin 对齐，先有 builtin 更容易校准行为

### 2.3 实施原则

- 统一入口先行，不要先把 backend 分叉到两套 API。
- builtin 与 qmd 共享 `MemorySearchManager`、`MemoryProviderStatus`、`SearchOptions` 等对外模型。
- 先实现“可工作”，再补“高鲁棒性”，但设计上要留出升级位置。
- 任何会影响索引一致性的行为，都要有明确的重试、回滚或降级语义。
- session memory 的行号语义必须在实现前明确，否则后续引用会返工。

---

## 3. 里程碑总览

| 里程碑 | 目标 | 结果定义 |
| - | - | - |
| M0 | 统一 API 与启动骨架 | 统一接口、DTO、factory、config 解析可编译 |
| M1 | SQLite MVP | 可索引 markdown，可 search/read/status/sync |
| M2 | SQLite 稳定化 | FTS-only、vector fallback、safe reindex、session sync、readonly recovery |
| M3 | QMD 基础设施 | runtime、CLI、collections、status、doc resolve 骨架可用 |
| M4 | QMD 搜索链路 | scope、CLI/mcporter、多 collection、fallback to query、结果映射 |
| M5 | QMD 更新链路 | update queue、boot/interval、embed、session export、repair |
| M6 | 统一降级与测试收口 | qmd->builtin fallback、状态一致性、集成测试矩阵 |

---

## 4. 编码前必须拍板的设计点

这些点如果不先定，会导致实现过程中频繁返工。

### 4.1 session 行号语义

必须二选一并写入 Java 实现约束：

- 方案 A：搜索结果行号回到原始 transcript `.jsonl`
- 方案 B：QMD session 结果行号只对应导出的 markdown

如果选择方案 A，则：

- SQLite 路径必须保留 `lineMap`
- QMD 路径必须额外持久化 `markdown line -> jsonl line` 映射

### 4.2 QMD embed 锁作用域

必须明确：

- 仅 manager 实例内串行
- 同 JVM 内所有 QMD manager 全局串行

如果预期多个 agent 共用同一宿主进程，推荐采用 **JVM 内全局串行**。

### 4.3 QMD 状态结构与统一 DTO 的边界

必须明确：

- `MemoryProviderStatus` 顶层字段统一
- QMD 可以省略 sqlite 专属字段
- fallback 信息由 wrapper 注入，而不是各 backend 自己伪造

### 4.4 配置合并规则

必须明确：

- defaults 与 agent override 做深合并
- 数组字段按语义处理，不是简单拼接
- 哪些字段变化会触发 full reindex

---

## 5. M0 统一 API 与启动骨架

### 5.1 API 与 DTO

#### 任务

- [ ] 定义 `MemorySearchManager`
- [ ] 定义 `SearchOptions`
- [ ] 定义 `ReadFileRequest`
- [ ] 定义 `ReadFileResult`
- [ ] 定义 `SyncRequest`
- [ ] 定义 `MemorySearchResult`
- [ ] 定义 `MemoryProviderStatus`
- [ ] 定义 `MemoryEmbeddingProbeResult`
- [ ] 定义 `SourceCount`、`FallbackStatus` 等状态子结构

#### 产出物

- `ai.openclaw.memory.api.*`

#### 完成标准

- builtin 与 qmd 两条路径都可以直接复用这些接口
- `status()` 返回结构可同时表达 builtin 与 qmd
- DTO 不暴露 SQLite/QMD 特有类型

### 5.2 backend 选择与统一工厂

#### 任务

- [ ] 实现 `MemoryBackendSelector`
- [ ] 实现 `MemorySearchManagerFactory`
- [ ] 定义 `Purpose`（至少包含 `default` 与 `status`）
- [ ] 设计 manager cache key 规则
- [ ] 设计 pending future cache 规则

#### 关键约束

- 同一 key 并发初始化只能发生一次
- pending future 失败必须及时清理
- `purpose=status` 必须允许轻量初始化

#### 完成标准

- 上层只依赖 factory 获取 manager
- 不同 backend 的分发逻辑不散落到调用方
- 并发获取同一 manager 不会重复初始化

### 5.3 运行时配置解析骨架

#### 任务

- [ ] 实现 `MemoryRuntimeConfigResolver`
- [ ] 明确 deep merge 规则
- [ ] 明确 normalize / clamp / validate 顺序
- [ ] 明确 disabled config 返回策略

#### 完成标准

- 配置解析结果可直接供 builtin 实现使用
- 将来扩展到 qmd 时无需重写入口层

---

## 6. M1 SQLite MVP

目标：先跑通 builtin / sqlite 的最小闭环。

### 6.1 schema 与 repository

#### 任务

- [ ] 实现 `MemorySchemaManager.ensureSchema()`
- [ ] 建立 `meta` / `files` / `chunks` / `embedding_cache` 表
- [ ] 尝试创建 FTS 表并返回可用性
- [ ] 实现 `MemoryIndexRepository` 的最小查询/写入能力

#### 完成标准

- 可以写入文件记录和 chunk 记录
- 可以读取基本统计信息
- FTS 创建失败时不会让 manager 整体初始化失败

### 6.2 memory 文件扫描与分块

#### 任务

- [ ] 实现 `MemoryFileScanner.listMemoryFiles()`
- [ ] 实现 `MemoryFileScanner.buildFileEntry()`
- [ ] 实现 `MarkdownChunker`
- [ ] 明确默认 memory 路径与 `extraPaths` 规则

#### 完成标准

- 能识别 `MEMORY.md` / `memory.md` / `memory/**`
- 能输出稳定 chunk 边界与 hash
- symlink、非法路径、非法后缀被正确过滤

### 6.3 embedding provider MVP

#### 任务

- [ ] 实现 `EmbeddingProviderFactory` 的最小版本
- [ ] 先接入一个 provider
- [ ] 定义 query embedding 与 batch embedding 的统一接口
- [ ] 允许 provider 不可用时返回 `provider=null`

#### 完成标准

- provider 存在时能生成 embedding
- provider 不存在时系统仍可继续初始化，为 FTS-only 留好入口

### 6.4 索引写入链路

#### 任务

- [ ] 实现 `MemoryEmbeddingIndexer.indexFile()`
- [ ] 实现 chunk -> embedding -> files/chunks/fts 写入流程
- [ ] 引入 embedding cache 最小实现
- [ ] 定义 `providerKey` 计算骨架

#### 完成标准

- 一个 markdown 文件可完整写入索引
- 重复索引同一文件不会残留旧 chunk 脏数据
- cache key 结构已固定，后面可扩展但不必返工 DTO

### 6.5 查询链路 MVP

#### 任务

- [ ] 实现 `MemoryQueryEngine.search()`
- [ ] 实现 `searchKeyword()`
- [ ] 实现 `searchVector()`
- [ ] 实现 `HybridResultMerger` 的最小版本

#### 完成标准

- provider 存在时可走 hybrid 或 vector-only
- provider 为空但 FTS 可用时可走 FTS-only
- `MemorySearchResult.score` 为可排序的归一化数值

### 6.6 builtin facade

#### 任务

- [ ] 实现 `MemoryIndexManager.search()`
- [ ] 实现 `MemoryIndexManager.sync()`
- [ ] 实现 `MemoryIndexManager.readFile()`
- [ ] 实现 `MemoryIndexManager.status()`
- [ ] 实现 `probeEmbeddingAvailability()` / `probeVectorAvailability()`

#### 完成标准

- 完成一次 force sync 后，`search/readFile/status` 全链路可用
- `status()` 能反映 files/chunks/sourceCounts/searchMode
- `readFile()` 对缺失文件返回空文本，不是硬异常

### 6.7 M1 验收

- [ ] `MEMORY.md` 改动后可重新 sync
- [ ] `search()` 能返回排序后的结果
- [ ] `readFile()` 能读取片段
- [ ] `status()` 能返回运行时快照
- [ ] 至少有一组端到端测试覆盖 sync -> search -> readFile

---

## 7. M2 SQLite 稳定化

目标：把 SQLite 路径从“能跑”升级为“可长期维护”。

### 7.1 FTS-only 正式化

#### 任务

- [ ] provider 不存在时进入正式 FTS-only 模式
- [ ] `status().custom.searchMode = "fts-only"`
- [ ] `probeEmbeddingAvailability=false`
- [ ] `probeVectorAvailability=false`

#### 完成标准

- provider 缺失不再被视为 manager 构造失败
- FTS-only 行为在测试中有明确断言

### 7.2 vector fallback

#### 任务

- [ ] 实现 `VectorExtensionManager`
- [ ] sqlite-vec 可用时走真实向量检索
- [ ] sqlite-vec 不可用时退回内存 cosine
- [ ] 维度变化时自动重建向量表

#### 完成标准

- sqlite-vec 加载失败不会让搜索整体失败
- 向量维度变化后不会复用旧表

### 7.3 safe reindex 与崩溃恢复

#### 任务

- [ ] 实现 `runSafeReindex()`
- [ ] 实现 temp DB 构建
- [ ] 实现 DB swap
- [ ] 处理 rename 平台差异
- [ ] 实现 backup / temp 残留恢复检查

#### 完成标准

- full reindex 不依赖原地删表重建
- swap 失败后 old DB 仍可用
- 启动时可处理上次崩溃留下的 temp/backup 文件

### 7.4 readonly recovery

#### 任务

- [ ] 识别 readonly DB error
- [ ] 关闭并重开 DB
- [ ] 重置 vector / fts runtime 状态
- [ ] 重试一次同步
- [ ] 统计 attempts / successes / failures

#### 完成标准

- 第一次只读失败能自动恢复时，不需要人工干预
- 第二次仍失败时才向上抛错
- `status().custom.readonlyRecovery` 有统计信息

### 7.5 meta 与 full reindex 判定

#### 任务

- [ ] 定义 `MemoryIndexMeta`
- [ ] 把 `providerKey`、model、sources、scopeHash、vectorDims、chunk 配置写入 meta
- [ ] 实现 `needsFullReindex` 判定
- [ ] 处理 meta parse error

#### 完成标准

- provider/model/baseUrl/dims 变化能触发重建
- meta 损坏时会进入保守重建，而不是继续带脏索引运行

### 7.6 session transcript adapter

#### 任务

- [ ] 实现 `SessionTranscriptAdapter.listSessionFiles()`
- [ ] 实现 `buildSessionEntry()`
- [ ] 实现 `extractSessionText()`
- [ ] 实现 `remapChunkLines()`
- [ ] 实现 targeted session sync

#### 完成标准

- session memory 不直接索引原始 JSON
- 搜索结果行号能回到原始 transcript `.jsonl`
- targeted sync 不会错误删除其它 transcript 数据

### 7.7 watcher 与 interval sync

#### 任务

- [ ] 实现 watcher
- [ ] 实现 session listener
- [ ] 实现 debounce
- [ ] 实现 onSearch 异步 sync
- [ ] 实现 onSessionStart 异步 warm
- [ ] 实现 interval sync

#### 完成标准

- 高频文件变化不会在 watcher 回调里直接做重活
- search 不因 sync 阻塞
- 同步 single-flight 语义稳定

### 7.8 batch embedding 与失败禁用

#### 任务

- [ ] 为支持 batch 的 provider 增加 batch 路径
- [ ] 维护 batch failure count
- [ ] 达阈值自动降级普通 embedding
- [ ] 把 batch 状态反映到 `status()`

#### 完成标准

- batch 失败不会让索引整体不可用
- provider 恢复后可重新回到 batch 路径

### 7.9 M2 验收

- [ ] SQLite 路径已覆盖 FTS-only / vector fallback / safe reindex / readonly recovery / session sync
- [ ] `status()` 可以区分 `hybrid` 与 `fts-only`
- [ ] 关键异常路径有单元测试或集成测试

---

## 8. M3 QMD 基础设施

目标：先把 QMD 路径依赖的运行时和只读能力搭好，不急着一口气做完整搜索。

### 8.1 QMD backend 配置与入口

#### 任务

- [ ] 在 `MemoryBackendSelector` 中补齐 QMD 配置解析
- [ ] 定义 `ResolvedQmdConfig`
- [ ] 定义 collections / sessions / update / limits / mcporter 解析规则
- [ ] 定义 qmd manager cache key 规则

#### 完成标准

- 当 backend=qmd 时，factory 能得到完整 resolved config
- config 已足够驱动 runtime / collections / search / update 初始化

### 8.2 QmdRuntimeContext

#### 任务

- [ ] 计算 workspace/state/agentState/qmdDir/xdg 路径
- [ ] 构造 QMD 所需 env
- [ ] 处理 shared models symlink
- [ ] 定义 agent scope 的 collection 名称规范化规则

#### 完成标准

- Java 版能稳定生成与 agent 绑定的 QMD 运行时目录
- 多 agent 不会共用同一个 XDG config/cache

### 8.3 CLI 与进程调用层

#### 任务

- [ ] 实现 `QmdCliClient`
- [ ] 实现 `resolveCliSpawnInvocation`
- [ ] 处理 timeout / output cap / discard stdout
- [ ] 处理 cwd/env 约束

#### 完成标准

- 能稳定执行 `qmd collection list/add/remove`、`qmd query`、`qmd update`
- 高输出命令不会因 stdout 过大把进程调用层打爆

### 8.4 collection 管理基础能力

#### 任务

- [ ] 实现 `QmdCollectionRegistry.bootstrapCollections()`
- [ ] 实现 `listCollectionsBestEffort()`
- [ ] 实现 `addCollection()` / `removeCollection()`
- [ ] 实现 `migrateLegacyUnscopedCollections()`

#### 完成标准

- 期望 collections 能被注册进 QMD
- 老版本未带 scope 的 collections 能被安全迁移
- 老版本不支持 `collection list --json` 时仍可 best-effort 工作

### 8.5 只读状态与 docid 基础能力

#### 任务

- [ ] 实现 `QmdIndexReader`
- [ ] 实现 `QmdStatusReader`
- [ ] 实现 `QmdDocPathResolver` 的最小版本

#### 完成标准

- `status()` 可返回 QMD documents 统计
- 能通过 `docid` / hints 解析到真实文件路径

### 8.6 M3 验收

- [ ] 不跑搜索也能完成 QMD runtime 初始化
- [ ] `status()` 可返回 qmd backend 运行时信息
- [ ] collections bootstrap 能稳定执行

---

## 9. M4 QMD 搜索链路

目标：补齐 QMD 的对外搜索能力。

### 9.1 scope 与请求入口

#### 任务

- [ ] 实现 `QmdScopePolicy`
- [ ] 统一 sessionKey normalize 规则
- [ ] 过滤 `subagent:` session
- [ ] deny 时记录可诊断日志

#### 完成标准

- 不允许访问的会话直接返回空结果
- scope 行为在 direct/group/channel 三类会话上可测试

### 9.2 query parser

#### 任务

- [ ] 实现 `QmdQueryParser.parseQueryJson()`
- [ ] 兼容 noisy stdout
- [ ] 兼容 `No results found`
- [ ] 生成稳定 stderr 摘要

#### 完成标准

- 不同 qmd 版本的 JSON 噪音都可被解析
- “无结果”不会被错误当成异常

### 9.3 单 collection 搜索

#### 任务

- [ ] 实现 CLI 单 collection 搜索
- [ ] 实现 `buildSearchArgs()`
- [ ] 实现 `buildCollectionFilterArgs()`
- [ ] 实现 `waitForPendingUpdateBeforeSearch()`

#### 完成标准

- 最多短暂等待 pending update，然后继续搜索旧索引
- 可在单 collection 场景下返回可用结果

### 9.4 fallback to `qmd query`

#### 任务

- [ ] 识别 `unknown flag|unknown option|unknown argument` 等错误
- [ ] 在 `search` / `vsearch` 不兼容时回退 `query`
- [ ] 对 fallback 失败路径保留诊断日志

#### 完成标准

- 新旧 qmd 版本差异不会直接打断搜索链路

### 9.5 多 collection 聚合

#### 任务

- [ ] 实现 `runQueryAcrossCollections()`
- [ ] 实现 `runMcporterAcrossCollections()`
- [ ] 统一 `docid` 去重与最高分保留
- [ ] 明确结果顺序与 limit 截断点

#### 完成标准

- 多 collection 不依赖 qmd 自带聚合
- 聚合后结果分数与去重语义稳定

### 9.6 mcporter 路径

#### 任务

- [ ] 实现 `QmdMcporterClient`
- [ ] 实现 daemon 启动策略
- [ ] 实现 `runQmdSearchViaMcporter()`
- [ ] 补齐冷启动 warn 语义

#### 完成标准

- mcporter 打开时单 collection 与多 collection 都可工作
- `startDaemon=false` 时不会偷偷起 daemon，但会有明确 warn

### 9.7 结果映射与预算控制

#### 任务

- [ ] 用 `QmdDocPathResolver` 把结果映射回真实路径
- [ ] 解析 snippet header 行号
- [ ] 决定 session 行号是否回到原始 transcript
- [ ] 实现 `diversifyResultsBySource()`
- [ ] 实现 `clampResultsByInjectedChars()`

#### 完成标准

- 返回结果包含 path/startLine/endLine/score/snippet/source
- memory 与 sessions 结果不会长期被某一来源淹没
- 注入预算有明确上限

### 9.8 missing collection repair

#### 任务

- [ ] 实现 `isMissingCollectionSearchError()`
- [ ] 实现 `tryRepairMissingCollectionSearch()`
- [ ] 限制 repair 只重试一次

#### 完成标准

- 缺失 collection 时能自动修复并重试一次
- 不会进入无限 repair 循环

### 9.9 M4 验收

- [ ] QMD 搜索支持 CLI 与 mcporter 两条技术路径
- [ ] 支持多 collection
- [ ] 支持 `qmd query` 自动降级
- [ ] 返回结果路径和行号语义清晰且可测试

---

## 10. M5 QMD 更新链路

目标：补齐 update/embed/export/repair，形成可长期运行的 QMD 后端。

### 10.1 update single-flight 与 force queue

#### 任务

- [ ] 实现 `pendingUpdate`
- [ ] 实现 `queuedForcedUpdate`
- [ ] 实现 `enqueueForcedUpdate()`
- [ ] 实现 `drainForcedUpdates()`
- [ ] 实现 debounce skip

#### 完成标准

- 普通 update 可复用在途任务
- force update 不丢失、不覆盖、可串行排队执行

### 10.2 boot / interval 行为

#### 任务

- [ ] 实现 `initialize(mode)`
- [ ] 实现 `onBoot` / `waitForBootSync`
- [ ] 实现 interval update timer
- [ ] 保留 `mode=status` 轻量初始化

#### 完成标准

- `purpose=status` 不会偷偷执行 update
- boot / interval 行为可配置且语义稳定

### 10.3 embed 与 backoff

#### 任务

- [ ] 实现 `shouldRunEmbed()`
- [ ] 实现 `noteEmbedFailure()`
- [ ] 实现 embed backoff
- [ ] 明确 embed 锁作用域

#### 完成标准

- `qmd embed` 失败不导致整个 update 路径失败
- backoff 生效，且恢复后可继续 embed

### 10.4 session export

#### 任务

- [ ] 实现 `exportSessions()`
- [ ] 复用 `buildSessionEntry()` 逻辑
- [ ] 实现增量导出
- [ ] 实现 retention 清理
- [ ] 如需要，补充 sidecar / marker 持久化行号映射

#### 完成标准

- session 导出不是每次全量重写
- stale markdown 可被清理
- 行号语义与事先拍板方案一致

### 10.5 repair service

#### 任务

- [ ] 实现 `QmdRepairService`
- [ ] 识别 null byte collection error
- [ ] 识别 duplicate document constraint error
- [ ] 实现 rebuild managed collections for repair
- [ ] 限制 repair 次数

#### 完成标准

- repair 命中时能自动恢复一次
- 未命中时不做无意义 destructive 重建

### 10.6 M5 验收

- [ ] QMD 后端可持续运行并自动 update
- [ ] force/update/embed 队列语义稳定
- [ ] session export / repair / backoff 都有测试覆盖

---

## 11. M6 统一降级与测试收口

目标：把两条后端链路收敛成统一可交付能力。

### 11.1 qmd -> builtin fallback

#### 任务

- [ ] 实现 `FallbackMemorySearchManager`
- [ ] primary 失败时标记 `primaryFailed`
- [ ] 关闭坏掉的 QMD manager
- [ ] 驱逐 cache entry
- [ ] 懒加载 builtin fallback
- [ ] 保证这些步骤并发安全

#### 完成标准

- 同一个坏 QMD 实例不会被后续请求继续命中
- cache 驱逐只发生一次
- fallback 也只初始化一次

### 11.2 status 一致性

#### 任务

- [ ] 确保 builtin 与 qmd 共享顶层 `MemoryProviderStatus`
- [ ] fallback 发生后注入 `fallback: { from: "qmd", reason }`
- [ ] 不伪造不存在的能力字段

#### 完成标准

- 调用方不需要按 backend 写两套状态消费逻辑

### 11.3 测试矩阵

#### 单元测试

- [ ] DTO / config resolver
- [ ] backend selector / factory cache
- [ ] query parser / scope policy
- [ ] result merger / line remap
- [ ] repair error classifier

#### 集成测试

- [ ] SQLite: sync -> search -> readFile -> status
- [ ] SQLite: FTS-only / vector fallback / readonly recovery / safe reindex
- [ ] QMD: collection bootstrap -> query -> doc resolve -> status
- [ ] QMD: search fallback to `qmd query`
- [ ] QMD: update queue / force queue / embed backoff / session export
- [ ] Fallback: qmd runtime failure -> builtin takeover

#### 手工验证

- [ ] 大 workspace 下索引耗时可接受
- [ ] session memory 引用行号正确
- [ ] backend 切换后状态可解释

### 11.4 M6 验收

- [ ] builtin 与 qmd 在统一 API 下都可工作
- [ ] qmd 运行失败可自动切换 builtin
- [ ] 关键降级路径都有测试证据
- [ ] 文档、实现、测试三者一致

---

## 12. 推荐并行化方式

如果团队里有 2 到 3 人，可以按下面方式并行，但前提是 M0 先完成。

### 路线 A

- 一人主做 SQLite 存储与同步：schema / repository / sync / safe reindex
- 一人主做 SQLite 查询与 provider：query / vector / FTS-only / provider factory
- 一人主做 QMD 基础设施：runtime / CLI / collections / parser / status

### 路线 B

- 等 SQLite 达到 M2 后，再由另一人切入 QMD 搜索与 update 链路
- fallback 与统一状态结构放到最后整合

### 不建议并行的部分

以下内容不建议在语义未拍板前并行编码：

- session 行号语义
- `MemoryProviderStatus` 最终结构
- factory cache / pending future 语义
- QMD embed 锁作用域

---

## 13. 最终交付定义

当以下条件全部满足时，可以认为 Java 版 MemorySearch 首个可交付版本完成：

- [ ] `MemorySearchManager` 统一接口稳定
- [ ] SQLite 路径具备可生产使用的 search/sync/status/readFile 能力
- [ ] SQLite 已补齐 safe reindex、readonly recovery、FTS-only、vector fallback、session sync
- [ ] QMD 路径具备可生产使用的 search/sync/status/readFile 能力
- [ ] QMD 已补齐 collection bootstrap、repair、update queue、embed backoff、session export、fallback to builtin
- [ ] builtin 与 qmd 的状态结构、降级语义、错误分类可以被统一消费
- [ ] 关键路径有单元测试与集成测试证据

---

## 14. 建议阅读顺序

真正开始编码前，建议按这个顺序阅读现有材料：

1. `docs/design/memory-architecture.md`
2. `docs/design/memory-sqllite-impl.md`
3. `docs/design/memory-qmd-impl.md`
4. `src/memory/search-manager.ts`
5. `src/memory/manager.ts`
6. `src/memory/manager-search.ts`
7. `src/memory/qmd-manager.ts`
8. `src/memory/session-files.ts`

这样可以先建立统一接口视角，再看 builtin，再看 qmd，避免一开始就陷入某条 backend 的细节里。
