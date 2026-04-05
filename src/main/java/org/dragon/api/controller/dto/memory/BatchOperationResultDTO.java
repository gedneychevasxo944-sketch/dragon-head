package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量操作结果 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResultDTO {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 删除数量
     */
    private int deletedCount;

    /**
     * 更新数量
     */
    private int updatedCount;

    /**
     * 清除数量
     */
    private int clearedCount;
}
