package org.dragon.task;

import io.ebean.Database;
import org.dragon.datasource.entity.TaskEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlTaskStore 任务MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlTaskStore implements TaskStore {

    private final Database mysqlDb;

    public MySqlTaskStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(Task task) {
        TaskEntity entity = TaskEntity.fromTask(task);
        mysqlDb.save(entity);
    }

    @Override
    public void update(Task task) {
        TaskEntity entity = TaskEntity.fromTask(task);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(TaskEntity.class, id);
    }

    @Override
    public Optional<Task> findById(String id) {
        TaskEntity entity = mysqlDb.find(TaskEntity.class, id);
        return entity != null ? Optional.of(entity.toTask()) : Optional.empty();
    }

    @Override
    public List<Task> findByWorkspaceId(String workspaceId) {
        return mysqlDb.find(TaskEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findList()
                .stream()
                .map(TaskEntity::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByParentTaskId(String parentTaskId) {
        return mysqlDb.find(TaskEntity.class)
                .where()
                .eq("parentTaskId", parentTaskId)
                .findList()
                .stream()
                .map(TaskEntity::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        return mysqlDb.find(TaskEntity.class)
                .where()
                .eq("status", status.name())
                .findList()
                .stream()
                .map(TaskEntity::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByCharacterId(String characterId) {
        return mysqlDb.find(TaskEntity.class)
                .where()
                .eq("characterId", characterId)
                .findList()
                .stream()
                .map(TaskEntity::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByCreatorId(String creatorId) {
        return mysqlDb.find(TaskEntity.class)
                .where()
                .eq("creatorId", creatorId)
                .findList()
                .stream()
                .map(TaskEntity::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findWaitingTasksByWorkspaceId(String workspaceId) {
        return mysqlDb.find(TaskEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .in("status", TaskStatus.SUSPENDED.name(), TaskStatus.WAITING_USER_INPUT.name(), TaskStatus.WAITING_DEPENDENCY.name())
                .findList()
                .stream()
                .map(TaskEntity::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return mysqlDb.find(TaskEntity.class, id) != null;
    }

    @Override
    public List<Task> findRunnableChildTasks(String parentTaskId) {
        return mysqlDb.find(TaskEntity.class)
                .where()
                .eq("parentTaskId", parentTaskId)
                .eq("status", TaskStatus.PENDING.name())
                .findList()
                .stream()
                .map(TaskEntity::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findWaitingTasksByDependencyTaskId(String dependencyTaskId) {
        return mysqlDb.find(TaskEntity.class)
                .where()
                .eq("status", TaskStatus.WAITING_DEPENDENCY.name())
                .findList()
                .stream()
                .filter(task -> task.getDependencyTaskIds() != null && task.getDependencyTaskIds().contains(dependencyTaskId))
                .map(TaskEntity::toTask)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByOriginalCharacterId(String originalCharacterId, String workspaceId) {
        return mysqlDb.find(TaskEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("status", TaskStatus.WAITING_DEPENDENCY.name())
                .eq("originalCharacterId", originalCharacterId)
                .findList()
                .stream()
                .map(TaskEntity::toTask)
                .collect(Collectors.toList());
    }
}
