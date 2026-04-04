package org.dragon.permission.store;

import io.ebean.Database;
import org.dragon.permission.entity.PermissionPolicyEntity;
import org.dragon.permission.enums.Role;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.permission.enums.ResourceType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MySqlPermissionPolicyStore 权限策略MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlPermissionPolicyStore implements PermissionPolicyStore {

    private final Database mysqlDb;

    public MySqlPermissionPolicyStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public List<PermissionPolicyEntity> findByRoleAndResourceType(Role role, ResourceType resourceType) {
        return mysqlDb.find(PermissionPolicyEntity.class)
                .where()
                .eq("role", role.name())
                .or()
                .eq("resourceType", resourceType.name())
                .eq("resourceType", "*")
                .endOr()
                .findList();
    }

    @Override
    public List<PermissionPolicyEntity> findByRole(Role role) {
        return mysqlDb.find(PermissionPolicyEntity.class)
                .where()
                .eq("role", role.name())
                .findList();
    }

    @Override
    public Optional<PermissionPolicyEntity> findById(Long id) {
        PermissionPolicyEntity entity = mysqlDb.find(PermissionPolicyEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public List<PermissionPolicyEntity> findAll() {
        return mysqlDb.find(PermissionPolicyEntity.class).findList();
    }
}
