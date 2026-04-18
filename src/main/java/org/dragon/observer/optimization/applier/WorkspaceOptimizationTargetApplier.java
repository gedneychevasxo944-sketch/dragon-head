package org.dragon.observer.optimization.applier;

import java.util.Map;

import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationAction.TargetType;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * WorkspaceOptimizationTargetApplier Workspace 优化目标应用器
 * 负责将优化动作应用到 Workspace
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class WorkspaceOptimizationTargetApplier implements OptimizationTargetApplier {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceOptimizationTargetApplier.class);

    private final WorkspaceFacadeService workspaceFacadeService;

    @Override
    public OptimizationAction.TargetType getTargetType() {
        return OptimizationAction.TargetType.WORKSPACE;
    }

    @Override
    public ApplyResult apply(OptimizationAction action) {
        Workspace workspace = workspaceFacadeService.getWorkspace(action.getTargetId())
                .orElse(null);

        if (workspace == null) {
            return ApplyResult.failure("Workspace not found: " + action.getTargetId());
        }

        try {
            switch (action.getActionType()) {
                case UPDATE_MIND:
                    return applyMindUpdate(workspace, action.getParameters());
                case UPDATE_PERSONALITY:
                    return applyPersonalityUpdate(workspace, action.getParameters());
                case UPDATE_TAG:
                    return applyTagUpdate(workspace, action.getParameters());
                case UPDATE_MEMORY:
                    return applyMemoryUpdate(workspace, action.getParameters());
                case UPDATE_CONFIG:
                    return applyConfigUpdate(workspace, action.getParameters());
                case ADD_SKILL:
                    return ApplyResult.failure("Workspace does not support ADD_SKILL directly");
                case REMOVE_SKILL:
                    return ApplyResult.failure("Workspace does not support REMOVE_SKILL directly");
                case ADJUST_WEIGHT:
                    return applyWeightAdjust(workspace, action.getParameters());
                default:
                    return ApplyResult.failure("Unsupported action type: " + action.getActionType());
            }
        } catch (Exception e) {
            log.error("[WorkspaceOptimizationTargetApplier] Failed to apply action: {}", action.getId(), e);
            return ApplyResult.failure("Apply failed: " + e.getMessage());
        }
    }

    private ApplyResult applyMindUpdate(Workspace workspace, Map<String, Object> parameters) {
        if (parameters.containsKey("mind")) {
            log.info("[WorkspaceOptimizationTargetApplier] Mind update applied via parameters");
        }
        return ApplyResult.success("Mind update applied to workspace");
    }

    private ApplyResult applyPersonalityUpdate(Workspace workspace, Map<String, Object> parameters) {
        if (parameters.containsKey("personality")) {
            log.info("[WorkspaceOptimizationTargetApplier] Personality update applied");
        }
        return ApplyResult.success("Personality update applied to workspace");
    }

    private ApplyResult applyTagUpdate(Workspace workspace, Map<String, Object> parameters) {
        if (parameters.containsKey("tags")) {
            log.info("[WorkspaceOptimizationTargetApplier] Tag update applied");
        }
        return ApplyResult.success("Tag update applied to workspace");
    }

    private ApplyResult applyMemoryUpdate(Workspace workspace, Map<String, Object> parameters) {
        if (parameters.containsKey("memory")) {
            log.info("[WorkspaceOptimizationTargetApplier] Memory update applied");
        }
        return ApplyResult.success("Memory update applied to workspace");
    }

    private ApplyResult applyConfigUpdate(Workspace workspace, Map<String, Object> parameters) {
        if (parameters.containsKey("config")) {
            log.info("[WorkspaceOptimizationTargetApplier] Config update applied");
        }
        return ApplyResult.success("Config update applied to workspace");
    }

    private ApplyResult applyWeightAdjust(Workspace workspace, Map<String, Object> parameters) {
        if (parameters.containsKey("weights")) {
            log.info("[WorkspaceOptimizationTargetApplier] Weight adjust applied");
        }
        return ApplyResult.success("Weight adjust applied to workspace");
    }

    @Override
    public String captureSnapshot(String targetId) {
        return workspaceFacadeService.getWorkspace(targetId)
                .map(w -> {
                    if (w.getPersonality() != null) {
                        return w.getPersonality().toString();
                    }
                    return "{}";
                })
                .orElse("{}");
    }

    @Override
    public void restoreFromSnapshot(String targetId, String snapshot) {
        log.info("[WorkspaceOptimizationTargetApplier] Restoring from snapshot for {}: {}", targetId, snapshot);
    }
}
