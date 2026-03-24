package org.dragon.workspace.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务暂停上下文
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspendContext {
    /**
     * 暂停原因
     */
    private String reason;

    /**
     * 暂停时间
     */
    private String suspendedAt;
}
