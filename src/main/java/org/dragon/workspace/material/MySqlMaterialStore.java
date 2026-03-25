package org.dragon.workspace.material;

import io.ebean.Database;
import org.dragon.datasource.entity.MaterialEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlMaterialStore 物料MySQL存储实现
 */
@Component
public class MySqlMaterialStore implements MaterialStore {

    private final Database mysqlDb;

    public MySqlMaterialStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(Material material) {
        MaterialEntity entity = MaterialEntity.fromMaterial(material);
        mysqlDb.save(entity);
    }

    @Override
    public void update(Material material) {
        MaterialEntity entity = MaterialEntity.fromMaterial(material);
        mysqlDb.update(entity);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(MaterialEntity.class, id);
    }

    @Override
    public Optional<Material> findById(String id) {
        MaterialEntity entity = mysqlDb.find(MaterialEntity.class, id);
        return entity != null ? Optional.of(entity.toMaterial()) : Optional.empty();
    }

    @Override
    public List<Material> findByWorkspaceId(String workspaceId) {
        return mysqlDb.find(MaterialEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findList()
                .stream()
                .map(MaterialEntity::toMaterial)
                .collect(Collectors.toList());
    }

    @Override
    public List<Material> findByName(String workspaceId, String name) {
        return mysqlDb.find(MaterialEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("name", name)
                .findList()
                .stream()
                .map(MaterialEntity::toMaterial)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return mysqlDb.find(MaterialEntity.class, id) != null;
    }
}
