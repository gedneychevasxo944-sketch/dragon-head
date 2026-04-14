package org.dragon.tool.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 抽象工具基类。
 *
 * <p>提供工具接口的通用实现，子类只需实现核心方法：
 * <ul>
 *   <li>{@link #doCall} - 实际的工具执行逻辑</li>
 *   <li>{@link #mapToolResultToToolResultBlockParam} - 结果转换</li>
 * </ul>
 *
 * <p>参考 TypeScript 版本的设计模式，基类提供：
 * <ul>
 *   <li>默认的权限检查（允许）</li>
 *   <li>默认的输入校验（通过）</li>
 *   <li>通用的 JSON Schema 构建</li>
 *   <li>进度报告包装</li>
 * </ul>
 *
 * @param <I> 输入参数类型
 * @param <O> 输出结果类型
 */
@Slf4j
@Getter
public abstract class AbstractTool<I, O> implements Tool<I, O> {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    private final String name;
    private final String description;
    private final Class<I> inputType;

    /**
     * 构造函数。
     *
     * @param name        工具名称
     * @param description 工具描述
     * @param inputType   输入参数类型
     */
    protected AbstractTool(String name, String description, Class<I> inputType) {
        this.name = name;
        this.description = description;
        this.inputType = inputType;
    }

    // ── 核心抽象方法 ─────────────────────────────────────────────────────

    /**
     * 子类实现的实际执行逻辑。
     *
     * <p>框架会在完成参数校验和权限检查后调用此方法。
     */
    protected abstract CompletableFuture<ToolResult<O>> doCall(I input,
                                                               ToolUseContext context,
                                                               Consumer<ToolProgress> progress);

    // ── Tool 接口实现 ─────────────────────────────────────────────────────

    @Override
    public CompletableFuture<ToolResult<O>> call(I input,
                                                  ToolUseContext context,
                                                  Consumer<ToolProgress> progress) {
        // 1. 检查是否已被取消
        if (context.isAborted()) {
            log.debug("[{}] 工具调用已取消: toolUseId={}", name, context.getToolUseId());
            return CompletableFuture.completedFuture(
                    ToolResult.fail("Tool execution was cancelled")
            );
        }

        // 2. 包装进度回调，添加工具名称日志
        Consumer<ToolProgress> wrappedProgress = p -> {
            log.trace("[{}] 进度更新: {}", name, p.getData().getType());
            if (progress != null) {
                progress.accept(p);
            }
        };

        // 3. 调用子类实现
        long startTime = System.currentTimeMillis();
        return doCall(input, context, wrappedProgress)
                .whenComplete((result, error) -> {
                    long duration = System.currentTimeMillis() - startTime;
                    if (error != null) {
                        log.error("[{}] 执行失败: duration={}ms, error={}",
                                name, duration, error.getMessage());
                    } else {
                        log.info("[{}] 执行完成: duration={}ms, success={}",
                                name, duration, result.isSuccess());
                    }
                });
    }

    @Override
    public ValidationResult validateInput(I input, ToolUseContext context) {
        // 默认通过所有输入
        return ValidationResult.ok();
    }

    @Override
    public PermissionResult checkPermissions(I input, ToolUseContext context) {
        // 默认允许所有操作
        return PermissionResult.allow();
    }

    @Override
    public boolean isConcurrencySafe(I input) {
        // 默认并发安全
        return true;
    }

    @Override
    public boolean isReadOnly(I input) {
        // 默认只读
        return true;
    }

    @Override
    public boolean isDestructive(I input) {
        // 默认非破坏性
        return false;
    }

    @Override
    public InterruptBehavior interruptBehavior() {
        // 默认阻塞等待
        return InterruptBehavior.BLOCK;
    }

    @Override
    public long getMaxResultSizeChars() {
        // 默认 50K 字符
        return 50_000;
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public String getSearchHint() {
        return null;
    }

    @Override
    public boolean requiresUserInteraction() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getUserFacingName(I input) {
        return name;
    }

    @Override
    public String getToolUseSummary(I input) {
        return null;
    }

    @Override
    public String getActivityDescription(I input) {
        return "Executing " + name;
    }

    // ── 输入解析 ─────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public I parseInput(JsonNode input) {
        if (inputType.isAssignableFrom(JsonNode.class)) {
            return (I) input;
        }
        if (inputType.isAssignableFrom(Map.class)) {
            return (I) objectMapper.convertValue(input, Map.class);
        }
        return objectMapper.convertValue(input, inputType);
    }

    @Override
    public void backfillObservableInput(Map<String, Object> input) {
        // 默认无操作
    }

    // ── 辅助方法 ─────────────────────────────────────────────────────────

    /**
     * 快速创建进度报告。
     */
    protected ToolProgress progress(String toolUseId, ToolProgress.ToolProgressData data) {
        return ToolProgress.builder()
                .toolUseId(toolUseId)
                .data(data)
                .build();
    }

}
