# LLM 调用规范

## 核心原则

所有 LLM 调用必须通过 `LLMCaller` 接口完成，**禁止在业务逻辑中直接实例化或调用具体的 LLM Caller 实现类**（如 `DeepseekLLMCaller`、`KimiLLMCaller`）。

## 核心接口

### LLMCaller

**文件路径：** `org.dragon.agent.model.LLMCaller`

```java
public interface LLMCaller {
    LLMResponse call(LLMRequest request);
    void streamCall(LLMRequest request, StreamCallback callback);
    CompletableFuture<LLMResponse> callAsync(LLMRequest request);
}
```

### ModelRegistry

**文件路径：** `org.dragon.agent.model.ModelRegistry`

- 通过 `ModelRegistry` 管理模型选择
- 优先使用 `LLMCaller#call(LLMRequest)` 发起调用

## 调用方式

```java
@Service
public class XxxService {
    private final LLMCaller llmCaller;
    private final PromptManager promptManager;

    public void doSomething() {
        // 构建 LLMRequest，system prompt 从 PromptManager 获取
        String systemPrompt = promptManager.getGlobalPrompt(PromptKeys.XXX, defaultVal);

        LLMRequest request = LLMRequest.builder()
                .model(modelId)
                .systemPrompt(systemPrompt)
                .userMessage("user message")
                .build();

        LLMResponse response = llmCaller.call(request);
    }
}
```

## 流式输出与异步

```java
// 流式输出
llmCaller.streamCall(request, new StreamCallback() {
    @Override
    public void onChunk(String chunk) {
        // 处理流式输出
    }
});

// 异步调用
CompletableFuture<LLMResponse> future = llmCaller.callAsync(request);
```

## 新增 LLM 提供商

1. 实现 `LLMCaller` 接口（如 `DeepseekLLMCaller`）
2. 在 `ModelRegistry` 中注册，不修改现有调用方
3. 使用 `@Component` 注解，自动注册为 Spring Bean

## 禁止事项

```java
// 错误 —— 直接实例化具体实现
DeepseekLLMCaller caller = new DeepseekLLMCaller();

// 错误 —— 直接调用具体实现
kimiCaller.call(request);
```
