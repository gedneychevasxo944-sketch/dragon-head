package org.dragon.workspace.commons.store;

import io.ebean.Database;
import org.dragon.datasource.entity.CommonSenseEntity;
import org.dragon.datasource.entity.CommonSenseFolderEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.workspace.commons.CommonSense;
import org.dragon.workspace.commons.CommonSenseFolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MySqlWorkspaceCommonSenseStore 工作空间常识MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlWorkspaceCommonSenseStore implements WorkspaceCommonSenseStore {

    private final Database mysqlDb;

    public MySqlWorkspaceCommonSenseStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    // ==================== 文件夹管理 ====================

    @Override
    public CommonSenseFolder saveFolder(CommonSenseFolder folder) {
        CommonSenseFolderEntity entity = CommonSenseFolderEntity.fromCommonSenseFolder(folder);
        mysqlDb.save(entity);
        return folder;
    }

    @Override
    public Optional<CommonSenseFolder> findFolderById(String id) {
        CommonSenseFolderEntity entity = mysqlDb.find(CommonSenseFolderEntity.class, id);
        return entity != null ? Optional.of(entity.toCommonSenseFolder()) : Optional.empty();
    }

    @Override
    public List<CommonSenseFolder> findRootFolders(String workspaceId) {
        return mysqlDb.find(CommonSenseFolderEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .isNull("parentId")
                .orderBy()
                .asc("sortOrder")
                .findList()
                .stream()
                .map(CommonSenseFolderEntity::toCommonSenseFolder)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSenseFolder> findFoldersByWorkspace(String workspaceId) {
        return mysqlDb.find(CommonSenseFolderEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .orderBy()
                .asc("sortOrder")
                .findList()
                .stream()
                .map(CommonSenseFolderEntity::toCommonSenseFolder)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSenseFolder> findChildFolders(String parentId) {
        return mysqlDb.find(CommonSenseFolderEntity.class)
                .where()
                .eq("parentId", parentId)
                .orderBy()
                .asc("sortOrder")
                .findList()
                .stream()
                .map(CommonSenseFolderEntity::toCommonSenseFolder)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteFolder(String id) {
        // 删除文件夹及其所有子文件夹
        List<CommonSenseFolderEntity> foldersToDelete = mysqlDb.find(CommonSenseFolderEntity.class)
                .where()
                .eq("id", id)
                .findList();

        for (CommonSenseFolderEntity folder : foldersToDelete) {
            mysqlDb.delete(CommonSenseFolderEntity.class, folder.getId());
        }

        // 删除文件夹下的所有常识
        mysqlDb.find(CommonSenseEntity.class)
                .where()
                .eq("folderId", id)
                .delete();

        return true;
    }

    // ==================== 常识管理 ====================

    @Override
    public CommonSense save(CommonSense commonSense) {
        CommonSenseEntity entity = CommonSenseEntity.fromCommonSense(commonSense);
        mysqlDb.save(entity);
        return commonSense;
    }

    @Override
    public Optional<CommonSense> findById(String id) {
        CommonSenseEntity entity = mysqlDb.find(CommonSenseEntity.class, id);
        return entity != null ? Optional.of(entity.toCommonSense()) : Optional.empty();
    }

    @Override
    public List<CommonSense> findByWorkspace(String workspaceId) {
        return mysqlDb.find(CommonSenseEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findList()
                .stream()
                .map(CommonSenseEntity::toCommonSense)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSense> findByFolder(String folderId) {
        return mysqlDb.find(CommonSenseEntity.class)
                .where()
                .eq("folderId", folderId)
                .findList()
                .stream()
                .map(CommonSenseEntity::toCommonSense)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSense> findEnabled(String workspaceId) {
        return mysqlDb.find(CommonSenseEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("enabled", true)
                .findList()
                .stream()
                .map(CommonSenseEntity::toCommonSense)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommonSense> findByWorkspaceAndCategory(String workspaceId, CommonSense.Category category) {
        return mysqlDb.find(CommonSenseEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .eq("category", category.name())
                .findList()
                .stream()
                .map(CommonSenseEntity::toCommonSense)
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        int rows = mysqlDb.find(CommonSenseEntity.class, id) != null ? 1 : 0;
        mysqlDb.delete(CommonSenseEntity.class, id);
        return rows > 0;
    }

    @Override
    public List<CommonSense> findAll() {
        return mysqlDb.find(CommonSenseEntity.class)
                .findList()
                .stream()
                .map(CommonSenseEntity::toCommonSense)
                .collect(Collectors.toList());
    }

    @Override
    public int countByWorkspace(String workspaceId) {
        return (int) mysqlDb.find(CommonSenseEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .findCount();
    }

    @Override
    public void clearByWorkspace(String workspaceId) {
        // 删除工作空间的所有常识
        mysqlDb.find(CommonSenseEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .delete();

        // 删除工作空间的所有文件夹
        mysqlDb.find(CommonSenseFolderEntity.class)
                .where()
                .eq("workspaceId", workspaceId)
                .delete();
    }
}
