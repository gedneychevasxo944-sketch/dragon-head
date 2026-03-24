package org.dragon.agent.orchestration;

import java.util.UUID;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 编排服务实现
 * 只负责决策（决定使用哪种执行策略）
 * 返回策略信息，由 Character 执行具体流程
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
public class OrchestrationServiceImpl implements OrchestrationService {

    private final CharacterRegistry characterRegistry;

    public OrchestrationServiceImpl(CharacterRegistry characterRegistry) {
        this.characterRegistry = characterRegistry;
    }

    @Override
    public OrchestrationResult orchestrate(OrchestrationRequest request) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 获取 Character
            Character character = characterRegistry.get(request.getCharacterId())
                    .orElseThrow(() -> new IllegalArgumentException("Character not found: " + request.getCharacterId()));

            // 根据模式决定执行策略
            Mode mode = request.getMode();
            String workflowId = request.getWorkflowId();

            // 如果没有指定模式或 workflowId，由 decideMode 决策
            if (mode == null || mode == Mode.REACT) {
                Mode resolvedMode = decideMode(request, character);
                if (resolvedMode == Mode.WORKFLOW) {
                    mode = Mode.WORKFLOW;
                    workflowId = getDefaultWorkflowId(character);
                } else {
                    mode = Mode.REACT;
                }
            } else if (mode == Mode.WORKFLOW && (workflowId == null || workflowId.isEmpty())) {
                workflowId = getDefaultWorkflowId(character);
            }

            log.info("[Orchestration] Decision: mode={}, workflowId={}", mode, workflowId);

            // 返回编排结果，包含执行策略信息（不包含执行结果，执行结果由 Character.run() 返回）
            return new OrchestrationResult.Builder()
                    .executionId(executionId)
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .mode(mode)
                    .workflowId(workflowId)
                    .build();

        } catch (Exception e) {
            log.error("[Orchestration] Orchestration error: {}", executionId, e);
            return new OrchestrationResult.Builder()
                    .executionId(executionId)
                    .success(false)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 决定执行模式
     * 规则：
     * 1. request.workflowId != null -> WORKFLOW
     * 2. request.mode == WORKFLOW && workflowId 可解析 -> WORKFLOW
     * 3. 其余一律 REACT
     */
    private Mode decideMode(OrchestrationRequest request, Character character) {
        // 1. 显式指定 workflowId
        if (request.getWorkflowId() != null && !request.getWorkflowId().isEmpty()) {
            return Mode.WORKFLOW;
        }

        // 2. 显式指定 WORKFLOW 模式且可获取 workflowId
        if (request.getMode() == Mode.WORKFLOW) {
            String workflowId = getDefaultWorkflowId(character);
            if (workflowId != null && !workflowId.isEmpty()) {
                return Mode.WORKFLOW;
            }
            // WORKFLOW 请求但无有效 workflowId，降级 REACT
            log.warn("[Orchestration] WORKFLOW mode requested but no valid workflowId, falling back to REACT");
            return Mode.REACT;
        }

        // 3. 默认 REACT
        return Mode.REACT;
    }

    /**
     * 获取默认 workflow ID
     */
    private String getDefaultWorkflowId(Character character) {
        if (character.getAgentEngineConfig() != null
                && character.getAgentEngineConfig().getWorkflowConfig() != null) {
            return character.getAgentEngineConfig().getWorkflowConfig().getDefaultWorkflowId();
        }
        return null;
    }
}
