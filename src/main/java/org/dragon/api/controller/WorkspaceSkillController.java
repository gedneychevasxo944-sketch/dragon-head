package org.dragon.api.controller;

import java.util.List;
import java.util.Map;

import org.dragon.skill.dto.WorkspaceSkillBindRequest;
import org.dragon.skill.dto.WorkspaceSkillResponse;
import org.dragon.skill.dto.WorkspaceSkillUpdateRequest;
import org.dragon.skill.service.WorkspaceSkillService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Workspace Skill 关联管理接口。
 *
 * POST   /api/workspaces/{workspaceId}/skills           - 圈选 Skill
 * DELETE /api/workspaces/{workspaceId}/skills/{skillId} - 取消圈选
 * PUT    /api/workspaces/{workspaceId}/skills/{skillId} - 更新版本策略/启用状态
 * GET    /api/workspaces/{workspaceId}/skills           - 查询已圈选的 Skill 列表
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/skills")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorkspaceSkillController {

    private final WorkspaceSkillService workspaceSkillService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> bindSkill(
            @PathVariable Long workspaceId,
            @RequestBody @Valid WorkspaceSkillBindRequest request) {
        WorkspaceSkillResponse response = workspaceSkillService.bindSkill(workspaceId, request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<Map<String, Object>> unbindSkill(
            @PathVariable Long workspaceId,
            @PathVariable Long skillId) {
        workspaceSkillService.unbindSkill(workspaceId, skillId);
        return ResponseEntity.ok(Map.of("success", true, "data", (Object) null));
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<Map<String, Object>> updateBinding(
            @PathVariable Long workspaceId,
            @PathVariable Long skillId,
            @RequestBody WorkspaceSkillUpdateRequest request) {
        WorkspaceSkillResponse response = workspaceSkillService.updateBinding(workspaceId, skillId, request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listSkills(
            @PathVariable Long workspaceId) {
        List<WorkspaceSkillResponse> list = workspaceSkillService.listWorkspaceSkills(workspaceId);
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }
}