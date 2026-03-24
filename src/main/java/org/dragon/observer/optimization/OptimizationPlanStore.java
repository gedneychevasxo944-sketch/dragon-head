package org.dragon.observer.optimization;

import java.util.List;
import java.util.Optional;

/**
 * OptimizationPlanStore 优化计划存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface OptimizationPlanStore {

    /**
     * 保存计划
     *
     * @param plan 优化计划
     * @return 保存后的计划
     */
    OptimizationPlan save(OptimizationPlan plan);

    /**
     * 根据 ID 查询
     *
     * @param id 计划 ID
     * @return Optional 计划
     */
    Optional<OptimizationPlan> findById(String id);

    /**
     * 根据评价 ID 查询
     *
     * @param evaluationId 评价 ID
     * @return 计划列表
     */
    List<OptimizationPlan> findByEvaluationId(String evaluationId);

    /**
     * 根据目标查询
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 计划列表
     */
    List<OptimizationPlan> findByTarget(OptimizationAction.TargetType targetType, String targetId);

    /**
     * 查询待审批的计划
     *
     * @return 计划列表
     */
    List<OptimizationPlan> findPendingApproval();

    /**
     * 查询执行中的计划
     *
     * @return 计划列表
     */
    List<OptimizationPlan> findExecuting();

    /**
     * 根据状态查询
     *
     * @param status 状态
     * @return 计划列表
     */
    List<OptimizationPlan> findByStatus(OptimizationPlan.Status status);

    /**
     * 删除计划
     *
     * @param id 计划 ID
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
     * 清除所有计划
     */
    void clear();

    // ==================== PlanItem 操作 ====================

    /**
     * 保存计划项目
     *
     * @param item 计划项目
     * @return 保存后的项目
     */
    OptimizationPlanItem saveItem(OptimizationPlanItem item);

    /**
     * 根据 ID 查询计划项目
     *
     * @param id 项目 ID
     * @return Optional 项目
     */
    Optional<OptimizationPlanItem> findItemById(String id);

    /**
     * 根据计划 ID 查询所有项目
     *
     * @param planId 计划 ID
     * @return 项目列表
     */
    List<OptimizationPlanItem> findItemsByPlanId(String planId);

    /**
     * 更新项目状态
     *
     * @param id     项目 ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateItemStatus(String id, OptimizationPlanItem.Status status);
}