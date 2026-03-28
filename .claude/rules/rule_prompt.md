# Prompt 管理规范

## 核心原则

所有 Prompt 的定义、注册和读取，必须通过 `org.dragon.config` 包下的三个核心类完成，**禁止在业务代码中硬编码 prompt 字符串**。

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
promptManager.getGlobalPrompt("my hardcoded prompt key", defaultVal);
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
    promptManager.setGlobalPrompt(PromptKeys.MY_MODULE_ACTION, myPrompt);
}

// 正确示例 —— 短 prompt 内联
promptManager.setGlobalPrompt(PromptKeys.MY_MODULE_ACTION, "请完成以下任务：");
```

### 1.3 Prompt 使用 → PromptManager

**文件路径：** `org.dragon.config.PromptManager`

- 业务代码中获取 prompt 必须调用 PromptManager 的方法
- 优先级由高到低：`Character > Organization > Workspace > Global`
- 根据上下文选择合适的重载方法

```java
// 获取 Character 级别 prompt（最高优先级）
promptManager.getCharacterPrompt(workspaceId, characterId, PromptKeys.XXX, defaultVal);

// 获取 Workspace 级别 prompt
promptManager.getWorkspacePrompt(workspaceId, PromptKeys.XXX, defaultVal);

// 获取全局 prompt（最低优先级）
promptManager.getGlobalPrompt(PromptKeys.XXX, defaultVal);
```

**禁止绕过 PromptManager 直接操作 ConfigStore 读写 prompt。**

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
4. 在业务代码中通过 `PromptManager` 获取使用
