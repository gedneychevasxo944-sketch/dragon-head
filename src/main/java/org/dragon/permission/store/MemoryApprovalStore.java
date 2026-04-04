package org.dragon.permission.store;

import org.dragon.permission.entity.ApprovalRequestEntity;
import org.dragon.permission.enums.ApprovalStatus;
import org.dragon.permission.enums.ApprovalType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.permission.enums.ResourceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MemoryApprovalStore 审批请求内存存储实现
 */
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryApprovalStore implements ApprovalStore {

    private final Map<String, ApprovalRequestEntity> store = new ConcurrentHashMap<>();

    @Override
    public void save(ApprovalRequestEntity request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("ApprovalRequest id cannot be null");
        }
        store.put(request.getId(), request);
    }

    @Override
    public void update(ApprovalRequestEntity request) {
        if (request.getId() == null || !store.containsKey(request.getId())) {
            throw new IllegalArgumentException("ApprovalRequest not found: " + request.getId());
        }
        store.put(request.getId(), request);
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public Optional<ApprovalRequestEntity> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ApprovalRequestEntity> findByResource(ResourceType resourceType, String resourceId) {
        return store.values().stream()
                .filter(r -> resourceType == r.getResourceType() && resourceId.equals(r.getResourceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ApprovalRequestEntity> findByTypeAndStatus(ApprovalType approvalType, ApprovalStatus status) {
        return store.values().stream()
                .filter(r -> approvalType == r.getApprovalType() && status == r.getStatus())
                .collect(Collectors.toList());
    }

    @Override
    public List<ApprovalRequestEntity> findPendingByApprover(Long approverId) {
        return store.values().stream()
                .filter(r -> approverId.equals(r.getApproverId()) && ApprovalStatus.PENDING == r.getStatus())
                .collect(Collectors.toList());
    }

    @Override
    public List<ApprovalRequestEntity> findByRequester(Long requesterId) {
        return store.values().stream()
                .filter(r -> requesterId.equals(r.getRequesterId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsPendingRequest(ResourceType resourceType, String resourceId, ApprovalType approvalType) {
        return store.values().stream()
                .anyMatch(r -> resourceType == r.getResourceType()
                        && resourceId.equals(r.getResourceId())
                        && approvalType == r.getApprovalType()
                        && ApprovalStatus.PENDING == r.getStatus());
    }
}
