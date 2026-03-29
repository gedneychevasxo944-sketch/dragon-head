package org.dragon.skill.service;

import org.dragon.skill.dto.*;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Skill 管理服务接口。
 * 提供 Skill 的 CRUD 管理能力。
 *
 * @since 1.0
 */
public interface SkillManageService {

    /**
     * 创建新 Skill（上传 ZIP 包 + 管理元数据）。
     *
     * @param file    上传的 ZIP 包
     * @param request 管理元数据
     * @return 创建后的 Skill 响应
     */
    SkillResponse createSkill(MultipartFile file, SkillCreateRequest request);

    /**
     * 更新 Skill（可选上传新 ZIP 包 + 更新管理元数据）。
     *
     * @param skillId Skill ID
     * @param file    新 ZIP 包（可选，为 null 时仅更新元数据）
     * @param request 更新的管理元数据
     * @return 更新后的 Skill 响应
     */
    SkillResponse updateSkill(Long skillId, MultipartFile file, SkillUpdateRequest request);

    /**
     * 删除 Skill。
     *
     * @param skillId Skill ID
     */
    void deleteSkill(Long skillId);

    /**
     * 分页查询 Skill 列表。
     *
     * @param request 查询条件
     * @return 列表结果
     */
    List<SkillResponse> listSkills(SkillQueryRequest request);

    /**
     * 查询单个 Skill 详情。
     *
     * @param skillId Skill ID
     * @return Skill 详情
     */
    SkillResponse getSkill(Long skillId);

    /**
     * 禁用 Skill。
     *
     * @param skillId Skill ID
     */
    void disableSkill(Long skillId);

    /**
     * 重新激活被禁用的 Skill。
     *
     * @param skillId Skill ID
     */
    void enableSkill(Long skillId);
}
