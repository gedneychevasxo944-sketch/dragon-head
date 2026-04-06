package org.dragon.skill.service;

import org.dragon.character.store.CharacterStore;
import org.dragon.skill.actionlog.BindCharacterDetail;
import org.dragon.skill.actionlog.BindWorkspaceDetail;
import org.dragon.skill.actionlog.BindCharacterWorkspaceDetail;
import org.dragon.skill.actionlog.UnbindDetail;
import org.dragon.skill.actionlog.SkillActionLogService;
import org.dragon.skill.domain.SkillBindingDO;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.dto.SkillBindingRequest;
import org.dragon.skill.dto.SkillBindingResult;
import org.dragon.skill.dto.SkillBindingVO;
import org.dragon.skill.enums.BindingType;
import org.dragon.skill.enums.SkillActionType;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.exception.SkillValidationException;
import org.dragon.skill.store.SkillBindingStore;
import org.dragon.skill.store.SkillStore;
import org.dragon.store.StoreFactory;
import org.dragon.util.bean.UserInfo;
import org.dragon.workspace.store.WorkspaceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Skill 绑定核心服务。
 *
 * <p>绑定关系绑定到 Skill 本身，具体使用哪个版本由 skill.publishedVersionId 决定。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SkillBindingService {

    @Autowired private StoreFactory storeFactory;
    @Autowired private SkillBindingStore skillBindingStore;
    @Autowired private SkillStore skillStore;
    @Autowired private SkillActionLogService actionLogService;

    // ── 绑定 ──────────────────────────────────────────────────────────

    public SkillBindingResult bindToCharacter(String characterId,
                                              SkillBindingRequest request, UserInfo user) {
        request.setBindingType("character");
        request.setCharacterId(characterId);
        request.setWorkspaceId(null);
        return doBind(request, user);
    }

    public SkillBindingResult bindToWorkspace(String workspaceId,
                                               SkillBindingRequest request, UserInfo user) {
        request.setBindingType("workspace");
        request.setCharacterId(null);
        request.setWorkspaceId(workspaceId);
        return doBind(request, user);
    }

    public SkillBindingResult bindToCharacterInWorkspace(String characterId,
                                                         String workspaceId,
                                                         SkillBindingRequest request, UserInfo user) {
        request.setBindingType("character_workspace");
        request.setCharacterId(characterId);
        request.setWorkspaceId(workspaceId);
        return doBind(request, user);
    }

    // ── 解绑 ──────────────────────────────────────────────────────────

    public void unbind(Long bindingId, UserInfo user) {
        SkillBindingDO binding = skillBindingStore.findById(bindingId)
                .orElseThrow(() -> new SkillNotFoundException("binding#" + bindingId));
        skillBindingStore.delete(bindingId);

        SkillDO skill = skillStore.findBySkillId(binding.getSkillId()).orElse(null);
        String skillName = skill != null ? skill.getName() : binding.getSkillId();
        UnbindDetail detail = new UnbindDetail(
                binding.getBindingType() != null ? binding.getBindingType().getValue() : null,
                binding.getCharacterId(), null,
                binding.getWorkspaceId(), null);
        actionLogService.log(binding.getSkillId(), skillName, SkillActionType.UNBIND,
                parseUserId(user.getUserId()), user.getUsername(), null, detail);
    }

    public void unbindWorkspaceSkill(String workspaceId, String skillId, UserInfo user) {
        SkillBindingDO binding = findBindingByWorkspaceAndSkill(workspaceId, skillId);
        if (binding != null) {
            SkillDO skill = skillStore.findBySkillId(skillId).orElse(null);
            String skillName = skill != null ? skill.getName() : skillId;
            String workspaceName = getWorkspaceName(workspaceId);
            UnbindDetail detail = new UnbindDetail("workspace", null, null, workspaceId, workspaceName);
            actionLogService.log(skillId, skillName, SkillActionType.UNBIND,
                    parseUserId(user.getUserId()), user.getUsername(), null, detail);
            skillBindingStore.delete(binding.getId());
        }
    }

    public void unbindCharacterSkill(String characterId, String skillId, UserInfo user) {
        SkillBindingDO binding = findBindingByCharacterAndSkill(characterId, skillId);
        if (binding != null) {
            SkillDO skill = skillStore.findBySkillId(skillId).orElse(null);
            String skillName = skill != null ? skill.getName() : skillId;
            String characterName = getCharacterName(characterId);
            UnbindDetail detail = new UnbindDetail("character", characterId, characterName, null, null);
            actionLogService.log(skillId, skillName, SkillActionType.UNBIND,
                    parseUserId(user.getUserId()), user.getUsername(), null, detail);
            skillBindingStore.delete(binding.getId());
        }
    }

    public void unbindCharacterWorkspaceSkill(String characterId, String workspaceId, String skillId, UserInfo user) {
        SkillBindingDO binding = findBindingByCharacterWorkspaceAndSkill(characterId, workspaceId, skillId);
        if (binding != null) {
            SkillDO skill = skillStore.findBySkillId(skillId).orElse(null);
            String skillName = skill != null ? skill.getName() : skillId;
            String characterName = getCharacterName(characterId);
            String workspaceName = getWorkspaceName(workspaceId);
            UnbindDetail detail = new UnbindDetail("character_workspace", characterId, characterName, workspaceId, workspaceName);
            actionLogService.log(skillId, skillName, SkillActionType.UNBIND,
                    parseUserId(user.getUserId()), user.getUsername(), null, detail);
            skillBindingStore.delete(binding.getId());
        }
    }

    // ── 查询 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SkillBindingVO> listByCharacter(String characterId) {
        List<SkillBindingDO> bindings = skillBindingStore.findByCharacterId(characterId);
        return assembleVOList(bindings);
    }

    @Transactional(readOnly = true)
    public List<SkillBindingVO> listByWorkspace(String workspaceId) {
        List<SkillBindingDO> bindings = skillBindingStore.findByWorkspaceId(workspaceId);
        return assembleVOList(bindings);
    }

    @Transactional(readOnly = true)
    public List<SkillBindingVO> listByCharacterInWorkspace(String characterId, String workspaceId) {
        List<SkillBindingDO> bindings =
                skillBindingStore.findByCharacterIdAndWorkspaceId(characterId, workspaceId);
        return assembleVOList(bindings);
    }

    @Transactional(readOnly = true)
    public List<SkillBindingVO> listAvailableSkills(String characterId, String workspaceId) {
        List<SkillBindingDO> bindings =
                skillBindingStore.findAvailableByCharacterAndWorkspace(characterId, workspaceId);
        return assembleVOList(bindings);
    }

    // ── 私有核心方法 ──────────────────────────────────────────────────

    private SkillBindingResult doBind(SkillBindingRequest request, UserInfo user) {
        validateRequest(request);

        // 校验 skill 存在
        SkillDO skill = skillStore.findBySkillId(request.getSkillId())
                .orElseThrow(() -> new SkillNotFoundException(request.getSkillId()));

        // 防重复绑定
        if (skillBindingStore.exists(request.getBindingType(),
                request.getCharacterId(), request.getWorkspaceId(), request.getSkillId())) {
            SkillBindingDO existing = findExistingBinding(request);
            if (existing != null) {
                return new SkillBindingResult(existing.getId(), existing.getSkillId());
            }
        }

        // 插入绑定记录
        SkillBindingDO binding = buildBinding(request);
        skillBindingStore.save(binding);

        // 记录日志
        String characterName = getCharacterName(request.getCharacterId());
        String workspaceName = getWorkspaceName(request.getWorkspaceId());
        if ("character".equals(request.getBindingType())) {
            BindCharacterDetail detail = new BindCharacterDetail(request.getCharacterId(), characterName);
            actionLogService.log(binding.getSkillId(), skill.getName(), SkillActionType.BIND_CHARACTER,
                    parseUserId(user.getUserId()), user.getUsername(), null, detail);
        } else if ("workspace".equals(request.getBindingType())) {
            BindWorkspaceDetail detail = new BindWorkspaceDetail(request.getWorkspaceId(), workspaceName);
            actionLogService.log(binding.getSkillId(), skill.getName(), SkillActionType.BIND_WORKSPACE,
                    parseUserId(user.getUserId()), user.getUsername(), null, detail);
        } else {
            BindCharacterWorkspaceDetail detail = new BindCharacterWorkspaceDetail(
                    request.getCharacterId(), characterName, request.getWorkspaceId(), workspaceName);
            actionLogService.log(binding.getSkillId(), skill.getName(), SkillActionType.BIND_CHARACTER_WORKSPACE,
                    parseUserId(user.getUserId()), user.getUsername(), null, detail);
        }

        return new SkillBindingResult(binding.getId(), binding.getSkillId());
    }

    private void validateRequest(SkillBindingRequest request) {
        switch (request.getBindingType()) {
            case "character":
                if (request.getCharacterId() == null) {
                    throw new SkillValidationException("characterId 不能为空");
                }
                break;
            case "workspace":
                if (request.getWorkspaceId() == null) {
                    throw new SkillValidationException("workspaceId 不能为空");
                }
                break;
            case "character_workspace":
                if (request.getCharacterId() == null || request.getWorkspaceId() == null) {
                    throw new SkillValidationException("characterId 和 workspaceId 不能为空");
                }
                break;
            default:
                throw new SkillValidationException("不支持的 bindingType：" + request.getBindingType());
        }
    }

    private SkillBindingDO buildBinding(SkillBindingRequest request) {
        SkillBindingDO binding = new SkillBindingDO();
        binding.setBindingType(BindingType.fromValue(request.getBindingType()));
        binding.setCharacterId(request.getCharacterId());
        binding.setWorkspaceId(request.getWorkspaceId());
        binding.setSkillId(request.getSkillId());
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());
        return binding;
    }

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

    private SkillBindingDO findBindingByWorkspaceAndSkill(String workspaceId, String skillId) {
        List<SkillBindingDO> candidates = skillBindingStore.findByWorkspaceId(workspaceId);
        return candidates.stream()
                .filter(b -> skillId.equals(b.getSkillId()))
                .findFirst()
                .orElse(null);
    }

    private SkillBindingDO findBindingByCharacterAndSkill(String characterId, String skillId) {
        List<SkillBindingDO> candidates = skillBindingStore.findByCharacterId(characterId);
        return candidates.stream()
                .filter(b -> skillId.equals(b.getSkillId()))
                .findFirst()
                .orElse(null);
    }

    private SkillBindingDO findBindingByCharacterWorkspaceAndSkill(String characterId, String workspaceId, String skillId) {
        List<SkillBindingDO> candidates = skillBindingStore.findByCharacterIdAndWorkspaceId(characterId, workspaceId);
        return candidates.stream()
                .filter(b -> skillId.equals(b.getSkillId()))
                .findFirst()
                .orElse(null);
    }

    // ── VO 聚合组装 ───────────────────────────────────────────────────

    private List<SkillBindingVO> assembleVOList(List<SkillBindingDO> bindings) {
        List<SkillBindingVO> result = new ArrayList<>();
        for (SkillBindingDO binding : bindings) {
            try {
                SkillDO skill = skillStore.findBySkillId(binding.getSkillId()).orElse(null);
                if (skill != null) {
                    result.add(toVO(binding, skill));
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private SkillBindingVO toVO(SkillBindingDO binding, SkillDO skill) {
        SkillBindingVO vo = new SkillBindingVO();
        vo.setBindingId(binding.getId());
        vo.setBindingType(binding.getBindingType() != null ? binding.getBindingType().getValue() : null);
        vo.setCharacterId(binding.getCharacterId());
        vo.setWorkspaceId(binding.getWorkspaceId());
        vo.setSkillId(skill.getId());
        vo.setSkillName(skill.getName());
        vo.setSkillDisplayName(null);
        vo.setSkillDescription(skill.getDescription());
        vo.setSkillStatus(skill.getStatus() != null ? skill.getStatus().getValue() : null);
        vo.setCreatedAt(binding.getCreatedAt());
        return vo;
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private CharacterStore getCharacterStore() {
        return storeFactory.get(CharacterStore.class);
    }

    private WorkspaceStore getWorkspaceStore() {
        return storeFactory.get(WorkspaceStore.class);
    }

    private String getCharacterName(String characterId) {
        if (characterId == null) return null;
        return getCharacterStore().findById(characterId)
                .map(org.dragon.character.Character::getName)
                .orElse(null);
    }

    private String getWorkspaceName(String workspaceId) {
        if (workspaceId == null) return null;
        return getWorkspaceStore().findById(workspaceId)
                .map(org.dragon.workspace.Workspace::getName)
                .orElse(null);
    }
}
