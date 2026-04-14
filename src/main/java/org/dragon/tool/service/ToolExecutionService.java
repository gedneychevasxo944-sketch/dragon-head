package org.dragon.tool.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.domain.ToolExecutionRecordDO;
import org.dragon.tool.runtime.PermissionResult;
import org.dragon.tool.runtime.Tool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolRegistry;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.dragon.tool.runtime.adapter.ToolCallRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 工具运行时执行服务（Agent 热路径唯一入口）。
 *
 * <p>对应 TypeScript 版本的 {@code src/services/tools/toolExecution.ts}。
 *
 * <p><b>职责</b>（运行时，非管理面）：
 * <p>{@link #runToolUse} — Agent 调用工具的唯一入口，支持所有 ToolType，完整生命周期管理：
 * 可见性检查 → 执行记录 → 权限检查 → tool.call() → 结果处理 → MessageUpdate
 *
 * <h3>调用流程：</h3>
 * <pre>
 * Agent 主循环
 *   │
 *   ├─▶ ToolRegistry.buildToolDeclarations()   每轮对话前构建注入 LLM 的工具列表
 *   │
 *   └─▶ runToolUse()                            LLM 返回 tool_call 后统一入口
 *         │
 *         ├─ 1. 可见性检查（ToolRegistry.findByName → Tool 实例）
 *         ├─ 2. 创建执行记录（ToolExecutionRecordService）
 *         ├─ 3. 权限检查（ToolUseContext 规则）
 *         ├─ 4. tool.call(params, context, progress) → ToolResult
 *         └─ 5. 结果处理（大结果落存 + 写执行记录 + 包装 MessageUpdate）
 * </pre>
 *
 * <h3>与 Storage 的关系：</h3>
 * <pre>
 * ToolExecutionService（主链路，唯一落存入口）
 *   ├─▶ ToolFileService.storeResult  大结果内容 → 本地文件 / S3
 *   └─▶ ToolExecutionRecordService  执行元数据 → MySQL（仅写记录，不存实际内容）
 * </pre>
 */
@Slf4j
@Service
public class ToolExecutionService {

    /** 大结果阈值（字符数），超过此值则落存外部存储 */
    private static final long LARGE_RESULT_THRESHOLD = 50_000L;

    /** 预览字符数 */
    private static final int PREVIEW_SIZE_CHARS = 2000;

    private final ToolExecutionRecordService executionRecordService;
    private final ToolFileService toolFileService;
    private final ToolRegistry toolRegistry;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── 构造函数 ─────────────────────────────────────────────────────────

    /**
     * 构造函数（Spring 注入）。
     */
    @Autowired
    public ToolExecutionService(ToolExecutionRecordService executionRecordService,
                                ToolFileService toolFileService,
                                ToolRegistry toolRegistry) {
        this.executionRecordService = executionRecordService;
        this.toolFileService = toolFileService;
        this.toolRegistry = toolRegistry;
        this.executorService = Executors.newCachedThreadPool();
        log.info("[ToolExecutionService] 初始化完成");
    }

    // ── 统一工具调用（唯一入口）──────────────────────────────────────────

    /**
     * 工具调用统一入口（支持所有 ToolType）。
     *
     * <p>完整执行流程：
     * <ol>
     *   <li>abort 检查：上下文已中止则直接返回停止消息</li>
     *   <li>可见性检查：通过 {@link ToolRegistry#findByName} 获取 Tool 实例（不存在则不可见）</li>
     *   <li>创建执行记录（ToolExecutionRecordService）</li>
     *   <li>权限检查（基于 ToolUseContext 规则）</li>
     *   <li>调用 {@link Tool#call} 执行工具</li>
     *   <li>结果处理：大结果落存 + 写执行记录 + 包装为 {@link MessageUpdate} 返回</li>
     * </ol>
     *
     * @param request          已从 LLM 原始格式解析的 tool_call 请求
     * @param characterId      当前执行的 Character ID
     * @param workspaceId      Character 所属的 Workspace ID
     * @param toolUseContext   工具执行上下文（session、abort 等）
     * @param progressConsumer 进度回调（可为 null）
     * @return 异步 MessageUpdate，包含 tool_result 内容块
     */
    public CompletableFuture<MessageUpdate> runToolUse(ToolCallRequest request,
                                                       String characterId,
                                                       String workspaceId,
                                                       ToolUseContext toolUseContext,
                                                       Consumer<ToolProgress> progressConsumer) {
        String toolName = request.getToolName();
        String toolUseId = request.getToolCallId();

        log.info("[ToolExecution] 开始执行工具: name={}, toolUseId={}", toolName, toolUseId);

        // 1. abort 检查
        if (toolUseContext.isAborted()) {
            log.info("[ToolExecution] 工具调用已取消: name={}", toolName);
            return CompletableFuture.completedFuture(createStopMessage(toolUseId));
        }

        // 2. 可见性检查：通过 ToolRegistry 查找 Tool 实例（命中缓存，含别名支持）
        Optional<Tool<JsonNode, ?>> toolOpt = toolRegistry.findByName(characterId, workspaceId, toolName);

        if (toolOpt.isEmpty()) {
            log.warn("[ToolExecution] 工具不可见（未注册或未绑定）: name={}, characterId={}, workspaceId={}",
                    toolName, characterId, workspaceId);
            return CompletableFuture.completedFuture(
                    createErrorMessage(toolUseId,
                            "Tool '" + toolName + "' is not accessible in the current context."));
        }

        Tool<JsonNode, ?> tool = toolOpt.get();

        // 3. 创建执行记录
        ToolExecutionRecordDO executionRecord = executionRecordService.startExecution(
                toolName,
                toolUseContext.getTenantId(),
                toolUseContext.getSessionId(),
                toolUseId,
                toInputMap(request.getParameters())
        );

        return CompletableFuture.supplyAsync(() ->
                        executeWithLifecycle(tool, request, toolUseContext, progressConsumer, executionRecord),
                executorService);
    }

    /**
     * 执行工具并管理完整生命周期（在异步线程中运行）。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private MessageUpdate executeWithLifecycle(Tool<JsonNode, ?> tool,
                                               ToolCallRequest request,
                                               ToolUseContext toolUseContext,
                                               Consumer<ToolProgress> progressConsumer,
                                               ToolExecutionRecordDO executionRecord) {
        String toolName = request.getToolName();
        String toolUseId = request.getToolCallId();

        try {
            // 4. 权限检查
            PermissionResult permission = checkPermissions(toolName, request.getParameters(), toolUseContext);

            executionRecordService.recordPermission(
                    executionRecord.getExecutionId(), permission, "context");

            if (!permission.isAllow()) {
                log.warn("[ToolExecution] 权限检查失败: name={}, behavior={}",
                        toolName, permission.getBehavior());
                return createPermissionDeniedMessage(toolUseId, permission);
            }

            // 5. 记录执行开始
            executionRecordService.appendEvent(executionRecord.getExecutionId(),
                    "start", "开始执行工具: " + toolName, null);

            // 6. 解析输入并调用 tool.call()
            //    parseInput 将 JsonNode 转为 Tool 自身的强类型输入（AbstractTool 已提供默认实现）
            JsonNode rawParams = request.getParameters();
            JsonNode input = tool.parseInput(rawParams);

            ToolResult<?> result = tool.call(input, toolUseContext, progressConsumer).get();

            // 7. 对 ATOMIC 工具：若 resultBlock 尚未填充，调用 mapToolResultToToolResultBlockParam 补充
            //    HTTP/MCP/CODE/SKILL 等包装工具的 doCall 已直接返回带 resultBlock 的 ToolResult
            result = fillResultBlockIfAbsent((Tool) tool, result, toolUseId);

            // 8. 处理结果
            return processResult(result, toolUseId, toolName, toolUseContext, permission, executionRecord);

        } catch (Exception e) {
            log.error("[ToolExecution] 工具执行异常: name={}, error={}", toolName, e.getMessage(), e);
            executionRecordService.completeWithError(executionRecord.getExecutionId(), e, isRetryable(e));
            return createErrorMessage(toolUseId,
                    "Error executing tool '" + toolName + "': " + e.getMessage());
        }
    }

    /**
     * 若 ToolResult 缺少 resultBlock（ATOMIC 工具 doCall 仅返回 ok(data) 不含 block），
     * 调用 {@link Tool#mapToolResultToToolResultBlockParam} 补充。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ToolResult<?> fillResultBlockIfAbsent(Tool tool, ToolResult<?> result, String toolUseId) {
        if (!result.isSuccess() || result.getResultBlock() != null) {
            return result;
        }
        try {
            ToolResultBlockParam block = tool.mapToolResultToToolResultBlockParam(result.getData(), toolUseId);
            return ToolResult.ok(result.getData(), block, result.getNewMessages(), result.getContextModifier());
        } catch (Exception e) {
            log.warn("[ToolExecution] mapToolResultToToolResultBlockParam 失败，fallback 到序列化: error={}",
                    e.getMessage());
            String fallback = serializeResultData(result.getData());
            return ToolResult.ok(result.getData(), ToolResultBlockParam.ofText(toolUseId, fallback),
                    result.getNewMessages(), result.getContextModifier());
        }
    }

    /**
     * 处理工具执行结果：大结果落存 + 写执行记录 + 包装 MessageUpdate。
     */
    private MessageUpdate processResult(ToolResult<?> result,
                                        String toolUseId,
                                        String toolName,
                                        ToolUseContext context,
                                        PermissionResult permission,
                                        ToolExecutionRecordDO executionRecord) {
        if (!result.isSuccess()) {
            executionRecordService.completeWithError(
                    executionRecord.getExecutionId(),
                    new RuntimeException(result.getError()), false);
            return createErrorMessage(toolUseId, result.getError());
        }

        // 1. 获取已转换好的 resultBlock
        ToolResultBlockParam resultBlock = result.getResultBlock();

        // 2. 防御性 fallback（正常情况不会走到这里）
        if (resultBlock == null) {
            log.warn("[ToolExecution] resultBlock 为 null，fallback 到序列化: tool={}", toolName);
            resultBlock = ToolResultBlockParam.ofText(toolUseId, serializeResultData(result.getData()));
        }

        // 3. 大结果落存（主链路唯一落存点）
        ToolExecutionRecordDO.ResultStorageMeta storageMeta = null;
        if (resultBlock.getContent() instanceof String content) {
            if (content.length() > LARGE_RESULT_THRESHOLD) {
                try {
                    String storagePath = toolFileService.storeResult(
                            context.getSessionId(), toolUseId, content);
                    String preview = content.substring(0, Math.min(content.length(), PREVIEW_SIZE_CHARS));
                    storageMeta = ToolExecutionRecordDO.ResultStorageMeta.builder()
                            .storageType(toolFileService.getStorageType())
                            .storagePath(storagePath)
                            .contentSize(content.length())
                            .contentType("text/plain")
                            .preview(preview)
                            .hasMore(content.length() > PREVIEW_SIZE_CHARS)
                            .build();
                    resultBlock = ToolResultBlockParam.ofText(toolUseId, buildPreviewMessage(storagePath, content));
                    log.info("[ToolExecution] 大结果已落存: toolUseId={}, storagePath={}",
                            toolUseId, storagePath);
                } catch (Exception e) {
                    log.warn("[ToolExecution] 大结果落存失败，原样返回: toolUseId={}, error={}",
                            toolUseId, e.getMessage());
                }
            }
        }

        // 4. 写执行记录（仅元数据，storageMeta 可为 null）
        executionRecordService.completeWithResult(
                executionRecord.getExecutionId(), result.getData(), storageMeta);

        // 5. 构建内容块列表（主结果 + 权限反馈 + 额外块）
        List<Object> contentBlocks = new ArrayList<>();
        contentBlocks.add(resultBlock);
        if (permission.getAcceptFeedback() != null) {
            contentBlocks.add(Map.of("type", "text", "text", permission.getAcceptFeedback()));
        }
        if (permission.getContentBlocks() != null) {
            contentBlocks.addAll(permission.getContentBlocks());
        }

        // 6. 包装 MessageUpdate
        return new MessageUpdate(
                createUserMessage(contentBlocks, result.getData(), context),
                result.getContextModifier() != null
                        ? new ContextModifierUpdate(toolUseId, result.getContextModifier())
                        : null,
                result.getNewMessages()
        );
    }

    // ── 清理 ─────────────────────────────────────────────────────────────

    /**
     * 关闭线程池（应用停止时调用）。
     */
    public void shutdown() {
        executorService.shutdown();
    }

    // ── 权限检查 ─────────────────────────────────────────────────────────

    /**
     * 通用权限检查（基于 ToolUseContext 规则）。
     */
    private PermissionResult checkPermissions(String toolName,
                                               JsonNode params,
                                               ToolUseContext context) {
        if (context.isAborted()) {
            return PermissionResult.deny("Execution context has been aborted.");
        }
        // TODO: 规则匹配逻辑（对应 TS: canUseTool() → matchPermissionRules()）
        return PermissionResult.allow();
    }

    // ── 内部辅助方法 ─────────────────────────────────────────────────────

    /**
     * 构建大结果预览消息（落存后替换原始内容返回给 LLM）。
     */
    private String buildPreviewMessage(String storagePath, String content) {
        String preview = content.substring(0, Math.min(content.length(), PREVIEW_SIZE_CHARS));
        boolean hasMore = content.length() > PREVIEW_SIZE_CHARS;
        return "<persisted-output>\n"
                + "Output too large (" + content.length() + " chars). Full output saved to: " + storagePath + "\n\n"
                + "Preview (first " + PREVIEW_SIZE_CHARS + " chars):\n"
                + preview
                + (hasMore ? "\n...\n" : "\n")
                + "</persisted-output>";
    }

    private String serializeResultData(Object data) {
        if (data == null) return "";
        if (data instanceof String) return (String) data;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return data.toString();
        }
    }

    private Map<String, Object> createUserMessage(List<Object> contentBlocks,
                                                   Object toolUseResult,
                                                   ToolUseContext context) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", contentBlocks);
        if (context.getAgentId() == null || context.isPreserveToolUseResults()) {
            message.put("toolUseResult", toolUseResult);
        }
        return message;
    }

    private MessageUpdate createErrorMessage(String toolUseId, String errorMessage) {
        ToolResultBlockParam errorBlock = ToolResultBlockParam.ofError(toolUseId, errorMessage);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", List.of(errorBlock));
        message.put("toolUseResult", "Error: " + errorMessage);
        return new MessageUpdate(message, null, null);
    }

    private MessageUpdate createStopMessage(String toolUseId) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", List.of(Map.of(
                "type", "tool_result",
                "tool_use_id", toolUseId,
                "content", "[Tool execution cancelled]"
        )));
        return new MessageUpdate(message, null, null);
    }

    private MessageUpdate createPermissionDeniedMessage(String toolUseId, PermissionResult permission) {
        String msg = permission.getMessage() != null ? permission.getMessage() : "Permission denied";
        return createErrorMessage(toolUseId, msg);
    }

    private Map<String, Object> toInputMap(JsonNode input) {
        if (input == null || !input.isObject()) return Collections.emptyMap();
        Map<String, Object> map = new LinkedHashMap<>();
        input.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isTextual()) map.put(entry.getKey(), value.asText());
            else if (value.isNumber()) map.put(entry.getKey(), value.asDouble());
            else if (value.isBoolean()) map.put(entry.getKey(), value.asBoolean());
            else map.put(entry.getKey(), value.toString());
        });
        return map;
    }

    private boolean isRetryable(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        return message.contains("timeout") || message.contains("connection") || message.contains("temporary");
    }

    // ── 内部类型 ─────────────────────────────────────────────────────────

    /**
     * 工具执行结果更新（返回给 Agent 主循环）。
     */
    @Getter
    public static class MessageUpdate {
        private final Map<String, Object> message;
        private final ContextModifierUpdate contextModifier;
        private final List<Map<String, Object>> newMessages;

        public MessageUpdate(Map<String, Object> message,
                             ContextModifierUpdate contextModifier,
                             List<Map<String, Object>> newMessages) {
            this.message = message;
            this.contextModifier = contextModifier;
            this.newMessages = newMessages;
        }
    }

    /**
     * 上下文修改器更新（携带 toolUseId 便于追踪来源）。
     */
    @Getter
    public static class ContextModifierUpdate {
        private final String toolUseId;
        private final java.util.function.Function<ToolUseContext, ToolUseContext> modifier;

        public ContextModifierUpdate(String toolUseId,
                                     java.util.function.Function<ToolUseContext, ToolUseContext> modifier) {
            this.toolUseId = toolUseId;
            this.modifier = modifier;
        }
    }
}
