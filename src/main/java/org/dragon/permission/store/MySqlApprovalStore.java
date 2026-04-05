package org.dragon.permission.store;

import io.ebean.Database;
import org.dragon.datasource.entity.ApprovalRequestEntity;
import org.dragon.permission.enums.ApprovalStatus;
import org.dragon.permission.enums.ApprovalType;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.permission.enums.ResourceType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MySqlApprovalStore 审批请求MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlApprovalStore implements ApprovalStore {

    private final Database mysqlDb;

    public MySqlApprovalStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(ApprovalRequestEntity request) {
        mysqlDb.save(request);
    }

    @Override
    public void update(ApprovalRequestEntity request) {
        mysqlDb.update(request);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(ApprovalRequestEntity.class, id);
    }

    @Override
    public Optional<ApprovalRequestEntity> findById(String id) {
        ApprovalRequestEntity entity = mysqlDb.find(ApprovalRequestEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public List<ApprovalRequestEntity> findByResource(ResourceType resourceType, String resourceId) {
        return mysqlDb.find(ApprovalRequestEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .eq("resourceId", resourceId)
                .findList();
    }

    @Override
    public List<ApprovalRequestEntity> findByTypeAndStatus(ApprovalType approvalType, ApprovalStatus status) {
        return mysqlDb.find(ApprovalRequestEntity.class)
                .where()
                .eq("approvalType", approvalType.name())
                .eq("status", status.name())
                .findList();
    }

    @Override
    public List<ApprovalRequestEntity> findPendingByApprover(Long approverId) {
        return mysqlDb.find(ApprovalRequestEntity.class)
                .where()
                .eq("approverId", approverId)
                .eq("status", ApprovalStatus.PENDING.name())
                .findList();
    }

    @Override
    public List<ApprovalRequestEntity> findByRequester(Long requesterId) {
        return mysqlDb.find(ApprovalRequestEntity.class)
                .where()
                .eq("requesterId", requesterId)
                .findList();
    }

    @Override
    public boolean existsPendingRequest(ResourceType resourceType, String resourceId, ApprovalType approvalType) {
        return mysqlDb.find(ApprovalRequestEntity.class)
                .where()
                .eq("resourceType", resourceType.name())
                .eq("resourceId", resourceId)
                .eq("approvalType", approvalType.name())
                .eq("status", ApprovalStatus.PENDING.name())
                .findCount() > 0;
    }

    @Override
    public List<ApprovalRequestEntity> findAll() {
        return mysqlDb.find(ApprovalRequestEntity.class).findList();
    }
}
