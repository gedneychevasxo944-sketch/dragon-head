package org.dragon.observer.commons;

import java.util.List;
import java.util.Optional;

/**
 * CommonSense 常识存储接口
 * 定义对常识库的基本操作
 *
 * @author wyj
 * @version 1.0
 */
public interface CommonSenseStore {

    /**
     * 保存常识
     *
     * @param commonSense 常识实体
     * @return 保存后的常识
     */
    CommonSense save(CommonSense commonSense);

    /**
     * 根据 ID 查询常识
     *
     * @param id 常识 ID
     * @return Optional 常识
     */
    Optional<CommonSense> findById(String id);

    /**
     * 查询所有常识
     *
     * @return 常识列表
     */
    List<CommonSense> findAll();

    /**
     * 根据类别查询常识
     *
     * @param category 类别
     * @return 常识列表
     */
    List<CommonSense> findByCategory(CommonSense.Category category);

    /**
     * 查询所有启用的常识
     *
     * @return 启用的常识列表
     */
    List<CommonSense> findEnabled();

    /**
     * 根据严重程度查询常识
     *
     * @param severity 严重程度
     * @return 常识列表
     */
    List<CommonSense> findBySeverity(CommonSense.Severity severity);

    /**
     * 删除常识
     *
     * @param id 常识 ID
     * @return 是否删除成功
     */
    boolean delete(String id);

    /**
     * 检查是否存在指定 ID 的常识
     *
     * @param id 常识 ID
     * @return 是否存在
     */
    boolean exists(String id);

    /**
     * 获取总数
     *
     * @return 总数
     */
    int count();

    /**
     * 清除所有常识
     */
    void clear();
}
