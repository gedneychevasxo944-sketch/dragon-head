package org.dragon.observer.log;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * ModificationLog 内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryModificationLogStore implements ModificationLogStore {

    private final Map<String, ModificationLog> store = new ConcurrentHashMap<>();
    private final List<ModificationLog> orderedList = new CopyOnWriteArrayList<>();

    @Override
    public ModificationLog save(ModificationLog log) {
        if (log.getId() == null) {
            throw new IllegalArgumentException("ModificationLog id cannot be null");
        }
        if (log.getTimestamp() == null) {
            log.setTimestamp(LocalDateTime.now());
        }
        store.put(log.getId(), log);
        orderedList.add(log);
        return log;
    }

    @Override
    public Optional<ModificationLog> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ModificationLog> findByTarget(ModificationLog.TargetType targetType, String targetId) {
        return orderedList.stream()
                .filter(l -> l.getTargetType() == targetType && targetId.equals(l.getTargetId()))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ModificationLog> findRecentByTarget(ModificationLog.TargetType targetType, String targetId, int limit) {
        return orderedList.stream()
                .filter(l -> l.getTargetType() == targetType && targetId.equals(l.getTargetId()))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModificationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return orderedList.stream()
                .filter(l -> l.getTimestamp() != null
                        && !l.getTimestamp().isBefore(startTime)
                        && !l.getTimestamp().isAfter(endTime))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ModificationLog> findByOperator(String operator) {
        return orderedList.stream()
                .filter(l -> operator.equals(l.getOperator()))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ModificationLog> findByTriggerSource(ModificationLog.TriggerSource triggerSource) {
        return orderedList.stream()
                .filter(l -> l.getTriggerSource() == triggerSource)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        ModificationLog removed = store.remove(id);
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
