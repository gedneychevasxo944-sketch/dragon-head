package org.dragon.observer.optimization.store;

import java.util.List;
import java.util.Optional;

import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationAction.Status;
import org.dragon.observer.optimization.plan.OptimizationAction.TargetType;

/**
 * OptimizationAction 优化动作存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface OptimizationActionStore {

    /**
     * 保存优化动作
     *
     * @param action 优化动作
     * @return 保存后的动作
     */
    OptimizationAction save(OptimizationAction action);

    /**
     * 根据 ID 查询
     *
     * @param id 动作 ID
     * @return Optional 动作
     */
    Optional<OptimizationAction> findById(String id);

    /**
     * 根据评价 ID 查询
     *
     * @param evaluationId 评价 ID
     * @return 动作列表
     */
    List<OptimizationAction> findByEvaluationId(String evaluationId);

    /**
     * 查询待执行的优化动作
     *
     * @return 动作列表
     */
    List<OptimizationAction> findPending();

    /**
     * 查询待执行的优化动作（按优先级排序）
     *
     * @param limit 数量限制
     * @return 动作列表
     */
    List<OptimizationAction> findPendingOrdered(int limit);

    /**
     * 根据目标查询
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 动作列表
     */
    List<OptimizationAction> findByTarget(OptimizationAction.TargetType targetType, String targetId);

    /**
     * 根据状态查询
     *
     * @param status 状态
     * @return 动作列表
     */
    List<OptimizationAction> findByStatus(OptimizationAction.Status status);

    /**
     * 更新状态
     *
     * @param id     动作 ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateStatus(String id, OptimizationAction.Status status);

    /**
     * 删除动作
     *
     * @param id 动作 ID
     * @return 是否删除成功
     */
    boolean delete(String id);

    /**
     * 获取总数
     *
     * @return 总数
     */
    int count();

    /**
     * 清除所有动作
     */
    void clear();
}
