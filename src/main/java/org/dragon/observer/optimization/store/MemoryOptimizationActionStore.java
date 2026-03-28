package org.dragon.observer.optimization.store;

import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationAction.Status;
import org.dragon.observer.optimization.plan.OptimizationAction.TargetType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * OptimizationAction 内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryOptimizationActionStore implements OptimizationActionStore {

    private final ConcurrentHashMap<String, OptimizationAction> store = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<OptimizationAction> orderedList = new CopyOnWriteArrayList<>();

    @Override
    public OptimizationAction save(OptimizationAction action) {
        if (action.getId() == null) {
            throw new IllegalArgumentException("OptimizationAction id cannot be null");
        }
        if (action.getCreatedAt() == null) {
            action.setCreatedAt(LocalDateTime.now());
        }
        store.put(action.getId(), action);
        orderedList.add(action);
        return action;
    }

    @Override
    public Optional<OptimizationAction> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<OptimizationAction> findByEvaluationId(String evaluationId) {
        return orderedList.stream()
                .filter(a -> evaluationId.equals(a.getEvaluationId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationAction> findPending() {
        return orderedList.stream()
                .filter(a -> a.getStatus() == OptimizationAction.Status.PENDING)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationAction> findPendingOrdered(int limit) {
        return orderedList.stream()
                .filter(a -> a.getStatus() == OptimizationAction.Status.PENDING)
                .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationAction> findByTarget(OptimizationAction.TargetType targetType, String targetId) {
        return orderedList.stream()
                .filter(a -> a.getTargetType() == targetType && targetId.equals(a.getTargetId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationAction> findByStatus(OptimizationAction.Status status) {
        return orderedList.stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateStatus(String id, OptimizationAction.Status status) {
        OptimizationAction action = store.get(id);
        if (action == null) {
            return false;
        }

        switch (status) {
            case EXECUTED:
                action.setStatus(OptimizationAction.Status.EXECUTED);
                action.setExecutedAt(LocalDateTime.now());
                break;
            case ROLLED_BACK:
                action.setStatus(OptimizationAction.Status.ROLLED_BACK);
                action.setRolledBackAt(LocalDateTime.now());
                break;
            case REJECTED:
                action.setStatus(OptimizationAction.Status.REJECTED);
                break;
            case FAILED:
                action.setStatus(OptimizationAction.Status.FAILED);
                break;
            default:
                action.setStatus(status);
        }
        return true;
    }

    @Override
    public boolean delete(String id) {
        OptimizationAction removed = store.remove(id);
        if (removed != null) {
            orderedList.remove(removed);
            return true;
        }
        return false;
    }

    @Override
    public int count() {
        return store.size();
    }

    @Override
    public void clear() {
        store.clear();
        orderedList.clear();
    }
}
