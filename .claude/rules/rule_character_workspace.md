# Character 与 Workspace 规范

## Character 规范

### 数据实体

- `Character` 为数据实体，使用 Lombok `@Builder` 构建
- **不在 Character 内部直接 new 依赖对象**
- 所有执行器（`ReActExecutor`、`WorkflowExecutor`、`ConfigApplication`、`ModelRegistry`）由外部注入

```java
// 正确示例
@Data
public class Character {
    private CharacterProfile profile;
    private CharacterExecutorConfig executorConfig;
    private CharacterRuntime runtime;
    private transient CharacterExecutor executor;  // 惰性初始化
}
```

### Prompt 调用

如果涉及自定义 Prompt，使用 `CharacterCaller` 进行调用：

```java
@Service
public class XxxService {
    private final CharacterCaller characterCaller;

    public void doSomething() {
        String result = characterCaller.call(character, userMessage);
    }
}
```

## Workspace 规范

### 业务入口

- Workspace 级别的业务逻辑入口**统一走 `WorkspaceService`**
- **不直接调用 `WorkspaceRegistry` 或 `WorkspaceScheduler`**

```java
// 正确示例
@Autowired
private WorkspaceService workspaceService;

public void doSomething(String workspaceId) {
    workspaceService.createWorkspace(workspace);
}

// 错误示例 —— 绕过 WorkspaceService
workspaceRegistry.create(workspace);
```

## Channel 适配器规范

- Channel 适配器新增时，**实现 `ChannelAdapter` 接口**
- 注册到 `ChannelManager`
- **禁止在 `WorkspaceService` 中直接引用具体 Channel 实现**

```java
// 正确示例
@Component
public class MyChannelAdapter implements ChannelAdapter {
    // 实现接口方法
}

@Autowired
private ChannelManager channelManager;

// 在 ChannelManager 中注册
channelManager.registerAdapter(new MyChannelAdapter());
```

## 模块职责划分

| 模块 | 职责 | 禁止 |
|------|------|------|
| `Character` | AI 智能体数据实体 | 内部创建执行器 |
| `Workspace` | 工作空间业务入口 | 直接调用 Registry |
| `WorkspaceService` | Workspace 级别业务逻辑统一入口 | 绕过直接调用 |
| `ChannelManager` | 渠道管理 | 直接引用具体 Adapter |
