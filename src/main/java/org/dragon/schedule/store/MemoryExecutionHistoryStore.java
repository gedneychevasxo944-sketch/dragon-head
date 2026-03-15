package org.dragon.schedule.store;

import lombok.extern.slf4j.Slf4j;
import org.dragon.schedule.entity.ExecutionHistory;
import org.dragon.schedule.entity.ExecutionStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存执行历史存储实现
 */
@Slf4j
public class MemoryExecutionHistoryStore implements ExecutionHistoryStore {

    private final Map<String, ExecutionHistory> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public void save(ExecutionHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("Execution history cannot be null");
        }
        
        // 生成 ID
        if (history.getId() == null) {
            history.setId(idGenerator.getAndIncrement());
        }
        
        store.put(history.getExecutionId(), copy(history));
        log.debug("Saved execution history: executionId={}", history.getExecutionId());
    }

    @Override
    public void update(ExecutionHistory history) {
        if (history == null || history.getExecutionId() == null) {
            throw new IllegalArgumentException("Execution history or execution ID cannot be null");
        }
        if (!store.containsKey(history.getExecutionId())) {
            throw new IllegalArgumentException("Execution history not found: " + history.getExecutionId());
        }
        store.put(history.getExecutionId(), copy(history));
        log.debug("Updated execution history: executionId={}", history.getExecutionId());
    }

    @Override
    public Optional<ExecutionHistory> findByExecutionId(String executionId) {
        if (executionId == null) {
            return Optional.empty();
        }
        ExecutionHistory history = store.get(executionId);
        return history != null ? Optional.of(copy(history)) : Optional.empty();
    }

    @Override
    public List<ExecutionHistory> findByCronId(String cronId, int limit) {
        if (cronId == null) {
            return Collections.emptyList();
        }
        return store.values().stream()
                .filter(h -> cronId.equals(h.getCronId()))
                .sorted(Comparator.comparing(ExecutionHistory::getTriggerTime, Comparator.reverseOrder()))
                .limit(limit)
                .map(this::copy)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionHistory> findByStatus(ExecutionStatus status, int limit) {
        if (status == null) {
            return Collections.emptyList();
        }
        return store.values().stream()
                .filter(h -> status.equals(h.getStatus()))
                .limit(limit)
                .map(this::copy)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionHistory> findRunningJobs() {
        return findByStatus(ExecutionStatus.RUNNING, Integer.MAX_VALUE);
    }

    @Override
    public List<ExecutionHistory> findByExecuteNode(String nodeId) {
        if (nodeId == null) {
            return Collections.emptyList();
        }
        return store.values().stream()
                .filter(h -> nodeId.equals(h.getExecuteNode()))
                .map(this::copy)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String executionId) {
        if (executionId == null) {
            return;
        }
        store.remove(executionId);
        log.debug("Deleted execution history: executionId={}", executionId);
    }

    @Override
    public int deleteBefore(long beforeTime) {
        List<String> toDelete = store.values().stream()
                .filter(h -> h.getTriggerTime() != null && h.getTriggerTime() < beforeTime)
                .map(ExecutionHistory::getExecutionId)
                .collect(Collectors.toList());
        
        for (String executionId : toDelete) {
            store.remove(executionId);
        }
        
        log.debug("Deleted {} execution histories before timestamp {}", toDelete.size(), beforeTime);
        return toDelete.size();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public long countByCronId(String cronId) {
        if (cronId == null) {
            return 0;
        }
        return store.values().stream()
                .filter(h -> cronId.equals(h.getCronId()))
                .count();
    }

    /**
     * 深拷贝
     */
    private ExecutionHistory copy(ExecutionHistory source) {
        if (source == null) {
            return null;
        }
        ExecutionHistory copy = new ExecutionHistory();
        copy.setId(source.getId());
        copy.setExecutionId(source.getExecutionId());
        copy.setCronId(source.getCronId());
        copy.setCronName(source.getCronName());
        copy.setTriggerTime(source.getTriggerTime());
        copy.setActualFireTime(source.getActualFireTime());
        copy.setCompleteTime(source.getCompleteTime());
        copy.setDurationMs(source.getDurationMs());
        copy.setStatus(source.getStatus());
        copy.setExecuteNode(source.getExecuteNode());
        copy.setExecuteThread(source.getExecuteThread());
        copy.setResultData(source.getResultData());
        copy.setErrorMessage(source.getErrorMessage());
        copy.setStackTrace(source.getStackTrace());
        copy.setRetryCount(source.getRetryCount());
        copy.setParentExecutionId(source.getParentExecutionId());
        copy.setExt1(source.getExt1());
        copy.setExt2(source.getExt2());
        return copy;
    }
}
