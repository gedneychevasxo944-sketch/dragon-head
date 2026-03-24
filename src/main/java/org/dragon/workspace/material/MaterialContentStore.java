package org.dragon.workspace.material;

import java.util.Optional;

/**
 * 物料解析内容存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface MaterialContentStore {

    /**
     * 保存解析后的内容
     */
    void saveParsedContent(ParsedMaterialContent content);

    /**
     * 根据 ID 查询
     */
    Optional<ParsedMaterialContent> findById(String id);

    /**
     * 根据物料 ID 查询最新解析内容
     */
    Optional<ParsedMaterialContent> findByMaterialId(String materialId);

    /**
     * 删除
     */
    void delete(String id);
}
