package org.dragon.memory.core;

import org.dragon.api.controller.dto.RuntimeStatusDTO;
import org.dragon.api.controller.dto.ProbeResultDTO;
import org.dragon.api.controller.dto.SyncResultDTO;
import org.dragon.api.controller.dto.BatchOperationResultDTO;

/**
 * 运维服务接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface OperationsService {
    /**
     * 获取运行时状态
     *
     * @param scope    范围
     * @param targetId 目标 ID
     * @return 运行时状态
     */
    RuntimeStatusDTO getRuntimeStatus(String scope, String targetId);

    /**
     * 执行系统检查
     *
     * @param scope    范围
     * @param targetId 目标 ID
     * @return 检查结果
     */
    ProbeResultDTO probe(String scope, String targetId);

    /**
     * 重建索引
     *
     * @param scope     范围
     * @param targetId  目标 ID
     * @param sourceId  数据源 ID
     * @return 同步结果
     */
    SyncResultDTO reindex(String scope, String targetId, String sourceId);

    /**
     * 清除缓存
     *
     * @param scope    范围
     * @param targetId 目标 ID
     * @return 批量操作结果
     */
    BatchOperationResultDTO clearCache(String scope, String targetId);
}
