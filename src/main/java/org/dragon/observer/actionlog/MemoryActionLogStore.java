package org.dragon.observer.actionlog;

import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Observer ActionLog 内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryActionLogStore implements ActionLogStore {

    private static final Logger log = LoggerFactory.getLogger(MemoryActionLogStore.class);

    private final Map<String, ObserverActionLog> logs = new ConcurrentHashMap<>();

    @Override
    public void save(ObserverActionLog actionLog) {
        logs.put(actionLog.getId(), actionLog);
        log.debug("[MemoryActionLogStore] Saved log: {}", actionLog.getId());
    }

    @Override
    public List<ObserverActionLog> findByTarget(String targetType, String targetId) {
        return logs.values().stream()
                .filter(log -> targetType.equals(log.getTargetType()) && targetId.equals(log.getTargetId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ObserverActionLog> findByActionType(ActionType actionType) {
        return logs.values().stream()
                .filter(log -> actionType == log.getActionType())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ObserverActionLog> findByTargetAndActionType(String targetType, String targetId, ActionType actionType) {
        return logs.values().stream()
                .filter(log -> targetType.equals(log.getTargetType())
                        && targetId.equals(log.getTargetId())
                        && actionType == log.getActionType())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ObserverActionLog> findAll() {
        return new ArrayList<>(logs.values()).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        logs.remove(id);
        log.debug("[MemoryActionLogStore] Deleted log: {}", id);
    }

    @Override
    public void clear() {
        logs.clear();
        log.info("[MemoryActionLogStore] Cleared all logs");
    }
}
