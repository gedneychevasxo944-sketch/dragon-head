# Dragon Head 项目规范

## 编码规范

编写代码时必须遵守 `.claude/rules/` 下的所有规范文件：

| 规范文件 | 内容 |
|---------|------|
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
