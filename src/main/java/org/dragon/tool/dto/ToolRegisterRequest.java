package org.dragon.tool.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * 注册/更新 Tool 的请求体。
 *
 * <p>使用场景：
 * - 首次注册：POST /api/v1/tools
 * - 更新（写新版本）：PUT /api/v1/tools/{id}
 *
 * <p>说明：
 * - executionConfig 为工具的执行配置，格式因 toolType 而异（HTTP 配置、Code 配置等）。
 * - parameters 为工具参数 schema，格式为 Map<String, ParameterSchema>。
 * </p>
 *
 * @author ypf
 * @version 1.0
 */
@Data
public class ToolRegisterRequest {

    /**
     * 工具调用名称。
     * 规则：字母/数字/中划线/下划线/中文，长度 2-100。
     */
    @NotBlank(message = "name 不能为空")
    @Size(min = 2, max = 100, message = "name 长度需在 2-100 之间")
    @Pattern(
        regexp = "^[a-zA-Z0-9_\\-\u4e00-\u9fa5]+$",
        message = "name 只允许字母、数字、中划线、下划线、中文"
    )
    private String name;

    /** 展示名称，可含任意 Unicode 字符 */
    @NotBlank(message = "displayName 不能为空")
    @Size(max = 128, message = "displayName 最长 128 字符")
    private String displayName;

    /** 工具简介 */
    private String introduction;

    /** 工具描述（用于 LLM tool_call 的 description 字段） */
    private String description;

    /**
     * 工具类型。
     * 允许值：HTTP / CODE / MCP / COMPOSITE / SKILL
     */
    private String toolType;

    /**
     * 可见性。
     * 允许值：public / private / workspace
     */
    @Pattern(regexp = "^(public|private|workspace)$", message = "visibility 只允许 public/private/workspace")
    private String visibility;

    /** 标签列表 */
    private List<String> tags;

    /**
     * 工具参数 schema（Map 格式，key 为参数名，value 为参数 schema）。
     * 对应 UnifiedToolDeclaration 的 parameters 字段。
     */
    private Map<String, Object> parameters;

    /**
     * 工具执行配置（各 toolType 格式不同）。
     * - HTTP：包含 url、method、headers 等
     * - CODE：包含 language、code 等
     * - MCP：包含 serverName、toolName 等
     */
    private Object executionConfig;
}
