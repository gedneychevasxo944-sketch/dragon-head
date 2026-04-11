# Prompt 管理规范

## 核心原则

所有 Prompt 的定义、注册和读取，必须通过 `org.dragon.config` 包下的组件完成，**禁止在业务代码中硬编码 prompt 字符串**。

## 核心组件

### 1.1 Prompt Key 定义 → PromptKeys

**文件路径：** `org.dragon.config.PromptKeys`

- 所有 prompt key 必须以公共静态常量的形式定义在 `PromptKeys` 类中
- 命名规则：`模块名.功能名`，使用小驼峰
- 新增模块时，在 PromptKeys 中新建一个注释区块，并为每个 key 添加 Javadoc 说明

```java
// 正确示例
public static final String MY_MODULE_ACTION = "myModule.action";
```

```java
// 错误示例 —— 禁止在业务代码中直接写字符串
configApplication.getGlobalPrompt("my hardcoded prompt key", defaultVal);
```

### 1.2 Prompt 初始化 → PromptInitializer

**文件路径：** `org.dragon.config.PromptInitializer`

- 所有全局默认 prompt 必须在 `PromptInitializer#run()` 中注册，按模块分成独立的 `initXxxPrompts()` 私有方法
- 较长的 prompt 内容优先从 `src/main/resources/prompts/` 目录下的 `.txt` 文件加载（使用 `loadPromptFromFile()`），避免在 Java 代码中写大段文本
- 简短的 prompt（1~2句）可以直接内联在 `initXxxPrompts()` 方法中

```java
// 正确示例 —— 长 prompt 走文件
String myPrompt = loadPromptFromFile("prompts/my-module-action-prompt.txt");
if (myPrompt != null) {
    configApplication.setGlobalPrompt(PromptKeys.MY_MODULE_ACTION, myPrompt);
}

// 正确示例 —— 短 prompt 内联
configApplication.setGlobalPrompt(PromptKeys.MY_MODULE_ACTION, "请完成以下任务：");
```

### 1.3 Prompt 使用 → ConfigApplication

**文件路径：** `org.dragon.config.service.ConfigApplication`

- 业务代码中获取 prompt 必须调用 ConfigApplication 的方法
- 优先级由高到低：`Character > Workspace > Global`
- 根据上下文选择合适的方法

```java
// 使用 InheritanceContext 获取 prompt
InheritanceContext context = InheritanceContext.forCharacter(null, workspaceId, characterId);
String prompt = configApplication.getPrompt(PromptKeys.XXX, context);

// 获取 Workspace 级别 prompt
configApplication.getWorkspacePrompt(workspaceId, PromptKeys.XXX, defaultVal);

// 获取全局 prompt（最低优先级）
configApplication.getGlobalPrompt(PromptKeys.XXX, defaultVal);
```

**禁止绕过 ConfigApplication 直接操作 ConfigStore 读写 prompt。**

## Prompt 文件放置

Prompt 文本文件统一放在 `src/main/resources/prompts/` 目录下：

```
src/main/resources/prompts/
├── observer-suggestion-prompt.txt
├── observer-personality-enhancement-prompt.txt
├── member-selector-select-prompt.txt
├── project-manager-decompose-prompt.txt
└── character-collaboration-decision-prompt.txt
```

命名规范：`{模块}-{功能}-prompt.txt`，使用小写和连字符。

## 新增 Prompt 步骤

1. 在 `PromptKeys` 中定义常量（带 Javadoc）
2. 在 `PromptInitializer` 中添加 `init{Module}Prompts()` 方法
3. 创建对应的 `prompts/{module}-{action}-prompt.txt` 文件（如需长 prompt）
4. 在业务代码中通过 `ConfigApplication` 获取使用

## Prompt 物料上下文

### PromptMaterialContext

**文件路径：** `org.dragon.agent.react.context.PromptMaterialContext`

统一物料上下文，存放所有 Prompt 生成相关物料，避免散落在多处：

```java
PromptMaterialContext
├── workspacePersonality        ← Workspace人格
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

### PromptMaterialContextBuilder

**文件路径：** `org.dragon.agent.react.context.PromptMaterialContextBuilder`

收集并填充 `PromptMaterialContext` 的各个部分：

```java
// 构建 ReAct 执行所需的上下文
PromptMaterialContext ctx = builder.buildForReAct(
    workspaceId, characterId, task, mind, activeSkills, reActConfig);

// 构建成员选择所需的上下文
PromptMaterialContext ctx = builder.buildForMemberSelection(
    workspaceId, parentTask, members);

// 构建任务分解所需的上下文
PromptMaterialContext ctx = builder.buildForTaskDecomposition(
    workspaceId, parentTask, members);
```

### PromptMaterialConfig

**文件路径：** `org.dragon.agent.react.context.PromptMaterialConfig`

控制各部分是否包含在 `PromptMaterialContext` 中：

```java
// 通过配置文件控制（dragon.prompt-material.*）
// 或使用预设配置
PromptMaterialConfig.defaultReAct();        // ReAct 执行用
PromptMaterialConfig.memberSelection();     // 成员选择用
PromptMaterialConfig.taskDecomposition();   // 任务分解用
PromptMaterialConfig.observerSuggestion(); // Observer建议用
```

对应的配置属性：`org.dragon.config.config.PromptMaterialConfigProperties`

### 调用时机

`CharacterExecutor.runReAct()` 在构建 `ReActContext` 前调用 `PromptMaterialContextBuilder`：

```java
PromptMaterialContext ctx = buildPromptMaterialContext(workspace, task);
String systemPrompt = resolveSystemPrompt(ctx);
// ctx 被放入 ReActContext，在 ThoughtPromptAssembler 中使用
```

## Prompt 分类

| 类型 | 来源 | 用途 |
|------|------|------|
| **System Prompt** | `CharacterExecutor.resolveSystemPrompt()` | Character 人格 + Workspace Personality + Skill 目录 |
| **Thought Prompt** | `ThoughtPromptAssembler` / `PromptWriter` | 用户输入 + 任务描述 + 历史 + 工具列表 |
| **Tool Prompt** | `ToolRegistry` | 工具定义和参数 schema |

## System Prompt 组装顺序

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
