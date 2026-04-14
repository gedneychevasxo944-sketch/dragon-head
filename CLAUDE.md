# Dragon Head 项目规范

## 编码规范

编写代码时必须遵守 `.claude/rules/` 下的所有规范文件：

| 规范文件 | 内容 |
|---------|------|
| [rule_asset.md](.claude/rules/rule_asset.md) | Asset 资产模块规范 |
| [rule_prompt.md](.claude/rules/rule_prompt.md) | Prompt 管理规范 |
| [rule_config.md](.claude/rules/rule_config.md) | 配置管理规范 |
| [rule_llm.md](.claude/rules/rule_llm.md) | LLM 调用规范 |
| [rule_react.md](.claude/rules/rule_react.md) | ReAct 执行规范 |
| [rule_character_workspace.md](.claude/rules/rule_character_workspace.md) | Character 与 Workspace 规范 |
| [rule_store.md](.claude/rules/rule_store.md) | 存储层规范 |
| [rule_general.md](.claude/rules/rule_general.md) | 通用代码规范 |

## 快速检查清单

提交代码前，逐项确认：
- [ ] 没有在业务代码中硬编码 prompt 字符串
- [ ] 新 prompt key 已在 PromptKeys 中定义为常量
- [ ] 新 prompt 默认值已在 PromptInitializer 中注册
- [ ] 所有 LLM 调用走 LLMCaller 接口，未直接引用具体实现类
- [ ] ConfigStore 的读写使用了 ConfigKey 的语义工厂方法
- [ ] 存储层依赖注入的是接口而非 Memory 实现类
- [ ] 所有 public 方法有 Javadoc
- [ ] 日志格式包含类名前缀
- [ ] Owner/PublishStatus/Association 相关逻辑优先使用 Asset 模块服务，不重复实现
- [ ] 列表接口不在循环中单条查询关联数据（标签、状态等），改用批量查询
- [ ] 有值得变成常规编码规则的内容，写入Claude.md的规则文件

## Prompt 架构与调用链路

### Prompt 分类

| 类型 | 来源 | 用途 |
|------|------|------|
| **System Prompt** | `CharacterExecutor.resolveSystemPrompt()` | Character 人格 + Workspace Personality + Skill 目录 |
| **Thought Prompt** | `ThoughtPromptAssembler` / `PromptWriter` | 用户输入 + 任务描述 + 历史 + 工具列表 |
| **Tool Prompt** | `ToolRegistry` | 工具定义和参数 schema |

### 调用链路

```
用户请求
    │
    ▼
Character.run(userInput)
    │
    ▼
OrchestrationService.orchestrate()
    │
    ├─→ [WORKFLOW] → WorkflowExecutor
    │
    └─→ [REACT] → CharacterExecutor.runReAct()
                        │
                        ├─→ PromptMaterialContextBuilder.buildForReAct()
                        │       收集 WorkspacePersonality, Skills, Task 等
                        │
                        ├─→ resolveSystemPrompt()
                        │       生成 System Prompt
                        │
                        └─→ ReActContext (含 promptMaterialContext)
                                │
                                ▼
                        ReActExecutor.execute()
                                │
                ┌───────────────┼───────────────┐
                ▼               ▼               ▼
            think()          act()           observe()
            (LLM调用)    (执行Action)    (评估结果)
```

### System Prompt 组装

```
1. ConfigApplication.getPrompt(workspace, character, CHARACTER_SYSTEM)
   // 4级层次: workspace+char > char > workspace > global

2. Fallback: Mind.getPersonality().toPrompt()
   // traits, values, communicationStyle, decisionStyle

3. SkillDirectoryBuilder.buildDirectoryPrompt()
   // displayName, description, whenToUse, argumentHint

4. WorkspacePersonality (通过 PromptMaterialContext 注入)
   // workingStyle, decisionPattern, riskTolerance, coreValues
```

### PromptMaterialContext

统一物料上下文，存放所有 Prompt 生成相关物料：

```java
PromptMaterialContext
├── workspacePersonality        ← Workspace人格 (之前未注入)
├── availableMembers           ← 可用成员
├── teamPositions              ← 职位信息
├── characterPersonality       ← Character人格 (Mind)
├── availableSkills            ← Skill列表
├── skillDirectoryPrompt        ← 已组装Skill目录
├── taskId/Name/Description    ← 任务信息
├── recentEvaluation           ← 评估记录
├── recentMemories            ← Memory
└── collaborationContext       ← 协作上下文
```

通过 `PromptMaterialContextBuilder` 收集，通过 `PromptMaterialConfig` 控制各部分开关。

### 关键文件

| 文件 | 职责 |
|------|------|
| `PromptKeys.java` | 所有 Prompt Key 常量 |
| `PromptInitializer.java` | 启动时注册默认 Prompt |
| `ConfigApplication.java` | 4 级层次查询 |
| `CharacterExecutor.java` | System Prompt 生成入口 |
| `ReActExecutor.java` | ReAct 循环执行 |
| `ThoughtPromptAssembler.java` | Thought Prompt 组装 (Fallback) |
| `PromptMaterialContext.java` | 统一物料上下文 |
| `PromptMaterialContextBuilder.java` | 物料收集器 |
| `PromptMaterialConfig.java` | 物料开关配置 |

## 批量加载模式（避免 N+1 查询）

列表接口返回含关联数据（标签、状态等）的对象时，**禁止在循环或 stream 中逐条查询关联**，必须批量加载。

### 标准做法

**1. Store 层提供批量查询接口**

```java
// AssetAssociationStore
List<AssetAssociationEntity> findByTargets(
    AssociationType type, ResourceType targetType, List<String> targetIds);
```

**2. Service 层提供批量返回 Map 的方法**

```java
// AssetTagService
Map<String, List<AssetTagDTO>> getTagsForAssets(
    ResourceType resourceType, List<String> resourceIds);
// 返回 Map<resourceId, List<TagDTO>>
```

**3. 列表接口：先分页，再批量加载，最后转换**

```java
// 先取当前页数据
List<TraitEntity> pageTraits = allTraits.subList(fromIndex, toIndex);

// 批量加载关联（一次查询）
List<String> pageIds = pageTraits.stream().map(TraitEntity::getId).toList();
Map<String, List<AssetTagDTO>> tagsMap =
    assetTagService.getTagsForAssets(ResourceType.TRAIT, pageIds);

// 转换时使用预加载数据，不再触发 DB 查询
List<Map<String, Object>> result = pageTraits.stream()
    .map(t -> toMap(t, tagsMap.getOrDefault(t.getId(), List.of())))
    .toList();
```

**4. toMap 双重重载**

单条场景（创建/详情/更新）保留 `public toMap(entity)` 内部调用 `getTagsForAsset()`；
列表场景使用 `private toMap(entity, tags)` 接收预加载数据。

```java
// 单条：外部调用，内部懒加载
public Map<String, Object> toMap(TraitEntity trait) {
    List<AssetTagDTO> tags = assetTagService.getTagsForAsset(ResourceType.TRAIT, trait.getId());
    return toMap(trait, tags);
}

// 批量：接受预加载 tags，不触发额外查询
private Map<String, Object> toMap(TraitEntity trait, List<AssetTagDTO> tags) {
    // ... 直接使用 tags
}
```

### 典型文件参考

| 层 | 文件 | 方法 |
|----|------|------|
| Store 接口 | `AssetAssociationStore` | `findByTargets()` |
| Store 实现 | `MySqlAssetAssociationStore` | `.in("targetId", ids)` |
| Service | `AssetTagService` | `getTagsForAssets()` |
| 业务 Service | `TraitService` | `listTraits()` + `toMap(entity, tags)` |
