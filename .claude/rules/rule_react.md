# ReAct 执行规范

## 核心原则

ReAct 流程的扩展和定制通过 `ReActContext` 传参完成，**不修改 ReActExecutor 的核心循环逻辑**。

## 核心组件

### ReActContext

**文件路径：** `org.dragon.agent.react.context.ReActContext`

- 用于传递 ReAct 任务的配置参数
- 包含迭代上限、终止条件等参数

### ReActExecutor

**文件路径：** `org.dragon.agent.react.ReActExecutor`

- 核心执行器，包含 ReAct 循环逻辑
- **禁止修改核心循环逻辑**

### ToolRegistry

**文件路径：** `org.dragon.tools.registry.ToolRegistry`

- Tool 注册通过 `ToolRegistry` 完成
- 遵循 `ToolConnector` 接口约定

## Prompt 使用

所有 ReAct 任务的 system prompt 必须使用 `PromptKeys` 常量：

```java
PromptKeys.REACT_EXECUTE
PromptKeys.REACT_TASK_DECOMPOSE
```

通过 `ConfigApplication` 获取：

```java
String systemPrompt = configApplication.getWorkspacePrompt(
    workspaceId, PromptKeys.REACT_EXECUTE, defaultVal);
```

## 配置参数

迭代上限、终止条件等参数通过 `TerminationConfig` 配置，**不在代码中写魔法数字**：

```java
// 正确 —— 通过配置获取
TerminationConfig config = context.getTerminationConfig();
int maxIterations = config.getMaxIterations();

// 错误 —— 硬编码数字
if (iteration > 10) { ... }
```

## 已有配置示例

| 配置 | 说明 |
|------|------|
| `TerminationConfig` | 迭代终止条件配置 |
| `ReActContext` | ReAct 执行上下文 |
| `ToolRegistry` | 工具注册中心 |
