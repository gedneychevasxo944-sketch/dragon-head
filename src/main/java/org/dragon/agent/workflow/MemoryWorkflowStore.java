package org.dragon.agent.workflow;

import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryWorkflowStore 内存实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryWorkflowStore implements WorkflowStore {

    private final Map<String, Workflow> workflows = new ConcurrentHashMap<>();
    private final Map<String, WorkflowState> states = new ConcurrentHashMap<>();

    @Override
    public Workflow save(Workflow workflow) {
        workflows.put(workflow.getId(), workflow);
        return workflow;
    }

    @Override
    public Optional<Workflow> findById(String id) {
        return Optional.ofNullable(workflows.get(id));
    }

    @Override
    public Optional<Workflow> findByName(String name) {
        return workflows.values().stream()
                .filter(w -> name.equals(w.getName()))
                .findFirst();
    }

    @Override
    public List<Workflow> findAll() {
        return new ArrayList<>(workflows.values());
    }

    @Override
    public List<Workflow> findByCharacterId(String characterId) {
        return workflows.values().stream()
                .filter(w -> characterId.equals(w.getVariables().get("characterId")))
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        return workflows.remove(id) != null;
    }

    @Override
    public int count() {
        return workflows.size();
    }

    @Override
    public void clear() {
        workflows.clear();
    }

    // ==================== 执行状态实现 ====================

    @Override
    public WorkflowState saveState(WorkflowState state) {
        states.put(state.getExecutionId(), state);
        return state;
    }

    @Override
    public Optional<WorkflowState> findStateByExecutionId(String executionId) {
        return Optional.ofNullable(states.get(executionId));
    }

    @Override
    public List<WorkflowState> findRunningStates() {
        return states.values().stream()
                .filter(s -> s.getStatus() == WorkflowState.State.RUNNING
                        || s.getStatus() == WorkflowState.State.SUSPENDED)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteState(String executionId) {
        return states.remove(executionId) != null;
    }

    @Override
    public void clearStates() {
        states.clear();
    }
}