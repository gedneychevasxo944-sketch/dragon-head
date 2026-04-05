package org.dragon.skill.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 注册/更新 Skill 的请求体。
 *
 * <p>使用场景：
 * - 首次注册：POST /api/v1/skills
 * - 更新（写新版本）：PUT /api/v1/skills/{skillId}
 *
 * <p>说明：
 * - content 字段为在线编辑或直接填写的 SKILL.md 正文（frontmatter 之后的部分）。
 * - Service 层会将 frontmatter 元信息 + content 重新序列化成完整的 SKILL.md 文件上传到 Storage。
 * - attachments 为除 SKILL.md 之外的辅助文件（如 schema、示例文件等），可为空。
 * </p>
 */
@Data
public class SkillRegisterRequest {

    /**
     * 技能调用名称。
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
    @Size(max = 100, message = "displayName 最长 100 字符")
    private String displayName;

    /** 技能描述 */
    private String description;

    /**
     * SKILL.md 正文内容（Prompt 模板，不含 frontmatter）。
     * 在线编辑时直接填写；上传文件时由 Service 解析后填入。
     */
    @NotBlank(message = "content 不能为空")
    private String content;

    /** 别名列表 */
    private List<String> aliases;

    /** 使用场景说明 */
    private String whenToUse;

    /** 参数提示 */
    private String argumentHint;

    /** 允许的工具列表 */
    private List<String> allowedTools;

    /** 指定模型，可为 null（使用默认模型） */
    private String model;

    /** 是否禁用模型自动调用，默认 false */
    private Boolean disableModelInvocation;

    /** 用户是否可调用，默认 true */
    private Boolean userInvocable;

    /**
     * 执行上下文。
     * 允许值：inline（当前会话内执行）/ fork（独立子会话执行）
     */
    @Pattern(regexp = "^(inline|fork)$", message = "executionContext 只允许 inline 或 fork")
    private String executionContext;

    /**
     * 努力程度。
     * 允许值：auto / quick / standard / thorough
     */
    @Pattern(regexp = "^(auto|quick|standard|thorough)$", message = "effort 值非法")
    private String effort;

    /** 技能分类：development / deployment / analysis / utility / integration / other */
    private String category;

    /**
     * 可见性。
     * 允许值：public / private
     */
    @Pattern(regexp = "^(public|private)$", message = "visibility 只允许 public 或 private")
    private String visibility;

    /**
     * 附加文件列表（可为空）。
     * 每个文件必须通过 SkillValidationService 校验（大小、扩展名白名单）。
     * 注意：MultipartFile 字段不参与 @Validated JSON 反序列化，
     *       由 Controller 通过 @RequestPart 单独接收后注入。
     */
    private List<MultipartFile> attachments;
}

