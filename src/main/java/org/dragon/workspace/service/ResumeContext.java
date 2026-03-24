package org.dragon.workspace.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务恢复上下文
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeContext {
    /**
     * 新的输入（用户回复等）
     */
    private Object newInput;

    /**
     * 恢复原因
     */
    private String reason;
}
