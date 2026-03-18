package org.dragon.observer.evaluation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * EvaluationRecord 内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class MemoryEvaluationRecordStore implements EvaluationRecordStore {

    private final Map<String, EvaluationRecord> store = new ConcurrentHashMap<>();
    private final List<EvaluationRecord> orderedList = new CopyOnWriteArrayList<>();

    @Override
    public EvaluationRecord save(EvaluationRecord record) {
        if (record.getId() == null) {
            throw new IllegalArgumentException("EvaluationRecord id cannot be null");
        }
        if (record.getTimestamp() == null) {
            record.setTimestamp(LocalDateTime.now());
        }
        store.put(record.getId(), record);
        orderedList.add(record);
        return record;
    }

    @Override
    public Optional<EvaluationRecord> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<EvaluationRecord> findByTarget(EvaluationRecord.TargetType targetType, String targetId) {
        return orderedList.stream()
                .filter(r -> r.getTargetType() == targetType && targetId.equals(r.getTargetId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationRecord> findRecentByTarget(EvaluationRecord.TargetType targetType, String targetId, int limit) {
        return orderedList.stream()
                .filter(r -> r.getTargetType() == targetType && targetId.equals(r.getTargetId()))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationRecord> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return orderedList.stream()
                .filter(r -> r.getTimestamp() != null
                        && !r.getTimestamp().isBefore(startTime)
                        && !r.getTimestamp().isAfter(endTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationRecord> findByTargetAndTimeRange(
            EvaluationRecord.TargetType targetType, String targetId,
            LocalDateTime startTime, LocalDateTime endTime) {
        return orderedList.stream()
                .filter(r -> r.getTargetType() == targetType
                        && targetId.equals(r.getTargetId())
                        && r.getTimestamp() != null
                        && !r.getTimestamp().isBefore(startTime)
                        && !r.getTimestamp().isAfter(endTime))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EvaluationRecord> findByTaskId(String taskId) {
        return orderedList.stream()
                .filter(r -> taskId.equals(r.getTaskId()))
                .findFirst();
    }

    @Override
    public List<EvaluationRecord> findBelowThreshold(double threshold) {
        return orderedList.stream()
                .filter(r -> {
                    if (r.getOverallScore() == null) {
                        r.calculateOverallScore();
                    }
                    return r.getOverallScore() != null && r.getOverallScore() < threshold;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        EvaluationRecord removed = store.remove(id);
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
