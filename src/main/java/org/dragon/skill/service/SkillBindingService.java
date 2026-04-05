package org.dragon.skill.service;

import org.dragon.skill.domain.SkillBindingDO;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.dto.SkillBindingRequest;
import org.dragon.skill.dto.SkillBindingResult;
import org.dragon.skill.dto.SkillBindingVO;
import org.dragon.skill.enums.BindingType;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.VersionType;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillValidationException;
import org.dragon.skill.store.SkillBindingStore;
import org.dragon.skill.store.SkillStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Skill 绑定核心服务。
 *
 * <p>负责 Character / Workspace / Character+Workspace 三种绑定关系的增删查逻辑，
 * 不暴露 HTTP 接口，由 CharacterService / WorkspaceService 调用。
 *
 * <p>可用 skill 并集语义：
 * <pre>
 * 可用 skill = 关系一（Character 自有）
 *            ∪ 关系二（Workspace 公共）
 *            ∪ 关系三（Character@Workspace 专属）
 * </pre>
 *
 * <p>版本解析规则：
 * <ul>
 *   <li>{@code versionType = 'latest'}：运行时查询 skills 表，取 status='active' 的最大 version</li>
 *   <li>{@code versionType = 'fixed'}：直接使用 fixedVersion 定位具体版本记录</li>
 * </ul>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SkillBindingService {

    @Autowired private SkillBindingStore skillBindingStore;
    @Autowired private SkillStore        skillStore;

    // ── 绑定 ──────────────────────────────────────────────────────────

    /**
     * 绑定 Skill（Character 自有）。
     *
     * @param characterId Character 主键
     * @param request     绑定请求（skillId、versionType、fixedVersion）
     * @return 绑定结果
     */
    public SkillBindingResult bindToCharacter(String characterId, SkillBindingRequest request) {
        request.setBindingType("character");
        request.setCharacterId(characterId);
        request.setWorkspaceId(null);
        return doBind(request);
    }

    /**
     * 绑定 Skill 到 Workspace（公共 skill 池）。
     *
     * @param workspaceId Workspace 主键
     * @param request     绑定请求（skillId、versionType、fixedVersion）
     * @return 绑定结果
     */
    public SkillBindingResult bindToWorkspace(String workspaceId, SkillBindingRequest request) {
        request.setBindingType("workspace");
        request.setCharacterId(null);
        request.setWorkspaceId(workspaceId);
        return doBind(request);
    }

    /**
     * 绑定 Skill（Character 在特定 Workspace 下的专属 skill）。
     *
     * @param characterId Character 主键
     * @param workspaceId Workspace 主键
     * @param request     绑定请求（skillId、versionType、fixedVersion）
     * @return 绑定结果
     */
    public SkillBindingResult bindToCharacterInWorkspace(String characterId, String workspaceId,
                                                         SkillBindingRequest request) {
        request.setBindingType("character_workspace");
        request.setCharacterId(characterId);
        request.setWorkspaceId(workspaceId);
        return doBind(request);
    }

    // ── 解绑 ──────────────────────────────────────────────────────────

    /**
     * 按绑定记录主键解绑（通用，三种类型均适用）。
     *
     * @param bindingId 绑定记录物理主键
     */
    public void unbind(Long bindingId) {
        skillBindingStore.findById(bindingId)
                .orElseThrow(() -> new SkillNotFoundException("binding#" + bindingId));
        skillBindingStore.delete(bindingId);
    }

    // ── 按 ID 查找后解绑（workspaceId + skillId）──────────────────────

    /**
     * 解绑 Workspace 下的 Skill（通过 workspaceId + skillId 查找后删除）。
     */
    public void unbindWorkspaceSkill(String workspaceId, String skillId) {
        SkillBindingDO binding = findBindingByWorkspaceAndSkill(workspaceId, skillId);
        if (binding != null) {
            skillBindingStore.delete(binding.getId());
        }
    }

    /**
     * 解绑 Character 下的 Skill（通过 characterId + skillId 查找后删除）。
     */
    public void unbindCharacterSkill(String characterId, String skillId) {
        SkillBindingDO binding = findBindingByCharacterAndSkill(characterId, skillId);
        if (binding != null) {
            skillBindingStore.delete(binding.getId());
        }
    }

    /**
     * 解绑 Character 在特定 Workspace 下的专属 Skill（通过 characterId + workspaceId + skillId 查找后删除）。
     */
    public void unbindCharacterWorkspaceSkill(String characterId, String workspaceId, String skillId) {
        SkillBindingDO binding = findBindingByCharacterWorkspaceAndSkill(characterId, workspaceId, skillId);
        if (binding != null) {
            skillBindingStore.delete(binding.getId());
        }
    }

    // ── 按 ID 查找后更新绑定策略 ──────────────────────────────────────

    /**
     * 更新 Workspace 绑定策略（通过 workspaceId + skillId 查找后更新）。
     *
     * @param workspaceId  Workspace 主键
     * @param skillId      Skill 业务 UUID
     * @param versionType  版本策略（latest/fixed）
     * @param fixedVersion 固定版本号（versionType=fixed 时必填）
     */
    public void updateWorkspaceBinding(String workspaceId, String skillId,
                                       String versionType, Integer fixedVersion) {
        SkillBindingDO binding = findBindingByWorkspaceAndSkill(workspaceId, skillId);
        if (binding == null) {
            throw new SkillNotFoundException("workspace=" + workspaceId + ", skillId=" + skillId);
        }
        doUpdateBinding(binding, versionType, fixedVersion);
    }

    /**
     * 更新 Character 绑定策略（通过 characterId + skillId 查找后更新）。
     *
     * @param characterId  Character 主键
     * @param skillId      Skill 业务 UUID
     * @param versionType  版本策略（latest/fixed）
     * @param fixedVersion 固定版本号（versionType=fixed 时必填）
     */
    public void updateCharacterBinding(String characterId, String skillId,
                                       String versionType, Integer fixedVersion) {
        SkillBindingDO binding = findBindingByCharacterAndSkill(characterId, skillId);
        if (binding == null) {
            throw new SkillNotFoundException("characterId=" + characterId + ", skillId=" + skillId);
        }
        doUpdateBinding(binding, versionType, fixedVersion);
    }

    /**
     * 更新 Character 在特定 Workspace 下的专属 Skill 绑定策略。
     *
     * @param characterId  Character 主键
     * @param workspaceId Workspace 主键
     * @param skillId     Skill 业务 UUID
     * @param versionType 版本策略（latest/fixed）
     * @param fixedVersion 固定版本号（versionType=fixed 时必填）
     */
    public void updateCharacterWorkspaceBinding(String characterId, String workspaceId, String skillId,
                                                String versionType, Integer fixedVersion) {
        SkillBindingDO binding = findBindingByCharacterWorkspaceAndSkill(characterId, workspaceId, skillId);
        if (binding == null) {
            throw new SkillNotFoundException("characterId=" + characterId + ", workspaceId=" + workspaceId + ", skillId=" + skillId);
        }
        doUpdateBinding(binding, versionType, fixedVersion);
    }

    // ── 查询 ──────────────────────────────────────────────────────────

    /**
     * 查询 Character 自有的 Skill 绑定列表（含 skill 基本信息）。
     *
     * @param characterId Character 主键
     * @return 绑定 VO 列表
     */
    @Transactional(readOnly = true)
    public List<SkillBindingVO> listByCharacter(String characterId) {
        List<SkillBindingDO> bindings = skillBindingStore.findByCharacterId(characterId);
        return assembleVOList(bindings);
    }

    /**
     * 查询 Workspace 公共 Skill 绑定列表（含 skill 基本信息）。
     *
     * @param workspaceId Workspace 主键
     * @return 绑定 VO 列表
     */
    @Transactional(readOnly = true)
    public List<SkillBindingVO> listByWorkspace(String workspaceId) {
        List<SkillBindingDO> bindings = skillBindingStore.findByWorkspaceId(workspaceId);
        return assembleVOList(bindings);
    }

    /**
     * 查询 Character 在某 Workspace 下的专属 Skill 绑定列表（含 skill 基本信息）。
     *
     * @param characterId Character 主键
     * @param workspaceId Workspace 主键
     * @return 绑定 VO 列表
     */
    @Transactional(readOnly = true)
    public List<SkillBindingVO> listByCharacterInWorkspace(String characterId, String workspaceId) {
        List<SkillBindingDO> bindings =
                skillBindingStore.findByCharacterIdAndWorkspaceId(characterId, workspaceId);
        return assembleVOList(bindings);
    }

    /**
     * 查询 Character 在某 Workspace 下的全量可用 Skill（三种关系的并集）。
     *
     * <p>并集结果中同一 skillId 可能来自多种关系（如既是 character 自有，又在 workspace 中），
     * 此处保留全部绑定记录，调用方（Agent 运行时）可按需去重或取优先级最高的策略。
     *
     * @param characterId Character 主键
     * @param workspaceId Workspace 主键
     * @return 全量可用 skill 绑定 VO 列表
     */
    @Transactional(readOnly = true)
    public List<SkillBindingVO> listAvailableSkills(String characterId, String workspaceId) {
        List<SkillBindingDO> bindings =
                skillBindingStore.findAvailableByCharacterAndWorkspace(characterId, workspaceId);
        return assembleVOList(bindings);
    }

    /**
     * 解析某条绑定记录对应的实际 Skill 版本。
     *
     * <ul>
     *   <li>versionType = 'latest'：取 skills 表中 status='active' 的最大 version</li>
     *   <li>versionType = 'fixed'：直接取 fixedVersion 指定的版本记录</li>
     * </ul>
     *
     * @param binding 绑定记录
     * @return 对应的 SkillDO，若未找到则抛出 SkillNotFoundException
     */
    @Transactional(readOnly = true)
    public SkillDO resolveSkillVersion(SkillBindingDO binding) {
        String skillId = binding.getSkillId();
        if (VersionType.FIXED == binding.getVersionType()) {
            return skillStore.findBySkillIdAndVersion(skillId, binding.getFixedVersion())
                    .orElseThrow(() -> new SkillNotFoundException(
                            skillId + " v" + binding.getFixedVersion()));
        } else {
            return skillStore.findLatestActiveBySkillId(skillId)
                    .orElseThrow(() -> new SkillNotFoundException(
                            skillId + " (no active version)"));
        }
    }

    // ── 私有核心方法 ──────────────────────────────────────────────────

    /**
     * 绑定通用执行逻辑：
     * <ol>
     *   <li>参数校验（versionType=fixed 时 fixedVersion 必填）</li>
     *   <li>校验 skill 存在且处于 active 状态</li>
     *   <li>防重复绑定（幂等返回已有绑定结果）</li>
     *   <li>INSERT 绑定记录</li>
     * </ol>
     */
    private SkillBindingResult doBind(SkillBindingRequest request) {
        // 1. 参数校验
        validateRequest(request);

        // 2. 校验 skill 可用性，并解析实际版本号
        int resolvedVersion = validateAndResolveVersion(request);

        // 3. 防重复绑定（幂等）
        if (skillBindingStore.exists(request.getBindingType(),
                request.getCharacterId(), request.getWorkspaceId(), request.getSkillId())) {
            // 查出已有记录的 id，用于返回 bindingId
            SkillBindingDO existing = findExistingBinding(request);
            if (existing != null) {
                return new SkillBindingResult(
                        existing.getId(),
                        existing.getBindingType() != null ? existing.getBindingType().getValue() : null,
                        existing.getSkillId(),
                        existing.getVersionType() != null ? existing.getVersionType().getValue() : null,
                        resolvedVersion);
            }
        }

        // 4. 构建并插入绑定记录
        SkillBindingDO binding = buildBinding(request);
        skillBindingStore.save(binding);

        return new SkillBindingResult(
                binding.getId(),
                binding.getBindingType() != null ? binding.getBindingType().getValue() : null,
                binding.getSkillId(),
                binding.getVersionType() != null ? binding.getVersionType().getValue() : null,
                resolvedVersion);
    }

    /**
     * 参数合法性校验。
     */
    private void validateRequest(SkillBindingRequest request) {
        if ("fixed".equals(request.getVersionType()) && request.getFixedVersion() == null) {
            throw new SkillValidationException("versionType 为 fixed 时，fixedVersion 不能为空");
        }
        switch (request.getBindingType()) {
            case "character":
                if (request.getCharacterId() == null) {
                    throw new SkillValidationException("bindingType=character 时，characterId 不能为空");
                }
                break;
            case "workspace":
                if (request.getWorkspaceId() == null) {
                    throw new SkillValidationException("bindingType=workspace 时，workspaceId 不能为空");
                }
                break;
            case "character_workspace":
                if (request.getCharacterId() == null || request.getWorkspaceId() == null) {
                    throw new SkillValidationException(
                            "bindingType=character_workspace 时，characterId 和 workspaceId 均不能为空");
                }
                break;
            default:
                throw new SkillValidationException("不支持的 bindingType：" + request.getBindingType());
        }
    }

    /**
     * 校验 skill 可用性并返回实际版本号。
     * <ul>
     *   <li>versionType='latest'：必须存在至少一个 active 版本</li>
     *   <li>versionType='fixed'：指定版本必须存在且 status='active'</li>
     * </ul>
     *
     * @return 实际解析到的版本号（供 SkillBindingResult 展示，不影响绑定本身）
     */
    private int validateAndResolveVersion(SkillBindingRequest request) {
        String skillId = request.getSkillId();
        if ("fixed".equals(request.getVersionType())) {
            SkillDO skill = skillStore.findBySkillIdAndVersion(skillId, request.getFixedVersion())
                    .orElseThrow(() -> new SkillNotFoundException(
                            skillId + " v" + request.getFixedVersion()));
            if (SkillStatus.ACTIVE != skill.getStatus()) {
                throw new SkillValidationException(
                        "skill [" + skillId + "] v" + request.getFixedVersion()
                                + " 状态为 " + skill.getStatus() + "，只能绑定 active 状态的版本");
            }
            return skill.getVersion();
        } else {
            SkillDO skill = skillStore.findLatestActiveBySkillId(skillId)
                    .orElseThrow(() -> new SkillNotFoundException(
                            skillId + " (no active version)"));
            return skill.getVersion();
        }
    }

    /** 构建 SkillBindingDO */
    private SkillBindingDO buildBinding(SkillBindingRequest request) {
        SkillBindingDO binding = new SkillBindingDO();
        binding.setBindingType(BindingType.fromValue(request.getBindingType()));
        binding.setCharacterId(request.getCharacterId());
        binding.setWorkspaceId(request.getWorkspaceId());
        binding.setSkillId(request.getSkillId());
        binding.setVersionType(VersionType.fromValue(request.getVersionType()));
        binding.setFixedVersion(VersionType.FIXED.name().equalsIgnoreCase(request.getVersionType())
                ? request.getFixedVersion() : null);
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        return binding;
    }

    /**
     * 查找已有的重复绑定记录（exists() 返回 true 后调用，用于取出 id）。
     * 根据 bindingType 选择合适的 store 查询方法。
     */
    private SkillBindingDO findExistingBinding(SkillBindingRequest request) {
        List<SkillBindingDO> candidates;
        switch (request.getBindingType()) {
            case "character":
                candidates = skillBindingStore.findByCharacterId(request.getCharacterId());
                break;
            case "workspace":
                candidates = skillBindingStore.findByWorkspaceId(request.getWorkspaceId());
                break;
            case "character_workspace":
                candidates = skillBindingStore.findByCharacterIdAndWorkspaceId(
                        request.getCharacterId(), request.getWorkspaceId());
                break;
            default:
                return null;
        }
        return candidates.stream()
                .filter(b -> b.getSkillId().equals(request.getSkillId()))
                .findFirst()
                .orElse(null);
    }

    // ── 辅助方法：按 ID 查找绑定 ─────────────────────────────────────

    /**
     * 通过 workspaceId + skillId 查找绑定记录。
     */
    private SkillBindingDO findBindingByWorkspaceAndSkill(String workspaceId, String skillId) {
        List<SkillBindingDO> candidates = skillBindingStore.findByWorkspaceId(workspaceId);
        return candidates.stream()
                .filter(b -> skillId.equals(b.getSkillId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 通过 characterId + skillId 查找绑定记录。
     */
    private SkillBindingDO findBindingByCharacterAndSkill(String characterId, String skillId) {
        List<SkillBindingDO> candidates = skillBindingStore.findByCharacterId(characterId);
        return candidates.stream()
                .filter(b -> skillId.equals(b.getSkillId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 通过 characterId + workspaceId + skillId 查找绑定记录。
     */
    private SkillBindingDO findBindingByCharacterWorkspaceAndSkill(String characterId, String workspaceId, String skillId) {
        List<SkillBindingDO> candidates = skillBindingStore.findByCharacterIdAndWorkspaceId(characterId, workspaceId);
        return candidates.stream()
                .filter(b -> skillId.equals(b.getSkillId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 通用绑定更新逻辑。
     */
    private void doUpdateBinding(SkillBindingDO binding, String versionType, Integer fixedVersion) {
        if ("fixed".equals(versionType) && fixedVersion == null) {
            throw new SkillValidationException("versionType 为 fixed 时，fixedVersion 不能为空");
        }
        binding.setVersionType(VersionType.fromValue(versionType));
        binding.setFixedVersion("fixed".equals(versionType) ? fixedVersion : null);
        binding.setUpdatedAt(LocalDateTime.now());
        skillBindingStore.update(binding);
    }

    // ── VO 聚合组装 ───────────────────────────────────────────────────

    /**
     * 将绑定 DO 列表组装为 VO 列表。
     * 对每条绑定，按其版本策略查询对应的 Skill 信息，填充到 VO 中。
     * 若某条绑定对应的 Skill 不存在或无 active 版本，则跳过（不抛异常，保证列表查询健壮性）。
     */
    private List<SkillBindingVO> assembleVOList(List<SkillBindingDO> bindings) {
        List<SkillBindingVO> result = new ArrayList<>();
        for (SkillBindingDO binding : bindings) {
            try {
                Optional<SkillDO> skillOpt = resolveSkillForVO(binding);
                skillOpt.ifPresent(skill -> result.add(toVO(binding, skill)));
            } catch (Exception ignored) {
                // skill 已删除或版本不存在时跳过，不中断整个列表
            }
        }
        return result;
    }

    /**
     * 按绑定的版本策略解析对应的 Skill 记录（返回 Optional，不抛异常）。
     */
    private Optional<SkillDO> resolveSkillForVO(SkillBindingDO binding) {
        if (VersionType.FIXED == binding.getVersionType()) {
            return skillStore.findBySkillIdAndVersion(
                    binding.getSkillId(), binding.getFixedVersion());
        } else {
            return skillStore.findLatestActiveBySkillId(binding.getSkillId());
        }
    }

    /**
     * 将 SkillBindingDO + SkillDO 组装为 SkillBindingVO。
     */
    private SkillBindingVO toVO(SkillBindingDO binding, SkillDO skill) {
        SkillBindingVO vo = new SkillBindingVO();
        // 绑定信息
        vo.setBindingId(binding.getId());
        vo.setBindingType(binding.getBindingType() != null ? binding.getBindingType().getValue() : null);
        vo.setCharacterId(binding.getCharacterId());
        vo.setWorkspaceId(binding.getWorkspaceId());
        // Skill 基本信息
        vo.setSkillId(skill.getSkillId());
        vo.setSkillName(skill.getName());
        vo.setSkillDisplayName(skill.getDisplayName());
        vo.setSkillDescription(skill.getDescription());
        vo.setSkillStatus(skill.getStatus() != null ? skill.getStatus().getValue() : null);
        // 版本策略
        vo.setVersionType(binding.getVersionType() != null ? binding.getVersionType().getValue() : null);
        vo.setDisplayVersion(skill.getVersion());
        // 时间
        vo.setCreatedAt(binding.getCreatedAt());
        return vo;
    }
}

