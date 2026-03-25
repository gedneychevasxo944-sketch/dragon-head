package org.dragon.agent.workflow;

import io.ebean.Database;
import org.dragon.datasource.entity.WorkflowEntity;
import org.dragon.datasource.entity.WorkflowStateEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlWorkflowStore 工作流MySQL存储实现
 */
@Component
public class MySqlWorkflowStore implements WorkflowStore {

    private final Database mysqlDb;

    public MySqlWorkflowStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public Workflow save(Workflow workflow) {
        WorkflowEntity entity = WorkflowEntity.fromWorkflow(workflow);
        mysqlDb.save(entity);
        return workflow;
    }

    @Override
    public Optional<Workflow> findById(String id) {
        WorkflowEntity entity = mysqlDb.find(WorkflowEntity.class, id);
        return entity != null ? Optional.of(entity.toWorkflow()) : Optional.empty();
    }

    @Override
    public Optional<Workflow> findByName(String name) {
        WorkflowEntity entity = mysqlDb.find(WorkflowEntity.class)
                .where()
                .eq("name", name)
                .findOne();
        return entity != null ? Optional.of(entity.toWorkflow()) : Optional.empty();
    }

    @Override
    public List<Workflow> findAll() {
        return mysqlDb.find(WorkflowEntity.class)
                .findList()
                .stream()
                .map(WorkflowEntity::toWorkflow)
                .collect(Collectors.toList());
    }

    @Override
    public List<Workflow> findByCharacterId(String characterId) {
        return mysqlDb.find(WorkflowEntity.class)
                .findList()
                .stream()
                .filter(w -> characterId.equals(w.getVariables() != null ? w.getVariables().get("characterId") : null))
                .map(WorkflowEntity::toWorkflow)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        int rows = mysqlDb.find(WorkflowEntity.class, id) != null ? 1 : 0;
        mysqlDb.delete(WorkflowEntity.class, id);
        return rows > 0;
    }

    @Override
    public int count() {
        return (int) mysqlDb.find(WorkflowEntity.class).findCount();
    }

    @Override
    public void clear() {
        mysqlDb.find(WorkflowEntity.class).delete();
    }

    // ==================== 执行状态实现 ====================

    @Override
    public WorkflowState saveState(WorkflowState state) {
        WorkflowStateEntity entity = WorkflowStateEntity.fromWorkflowState(state);
        mysqlDb.save(entity);
        return state;
    }

    @Override
    public Optional<WorkflowState> findStateByExecutionId(String executionId) {
        WorkflowStateEntity entity = mysqlDb.find(WorkflowStateEntity.class, executionId);
        return entity != null ? Optional.of(entity.toWorkflowState()) : Optional.empty();
    }

    @Override
    public List<WorkflowState> findRunningStates() {
        return mysqlDb.find(WorkflowStateEntity.class)
                .where()
                .in("status", WorkflowState.State.RUNNING.name(), WorkflowState.State.SUSPENDED.name())
                .findList()
                .stream()
                .map(WorkflowStateEntity::toWorkflowState)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteState(String executionId) {
        int rows = mysqlDb.find(WorkflowStateEntity.class, executionId) != null ? 1 : 0;
        mysqlDb.delete(WorkflowStateEntity.class, executionId);
        return rows > 0;
    }

    @Override
    public void clearStates() {
        mysqlDb.find(WorkflowStateEntity.class).delete();
    }
}
