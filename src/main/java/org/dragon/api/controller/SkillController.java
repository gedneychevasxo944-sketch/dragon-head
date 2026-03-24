package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.dragon.skill.dto.SkillCreateRequest;
import org.dragon.skill.dto.SkillQueryRequest;
import org.dragon.skill.dto.SkillResponse;
import org.dragon.skill.dto.SkillUpdateRequest;
import org.dragon.skill.service.SkillManageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Skill 管理 REST 控制器。
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillManageService skillManageService;

    @Operation(summary = "创建 Skill")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SkillResponse> createSkill(
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") SkillCreateRequest request) {
        SkillResponse response = skillManageService.createSkill(file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "更新 Skill")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SkillResponse> updateSkill(
            @PathVariable Long id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart("data") SkillUpdateRequest request) {
        SkillResponse response = skillManageService.updateSkill(id, file, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "删除 Skill")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillManageService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "查询 Skill 列表")
    @GetMapping
    public ResponseEntity<List<SkillResponse>> listSkills(SkillQueryRequest request) {
        List<SkillResponse> list = skillManageService.listSkills(request);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "查询单个 Skill 详情")
    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> getSkill(@PathVariable Long id) {
        return ResponseEntity.ok(skillManageService.getSkill(id));
    }

    @Operation(summary = "禁用 Skill")
    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disableSkill(@PathVariable Long id) {
        skillManageService.disableSkill(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "重新启用 Skill")
    @PostMapping("/{id}/enable")
    public ResponseEntity<Void> enableSkill(@PathVariable Long id) {
        skillManageService.enableSkill(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "手动重新加载 Skill")
    @PostMapping("/{id}/reload")
    public ResponseEntity<Void> reloadSkill(@PathVariable Long id) {
        skillManageService.reloadSkill(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "获取所有 Skill 生命周期状态快照")
    @GetMapping("/lifecycle")
    public ResponseEntity<Map<String, String>> getLifecycleSnapshot() {
        return ResponseEntity.ok(skillManageService.getLifecycleSnapshot());
    }

    @Operation(summary = "重试所有 FAILED 状态的 Skill")
    @PostMapping("/retry-failed")
    public ResponseEntity<Void> retryFailedSkills() {
        skillManageService.retryFailedSkills();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "全量重新加载")
    @PostMapping("/full-reload")
    public ResponseEntity<Void> fullReload() {
        skillManageService.fullReload();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "获取当前 System Prompt Fragment")
    @GetMapping("/system-prompt")
    public ResponseEntity<String> getSystemPromptFragment() {
        return ResponseEntity.ok(skillManageService.getSystemPromptFragment());
    }
}
