package org.dragon.workspace.material;

import java.io.InputStream;

/**
 * MaterialStorage 物料存储后端接口
 * 支持多种存储后端（本地磁盘、对象存储 S3、数据库 BLOB 等）
 *
 * @author wyj
 * @version 1.0
 */
public interface MaterialStorage {

    /**
     * 存储物料
     *
     * @param workspaceId 工作空间 ID
     * @param inputStream 输入流
     * @param filename 文件名
     * @return 存储键
     */
    String store(String workspaceId, InputStream inputStream, String filename);

    /**
     * 检索物料
     *
     * @param key 存储键
     * @return 输入流
     */
    InputStream retrieve(String key);

    /**
     * 删除物料
     *
     * @param key 存储键
     */
    void delete(String key);

    /**
     * 检查物料是否存在
     *
     * @param key 存储键
     * @return 是否存在
     */
    boolean exists(String key);
}
