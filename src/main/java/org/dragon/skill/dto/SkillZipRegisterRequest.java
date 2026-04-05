package org.dragon.skill.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * ZIP 包方式注册/更新 Skill 的请求体。
 * <p>
 * 使用场景：
 * - 首次注册：POST /api/skills/zip
 * - 更新（写新版本）：POST /api/skills/{skillId}/zip
 * </p>
 *
 * ZIP 包结构约定：
 * <pre>
 * my-skill.zip
 * ├── SKILL.md          （必须，位于根目录）
 * ├── schema/
 * │   └── input.json    （可选附件）
 * └── examples/
 *     └── sample.txt    （可选附件）
 * </pre>
 *
 * 校验规则（由 SkillValidationService 执行）：
 * - ZIP 包本身不超过 10MB
 * - 解压后文件总大小不超过 50MB，文件数不超过 100
 * - 不允许路径穿越（../）、软链接
 * - 文件扩展名白名单：.md .txt .json .yaml .yml
 * - 不允许危险扩展名：.sh .exe .py .js .ts .bat 等
 */
@Data
public class SkillZipRegisterRequest {

    /** ZIP 压缩包文件，必填 */
    @NotNull(message = "zipFile 不能为空")
    private MultipartFile zipFile;

    /**
     * 可见性覆盖（可选）。
     * 优先级高于 SKILL.md frontmatter 中的 visibility 字段。
     * 允许值：public / private
     */
    @Pattern(regexp = "^(public|private)$", message = "visibility 只允许 public 或 private")
    private String visibility;
}

